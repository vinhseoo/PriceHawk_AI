package com.pricehawk.catalog.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable price snapshot — append-only, never updated.
 * Does NOT extend BaseEntity to avoid an updatedAt column on an append-only table.
 * V1 schema uses seller_listing_id and recorded_at (not listing_id / created_at).
 */
@Entity
@Table(name = "price_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_listing_id", nullable = false)
    private SellerListing listing;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "recorded_at", updatable = false)
    private Instant recordedAt;

    @PrePersist
    void prePersist() {
        if (this.recordedAt == null) {
            this.recordedAt = Instant.now();
        }
    }
}
