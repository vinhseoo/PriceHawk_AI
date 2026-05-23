package com.pricehawk.catalog.consumer;

import com.pricehawk.catalog.domain.entity.Product;
import com.pricehawk.catalog.domain.entity.SellerListing;
import com.pricehawk.catalog.domain.enums.ScrapeStatus;
import com.pricehawk.catalog.repository.*;
import com.pricehawk.common.event.ScrapeResultEvent;
import com.pricehawk.messaging.publisher.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapeResultConsumerTest {

    @Mock ProductRepository productRepository;
    @Mock SellerListingRepository sellerListingRepository;
    @Mock PriceHistoryRepository priceHistoryRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock EventPublisher eventPublisher;
    @InjectMocks ScrapeResultConsumer consumer;

    private Product existingProduct;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        existingProduct = Product.builder().name("iPhone 14").slug("iphone-14").build();
        existingProduct.setId(productId);
    }

    @Test
    void onProductScraped_newProduct_createsProductAndListing() {
        ScrapeResultEvent event = buildEvent("shopee.vn", "SP001", 19990000.0, 2);

        when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(productRepository.save(any())).thenReturn(existingProduct);
        when(sellerListingRepository.findByDomainAndExternalProductId("shopee.vn", "SP001"))
            .thenReturn(Optional.empty());
        when(sellerListingRepository.save(any())).thenAnswer(inv -> {
            SellerListing sl = inv.getArgument(0);
            sl.setId(UUID.randomUUID());
            return sl;
        });
        when(sellerListingRepository.findFirstByProductIdAndIsAvailableTrueOrderByCurrentPriceAsc(productId))
            .thenReturn(Optional.empty());

        consumer.onProductScraped(event);

        verify(productRepository).save(any(Product.class));
        verify(sellerListingRepository).save(any(SellerListing.class));
        verify(priceHistoryRepository).save(any());
    }

    @Test
    void onProductScraped_existingListing_updatesPriceAndSavesHistory() {
        UUID listingId = UUID.randomUUID();
        SellerListing existingListing = SellerListing.builder()
            .product(existingProduct)
            .domain("shopee.vn")
            .externalProductId("SP001")
            .currentPrice(new BigDecimal("20000000"))
            .sellerName("Shopee Mall")
            .externalUrl("https://shopee.vn/product/1")
            .scrapeStatus(ScrapeStatus.COMPLETED)
            .build();
        existingListing.setId(listingId);

        ScrapeResultEvent event = buildEvent("shopee.vn", "SP001", 19000000.0, 0);

        when(productRepository.findBySlug(anyString())).thenReturn(Optional.of(existingProduct));
        when(sellerListingRepository.findByDomainAndExternalProductId("shopee.vn", "SP001"))
            .thenReturn(Optional.of(existingListing));
        when(sellerListingRepository.save(existingListing)).thenReturn(existingListing);
        when(sellerListingRepository.findFirstByProductIdAndIsAvailableTrueOrderByCurrentPriceAsc(productId))
            .thenReturn(Optional.of(existingListing));

        consumer.onProductScraped(event);

        // Price drop from 20M → 19M should trigger PriceUpdatedEvent
        verify(eventPublisher).publish(eq("pricehawk.price"), eq("price.updated"), any());

        // Price history should be appended
        verify(priceHistoryRepository).save(any());
    }

    @Test
    void onProductScraped_sameScrapeEventTwice_idempotent_doesNotDuplicateListing() {
        // Simulates the broker redelivering the same message (at-least-once delivery)
        UUID listingId = UUID.randomUUID();
        SellerListing existingListing = SellerListing.builder()
            .product(existingProduct)
            .domain("shopee.vn")
            .externalProductId("SP001")
            .currentPrice(new BigDecimal("19990000"))
            .sellerName("Shopee Mall")
            .externalUrl("https://shopee.vn/product/1")
            .scrapeStatus(ScrapeStatus.COMPLETED)
            .build();
        existingListing.setId(listingId);

        ScrapeResultEvent event = buildEvent("shopee.vn", "SP001", 19990000.0, 0);

        when(productRepository.findBySlug(anyString())).thenReturn(Optional.of(existingProduct));
        when(sellerListingRepository.findByDomainAndExternalProductId("shopee.vn", "SP001"))
            .thenReturn(Optional.of(existingListing));
        when(sellerListingRepository.save(existingListing)).thenReturn(existingListing);
        when(sellerListingRepository.findFirstByProductIdAndIsAvailableTrueOrderByCurrentPriceAsc(productId))
            .thenReturn(Optional.of(existingListing));

        // Process the same event twice
        consumer.onProductScraped(event);
        consumer.onProductScraped(event);

        // Product.save() must NOT be called — product already exists
        verify(productRepository, never()).save(any());

        // SellerListing must NOT be created again — only updated (save on existing)
        verify(sellerListingRepository, times(2)).save(existingListing);

        // Price did NOT change (same price) → no PriceUpdatedEvent
        verify(eventPublisher, never()).publish(eq("pricehawk.price"), anyString(), any());
    }

    @Test
    void onProductScraped_priceIncrease_doesNotPublishPriceEvent() {
        // Only price DROPS trigger notifications, not increases
        UUID listingId = UUID.randomUUID();
        SellerListing existingListing = SellerListing.builder()
            .product(existingProduct)
            .domain("tiki.vn")
            .externalProductId("TK999")
            .currentPrice(new BigDecimal("18000000"))
            .sellerName("Tiki Trading")
            .externalUrl("https://tiki.vn/product/999")
            .scrapeStatus(ScrapeStatus.COMPLETED)
            .build();
        existingListing.setId(listingId);

        // Price went UP from 18M → 20M
        ScrapeResultEvent event = buildEvent("tiki.vn", "TK999", 20000000.0, 0);

        when(productRepository.findBySlug(anyString())).thenReturn(Optional.of(existingProduct));
        when(sellerListingRepository.findByDomainAndExternalProductId("tiki.vn", "TK999"))
            .thenReturn(Optional.of(existingListing));
        when(sellerListingRepository.save(existingListing)).thenReturn(existingListing);
        when(sellerListingRepository.findFirstByProductIdAndIsAvailableTrueOrderByCurrentPriceAsc(productId))
            .thenReturn(Optional.of(existingListing));

        consumer.onProductScraped(event);

        // No price.updated event for price increases
        verify(eventPublisher, never()).publish(eq("pricehawk.price"), anyString(), any());
    }

    @Test
    void onProductScraped_nullProductData_skipsProcessing() {
        ScrapeResultEvent event = ScrapeResultEvent.builder()
            .jobId("job-001")
            .domain("shopee.vn")
            .productData(null)
            .sellerListings(List.of())
            .build();

        consumer.onProductScraped(event);

        verifyNoInteractions(productRepository);
    }

    @Test
    void slugify_convertsCorrently() {
        assertThat(ScrapeResultConsumer.slugify("iPhone 14 Pro Max!"))
            .isEqualTo("iphone-14-pro-max");
        assertThat(ScrapeResultConsumer.slugify("  Laptop  Gaming  "))
            .isEqualTo("laptop-gaming");
        assertThat(ScrapeResultConsumer.slugify(null))
            .isEmpty();
    }

    private ScrapeResultEvent buildEvent(String domain, String extId, double price, int reviewCount) {
        var listing = ScrapeResultEvent.ScrapedSellerListing.builder()
            .sellerName("Shopee Mall")
            .externalUrl("https://" + domain + "/product/1")
            .externalProductId(extId)
            .currentPrice(price)
            .currency("VND")
            .reviewCount(reviewCount)
            .build();

        var productData = ScrapeResultEvent.ScrapedProductData.builder()
            .name("iPhone 14")
            .brand("Apple")
            .build();

        return ScrapeResultEvent.builder()
            .jobId("job-001")
            .domain(domain)
            .productData(productData)
            .sellerListings(List.of(listing))
            .build();
    }
}
