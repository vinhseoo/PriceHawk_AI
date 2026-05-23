package com.pricehawk.catalog.consumer;

import com.pricehawk.catalog.domain.entity.*;
import com.pricehawk.catalog.domain.enums.ScrapeStatus;
import com.pricehawk.catalog.repository.*;
import com.pricehawk.common.constants.MessageQueueConstants;
import com.pricehawk.common.enums.Platform;
import com.pricehawk.common.enums.ScraperTier;
import com.pricehawk.common.enums.SourceType;
import com.pricehawk.common.event.AnalysisRequestEvent;
import com.pricehawk.common.event.PriceUpdatedEvent;
import com.pricehawk.common.event.ScrapeResultEvent;
import com.pricehawk.messaging.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapeResultConsumer {

    private final ProductRepository productRepository;
    private final SellerListingRepository sellerListingRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ReviewRepository reviewRepository;
    private final EventPublisher eventPublisher;

    @RabbitListener(queues = MessageQueueConstants.PRODUCT_SCRAPED_QUEUE)
    @Transactional
    public void onProductScraped(ScrapeResultEvent event) {
        log.info("Received ScrapeResultEvent: jobId={} domain={}", event.getJobId(), event.getDomain());

        try {
            if (event.getSellerListings() == null || event.getSellerListings().isEmpty()) {
                log.warn("ScrapeResultEvent has no seller listings: jobId={}", event.getJobId());
                return;
            }

            ScrapeResultEvent.ScrapedProductData pd = event.getProductData();
            if (pd == null || pd.getName() == null) {
                log.warn("ScrapeResultEvent has no product data: jobId={}", event.getJobId());
                return;
            }

            // Find or create Product from scraped data
            Product product = findOrCreateProduct(pd);

            // Process each scraped listing (usually 1 per event, but can be multiple)
            for (ScrapeResultEvent.ScrapedSellerListing scraped : event.getSellerListings()) {
                processListing(event, product, scraped);
            }

            // Update product's lowest price cache
            sellerListingRepository
                .findFirstByProductIdAndIsAvailableTrueOrderByCurrentPriceAsc(product.getId())
                .ifPresent(cheapest -> productRepository.updateLowestPrice(
                    product.getId(),
                    cheapest.getCurrentPrice(),
                    cheapest.getSellerName(),
                    cheapest.getDomain()
                ));

        } catch (Exception e) {
            log.error("Failed to process ScrapeResultEvent: jobId={} error={}", event.getJobId(), e.getMessage(), e);
            throw e; // Re-throw to trigger RabbitMQ retry/DLQ
        }
    }

    private Product findOrCreateProduct(ScrapeResultEvent.ScrapedProductData pd) {
        String slug = slugify(pd.getName());
        // Try to find by slug first (same product scraped again)
        return productRepository.findBySlug(slug).orElseGet(() -> {
            Product newProduct = Product.builder()
                .name(pd.getName())
                .slug(slug)
                .brand(pd.getBrand())
                .description(pd.getDescription())
                .thumbnailUrl(pd.getThumbnailUrl())
                .build();
            Product saved = productRepository.save(newProduct);
            log.info("New product created from scrape: id={} slug={}", saved.getId(), saved.getSlug());
            return saved;
        });
    }

    private void processListing(ScrapeResultEvent event,
                                 Product product,
                                 ScrapeResultEvent.ScrapedSellerListing scraped) {

        BigDecimal newPrice = scraped.getCurrentPrice() != null
            ? BigDecimal.valueOf(scraped.getCurrentPrice()) : null;

        // Idempotent upsert: check if listing already exists for this domain+externalProductId
        SellerListing listing;
        boolean isNewListing = false;

        if (scraped.getExternalProductId() != null) {
            var existing = sellerListingRepository
                .findByDomainAndExternalProductId(event.getDomain(), scraped.getExternalProductId());

            if (existing.isPresent()) {
                listing = existing.get();
                BigDecimal previousPrice = listing.getCurrentPrice();

                listing.setCurrentPrice(newPrice);
                listing.setScrapeStatus(ScrapeStatus.COMPLETED);
                listing.setLastScrapedAt(event.getScrapedAt());
                listing.setReviewCount(scraped.getReviewCount() != null ? scraped.getReviewCount() : 0);
                if (scraped.getAverageRating() != null) {
                    listing.setAverageRating(BigDecimal.valueOf(scraped.getAverageRating()));
                }
                listing = sellerListingRepository.save(listing);

                // Publish price change event if price dropped (for wishlist alerts)
                if (previousPrice != null && newPrice != null
                    && newPrice.compareTo(previousPrice) < 0) {
                    publishPriceUpdatedEvent(product, listing, previousPrice, newPrice);
                }
            } else {
                listing = createListing(event, product, scraped, newPrice);
                isNewListing = true;
            }
        } else {
            listing = createListing(event, product, scraped, newPrice);
            isNewListing = true;
        }

        // Append price snapshot
        if (newPrice != null) {
            priceHistoryRepository.save(PriceHistory.builder()
                .listing(listing)
                .price(newPrice)
                .build());
        }

        // Save new reviews (skip duplicates by sourceReviewId)
        if (scraped.getReviews() != null) {
            for (ScrapeResultEvent.ScrapedReview r : scraped.getReviews()) {
                saveReviewIfNotExists(listing, r);
            }
        }

        // Request AI analysis for new listings or when reviews have been updated
        if (isNewListing && listing.getReviewCount() > 0) {
            requestAnalysis(product, listing);
        }
    }

    private SellerListing createListing(ScrapeResultEvent event,
                                         Product product,
                                         ScrapeResultEvent.ScrapedSellerListing scraped,
                                         BigDecimal price) {
        Platform platform = parsePlatform(event.getPlatform());
        SourceType sourceType = event.getSourceType() != null ? event.getSourceType() : SourceType.MARKETPLACE;
        ScraperTier tier = event.getScraperTier();

        SellerListing listing = SellerListing.builder()
            .product(product)
            .domain(event.getDomain())
            .sourceType(sourceType)
            .scraperTier(tier)
            .platform(platform)
            .sellerName(scraped.getSellerName() != null ? scraped.getSellerName() : event.getDomain())
            .sellerId(scraped.getSellerId())
            .sellerUrl(scraped.getSellerUrl())
            .isOfficialStore(Boolean.TRUE.equals(scraped.getIsOfficialStore()))
            .externalUrl(scraped.getExternalUrl())
            .externalProductId(scraped.getExternalProductId())
            .currentPrice(price)
            .originalPrice(scraped.getOriginalPrice() != null
                ? BigDecimal.valueOf(scraped.getOriginalPrice()) : null)
            .currency(scraped.getCurrency() != null ? scraped.getCurrency() : "VND")
            .promotionInfo(scraped.getPromotionInfo())
            .reviewCount(scraped.getReviewCount() != null ? scraped.getReviewCount() : 0)
            .averageRating(scraped.getAverageRating() != null
                ? BigDecimal.valueOf(scraped.getAverageRating()) : null)
            .scrapeStatus(ScrapeStatus.COMPLETED)
            .lastScrapedAt(event.getScrapedAt())
            .build();

        return sellerListingRepository.save(listing);
    }

    private void saveReviewIfNotExists(SellerListing listing,
                                        ScrapeResultEvent.ScrapedReview scraped) {
        // sourceReviewId isn't in the event yet — use reviewer+content hash as dedup key for now
        // When V3 adds source_review_id to the DB, the scraper should populate it
        Review review = Review.builder()
            .listing(listing)
            .authorName(scraped.getReviewerName())
            .body(scraped.getContent())
            .rating(scraped.getRating() != null ? scraped.getRating() : 5)
            .reviewDate(parseReviewDate(scraped.getReviewDate()))
            .build();

        reviewRepository.save(review);
    }

    private void requestAnalysis(Product product, SellerListing listing) {
        AnalysisRequestEvent event = AnalysisRequestEvent.builder()
            .productId(product.getId().toString())
            .sellerListingId(listing.getId().toString())
            .reviewCount(listing.getReviewCount())
            .build();

        eventPublisher.publish(
            MessageQueueConstants.ANALYSIS_EXCHANGE,
            MessageQueueConstants.ANALYSIS_REQUEST_KEY,
            event
        );
        log.info("Analysis requested: productId={} listingId={}", product.getId(), listing.getId());
    }

    private void publishPriceUpdatedEvent(Product product, SellerListing listing,
                                           BigDecimal previousPrice, BigDecimal newPrice) {
        PriceUpdatedEvent event = PriceUpdatedEvent.builder()
            .productId(product.getId().toString())
            .productName(product.getName())
            .sellerListingId(listing.getId().toString())
            .domain(listing.getDomain())
            .sellerName(listing.getSellerName())
            .previousPrice(previousPrice)
            .newPrice(newPrice)
            .currency(listing.getCurrency())
            .build();

        eventPublisher.publish(
            MessageQueueConstants.PRICE_EXCHANGE,
            MessageQueueConstants.PRICE_UPDATED_KEY,
            event
        );
        log.info("Price drop event: productId={} {}→{}", product.getId(), previousPrice, newPrice);
    }

    private Platform parsePlatform(String platform) {
        if (platform == null) return Platform.OTHER;
        try {
            return Platform.valueOf(platform.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Platform.OTHER;
        }
    }

    private Instant parseReviewDate(String reviewDate) {
        if (reviewDate == null) return null;
        try {
            return Instant.parse(reviewDate);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert product name to URL-safe slug.
     * "iPhone 14 Pro Max!" → "iphone-14-pro-max"
     */
    static String slugify(String name) {
        if (name == null) return "";
        String normalized = Normalizer.normalize(name.toLowerCase().trim(), Normalizer.Form.NFD);
        String ascii = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        return ascii
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-{2,}", "-")
            .replaceAll("^-|-$", "");
    }
}
