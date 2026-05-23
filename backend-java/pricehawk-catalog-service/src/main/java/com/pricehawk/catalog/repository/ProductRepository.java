package com.pricehawk.catalog.repository;

import com.pricehawk.catalog.domain.entity.Product;
import com.pricehawk.data.repository.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends BaseRepository<Product> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Full-text search via PostgreSQL tsvector — uses FTS index created in V2 migration.
    // Caller passes a tsquery string, e.g. "iphone & 14 & pro"
    @Query(value = """
        SELECT * FROM products
        WHERE to_tsvector('english', name || ' ' || COALESCE(brand, '')) @@ to_tsquery('english', :query)
        ORDER BY ts_rank(to_tsvector('english', name || ' ' || COALESCE(brand, '')), to_tsquery('english', :query)) DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Product> fullTextSearch(@Param("query") String tsQuery,
                                  @Param("limit") int limit,
                                  @Param("offset") int offset);

    @Query(value = """
        SELECT COUNT(*) FROM products
        WHERE to_tsvector('english', name || ' ' || COALESCE(brand, '')) @@ to_tsquery('english', :query)
        """, nativeQuery = true)
    long countFullTextSearch(@Param("query") String tsQuery);

    // Vector similarity search — finds k nearest products by name embedding.
    // name_embedding is NOT mapped in JPA (pgvector type). Must use native query with ::vector cast.
    @Query(value = """
        SELECT * FROM products
        ORDER BY name_embedding <=> :embedding::vector
        LIMIT :k
        """, nativeQuery = true)
    List<Product> findSimilarByEmbedding(@Param("embedding") String embeddingJson,
                                          @Param("k") int k);

    // Update AI analysis fields after AnalysisResultEvent is received
    @Modifying
    @Query("""
        UPDATE Product p SET
            p.aiSummary = :aiSummary,
            p.sentimentScore = :sentimentScore,
            p.totalReviews = :totalReviews,
            p.realReviewRatio = :realReviewRatio
        WHERE p.id = :productId
        """)
    void updateAnalysisResult(@Param("productId") UUID productId,
                               @Param("aiSummary") String aiSummary,
                               @Param("sentimentScore") BigDecimal sentimentScore,
                               @Param("totalReviews") int totalReviews,
                               @Param("realReviewRatio") BigDecimal realReviewRatio);

    // Update lowest price cache after any listing price change
    @Modifying
    @Query("""
        UPDATE Product p SET
            p.lowestPrice = :lowestPrice,
            p.lowestPriceSeller = :sellerName,
            p.lowestPriceSource = :domain
        WHERE p.id = :productId
        """)
    void updateLowestPrice(@Param("productId") UUID productId,
                            @Param("lowestPrice") BigDecimal lowestPrice,
                            @Param("sellerName") String sellerName,
                            @Param("domain") String domain);

    // Store the OpenAI embedding vector for a product (pgvector native type)
    @Modifying
    @Query(value = "UPDATE products SET name_embedding = :embedding::vector WHERE id = :productId",
           nativeQuery = true)
    void updateEmbedding(@Param("productId") UUID productId,
                          @Param("embedding") String embeddingJson);
}
