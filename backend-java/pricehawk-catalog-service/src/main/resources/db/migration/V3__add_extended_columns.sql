-- V3: Add extended columns + missing BaseEntity audit columns to existing tables.

-- ─── product_specs ────────────────────────────────────────────────────────────
-- V1 created this table without created_at/updated_at.
-- ProductSpec extends BaseEntity which JPA Auditing expects these columns.
ALTER TABLE product_specs
    ADD COLUMN IF NOT EXISTS created_at   TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at   TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS dimensions   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS weight_grams INT,
    ADD COLUMN IF NOT EXISTS color        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS model        VARCHAR(100);

-- ─── reviews ──────────────────────────────────────────────────────────────────
-- V1 created reviews with created_at but not updated_at.
-- Add updated_at + new columns used by the Review entity.
ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS updated_at       TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS source_review_id VARCHAR(500),
    ADD COLUMN IF NOT EXISTS title            VARCHAR(500),
    ADD COLUMN IF NOT EXISTS fake_score       DECIMAL(3, 2),
    ADD COLUMN IF NOT EXISTS helpful_count    INT DEFAULT 0;

-- Partial unique index: same source review must not be inserted twice per listing
CREATE UNIQUE INDEX IF NOT EXISTS idx_review_source_dedup
    ON reviews (seller_listing_id, source_review_id)
    WHERE source_review_id IS NOT NULL;

-- ─── seller_listings ──────────────────────────────────────────────────────────
-- Add platform (enum), is_available flag, and sold_count for UX sorting.
ALTER TABLE seller_listings
    ADD COLUMN IF NOT EXISTS platform     VARCHAR(50)  DEFAULT 'OTHER',
    ADD COLUMN IF NOT EXISTS is_available BOOLEAN      DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS sold_count   INT          DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_listings_platform  ON seller_listings(platform);
CREATE INDEX IF NOT EXISTS idx_listings_available ON seller_listings(is_available);
