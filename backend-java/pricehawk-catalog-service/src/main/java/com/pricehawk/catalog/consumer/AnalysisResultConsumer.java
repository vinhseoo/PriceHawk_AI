package com.pricehawk.catalog.consumer;

import com.pricehawk.catalog.repository.ProductRepository;
import com.pricehawk.catalog.repository.SellerListingRepository;
import com.pricehawk.common.constants.MessageQueueConstants;
import com.pricehawk.common.event.AnalysisResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisResultConsumer {

    private final ProductRepository productRepository;
    private final SellerListingRepository sellerListingRepository;

    @RabbitListener(queues = MessageQueueConstants.ANALYSIS_COMPLETED_QUEUE)
    @Transactional
    public void onAnalysisCompleted(AnalysisResultEvent event) {
        log.info("Received AnalysisResultEvent: productId={} listingId={}",
            event.getProductId(), event.getSellerListingId());

        try {
            // Update Product aggregate fields from AI analysis
            if (event.getProductId() != null) {
                UUID productId = UUID.fromString(event.getProductId());

                BigDecimal sentimentScore = event.getSentimentScore() != null
                    ? BigDecimal.valueOf(event.getSentimentScore()) : null;
                BigDecimal realReviewRatio = event.getRealReviewRatio() != null
                    ? BigDecimal.valueOf(event.getRealReviewRatio()) : null;

                productRepository.updateAnalysisResult(
                    productId,
                    event.getAiSummary(),
                    sentimentScore,
                    event.getTotalReviews() != null ? event.getTotalReviews() : 0,
                    realReviewRatio
                );

                log.info("Product analysis updated: id={} sentiment={} realRatio={}",
                    productId, sentimentScore, realReviewRatio);
            }

            // Update SellerListing trust score from AI analysis
            if (event.getSellerListingId() != null && event.getTrustScore() != null) {
                UUID listingId = UUID.fromString(event.getSellerListingId());
                BigDecimal trustScore = BigDecimal.valueOf(event.getTrustScore());

                sellerListingRepository.findById(listingId).ifPresent(listing -> {
                    listing.setTrustScore(trustScore);
                    sellerListingRepository.save(listing);
                    log.info("Listing trust score updated: id={} score={}", listingId, trustScore);
                });
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID in AnalysisResultEvent: productId={} listingId={} error={}",
                event.getProductId(), event.getSellerListingId(), e.getMessage());
            // Do NOT re-throw — invalid UUID is a permanent failure, no point retrying
        } catch (Exception e) {
            log.error("Failed to process AnalysisResultEvent: productId={} error={}",
                event.getProductId(), e.getMessage(), e);
            throw e; // Re-throw to trigger RabbitMQ retry/DLQ
        }
    }
}
