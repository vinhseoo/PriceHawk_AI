package com.pricehawk.catalog.domain.entity;

import com.pricehawk.data.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "product_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSpec extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    // Flexible key-value specs stored as JSONB (V1 column: specs JSONB NOT NULL).
    // e.g. {"RAM": "16GB", "Storage": "512GB SSD", "Display": "15.6 inch"}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, String> specs = new HashMap<>();

    // Structured fields below are added via V3 migration for typed querying
    @Column(length = 100)
    private String dimensions;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(length = 50)
    private String color;

    @Column(length = 100)
    private String model;
}
