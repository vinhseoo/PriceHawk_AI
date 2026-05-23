-- Partial unique index: một seller chỉ có 1 listing cho mỗi product ID trên cùng domain.
-- WHERE external_product_id IS NOT NULL — cho phép row không có external_product_id (Tier 3 scraper chưa extract được).
CREATE UNIQUE INDEX IF NOT EXISTS idx_listing_source_unique
    ON seller_listings (domain, external_product_id)
    WHERE external_product_id IS NOT NULL;

-- Full-text search index trên product name (tiếng Việt + simple dictionary)
CREATE INDEX IF NOT EXISTS idx_products_fts
    ON products USING gin(to_tsvector('simple', name || ' ' || COALESCE(brand, '')));
