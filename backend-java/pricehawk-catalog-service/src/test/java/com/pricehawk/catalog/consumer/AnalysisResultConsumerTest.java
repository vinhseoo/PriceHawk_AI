package com.pricehawk.catalog.consumer;

import com.pricehawk.catalog.domain.entity.SellerListing;
import com.pricehawk.catalog.repository.ProductRepository;
import com.pricehawk.catalog.repository.SellerListingRepository;
import com.pricehawk.common.event.AnalysisResultEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisResultConsumerTest {

    @Mock ProductRepository productRepository;
    @Mock SellerListingRepository sellerListingRepository;
    @InjectMocks AnalysisResultConsumer consumer;

    @Test
    void onAnalysisCompleted_updatesProductFields() {
        UUID productId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        AnalysisResultEvent event = AnalysisResultEvent.builder()
            .productId(productId.toString())
            .sellerListingId(listingId.toString())
            .aiSummary("Sản phẩm tốt, pin trâu, màn đẹp")
            .sentimentScore(0.82)
            .totalReviews(250)
            .realReviewRatio(0.91)
            .trustScore(0.88)
            .build();

        SellerListing listing = SellerListing.builder()
            .domain("shopee.vn")
            .sellerName("Apple Official")
            .externalUrl("https://shopee.vn/product/1")
            .build();
        listing.setId(listingId);

        when(sellerListingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(sellerListingRepository.save(listing)).thenReturn(listing);

        consumer.onAnalysisCompleted(event);

        // Verify product analysis fields were updated
        verify(productRepository).updateAnalysisResult(
            eq(productId),
            eq("Sản phẩm tốt, pin trâu, màn đẹp"),
            eq(new BigDecimal("0.82")),
            eq(250),
            eq(new BigDecimal("0.91"))
        );

        // Verify trust score was written to listing
        verify(sellerListingRepository).save(argThat(sl ->
            sl.getTrustScore().compareTo(new BigDecimal("0.88")) == 0
        ));
    }

    @Test
    void onAnalysisCompleted_nullTrustScore_skipsListingUpdate() {
        UUID productId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        AnalysisResultEvent event = AnalysisResultEvent.builder()
            .productId(productId.toString())
            .sellerListingId(listingId.toString())
            .sentimentScore(0.75)
            .totalReviews(100)
            .realReviewRatio(0.85)
            .trustScore(null) // AI didn't produce a trust score
            .build();

        consumer.onAnalysisCompleted(event);

        // Product fields still updated
        verify(productRepository).updateAnalysisResult(any(), any(), any(), anyInt(), any());

        // Listing should NOT be touched — no trust score to write
        verifyNoInteractions(sellerListingRepository);
    }

    @Test
    void onAnalysisCompleted_nullProductId_skipsProductUpdate() {
        AnalysisResultEvent event = AnalysisResultEvent.builder()
            .productId(null)
            .sellerListingId(UUID.randomUUID().toString())
            .trustScore(0.80)
            .build();

        consumer.onAnalysisCompleted(event);

        verifyNoInteractions(productRepository);
    }

    @Test
    void onAnalysisCompleted_invalidUuid_doesNotThrow() {
        AnalysisResultEvent event = AnalysisResultEvent.builder()
            .productId("not-a-valid-uuid")
            .sellerListingId("also-invalid")
            .sentimentScore(0.5)
            .totalReviews(10)
            .build();

        // Should log error and swallow — invalid UUID is permanent failure, no retry
        consumer.onAnalysisCompleted(event);

        verifyNoInteractions(productRepository);
        verifyNoInteractions(sellerListingRepository);
    }

    @Test
    void onAnalysisCompleted_listingNotFound_skipsListingUpdate() {
        UUID productId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        AnalysisResultEvent event = AnalysisResultEvent.builder()
            .productId(productId.toString())
            .sellerListingId(listingId.toString())
            .sentimentScore(0.7)
            .totalReviews(50)
            .realReviewRatio(0.9)
            .trustScore(0.75)
            .build();

        when(sellerListingRepository.findById(listingId)).thenReturn(Optional.empty());

        consumer.onAnalysisCompleted(event);

        // Product is still updated
        verify(productRepository).updateAnalysisResult(any(), any(), any(), anyInt(), any());

        // No save called — listing not found
        verify(sellerListingRepository, never()).save(any());
    }
}
