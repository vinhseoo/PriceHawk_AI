CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE categories (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(255) NOT NULL UNIQUE,
    parent_id  UUID REFERENCES categories(id),
    level      INT     DEFAULT 0,
    sort_order INT     DEFAULT 0,
    is_active  BOOLEAN DEFAULT TRUE
);

CREATE TABLE products (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(500)   NOT NULL,
    slug                VARCHAR(500)   NOT NULL UNIQUE,
    brand               VARCHAR(255),
    description         TEXT,
    thumbnail_url       VARCHAR(500),
    category_id         UUID REFERENCES categories(id),
    ai_summary          TEXT,
    sentiment_score     DECIMAL(3, 2),
    total_reviews       INT            DEFAULT 0,
    real_review_ratio   DECIMAL(3, 2),
    lowest_price        DECIMAL(15, 2),
    lowest_price_seller VARCHAR(255),
    lowest_price_source VARCHAR(255),
    name_embedding      vector(1536),
    created_at          TIMESTAMP      DEFAULT NOW(),
    updated_at          TIMESTAMP      DEFAULT NOW()
);

CREATE INDEX idx_products_embedding ON products USING ivfflat (name_embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand ON products(brand);
CREATE INDEX idx_products_name_trgm ON products USING gin(name gin_trgm_ops);

CREATE TABLE product_specs (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    specs      JSONB NOT NULL
);
CREATE INDEX idx_specs_gin ON product_specs USING gin(specs);

CREATE TABLE seller_listings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          UUID          NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    domain              VARCHAR(255)  NOT NULL,
    source_type         VARCHAR(20)   NOT NULL,
    scraper_tier        VARCHAR(20),
    seller_name         VARCHAR(255)  NOT NULL,
    seller_id           VARCHAR(255),
    seller_url          VARCHAR(1000),
    is_official_store   BOOLEAN       DEFAULT FALSE,
    external_url        VARCHAR(1000) NOT NULL,
    external_product_id VARCHAR(255),
    current_price       DECIMAL(15, 2),
    original_price      DECIMAL(15, 2),
    currency            VARCHAR(3)    DEFAULT 'VND',
    promotion_info      TEXT,
    trust_score         DECIMAL(3, 2),
    review_count        INT           DEFAULT 0,
    average_rating      DECIMAL(2, 1),
    fake_review_ratio   DECIMAL(3, 2),
    last_scraped_at     TIMESTAMP,
    scrape_status       VARCHAR(20)   DEFAULT 'PENDING',
    created_at          TIMESTAMP     DEFAULT NOW(),
    updated_at          TIMESTAMP     DEFAULT NOW()
);
CREATE INDEX idx_listings_product ON seller_listings(product_id);
CREATE INDEX idx_listings_domain ON seller_listings(domain);
CREATE INDEX idx_listings_price ON seller_listings(current_price);
CREATE INDEX idx_listings_trust ON seller_listings(trust_score DESC NULLS LAST);

CREATE TABLE price_history (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_listing_id UUID           NOT NULL REFERENCES seller_listings(id) ON DELETE CASCADE,
    price             DECIMAL(15, 2) NOT NULL,
    recorded_at       TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_price_history ON price_history(seller_listing_id, recorded_at DESC);

CREATE TABLE reviews (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_listing_id UUID      NOT NULL REFERENCES seller_listings(id) ON DELETE CASCADE,
    reviewer_name     VARCHAR(255),
    rating            SMALLINT,
    content           TEXT,
    review_date       TIMESTAMP,
    sentiment         VARCHAR(10),
    is_likely_fake    BOOLEAN   DEFAULT FALSE,
    fake_reason       VARCHAR(100),
    created_at        TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_reviews_listing ON reviews(seller_listing_id);

INSERT INTO categories (name, slug, level, sort_order) VALUES
    ('Điện thoại & Máy tính bảng', 'dien-thoai-may-tinh-bang', 0, 1),
    ('Laptop & Máy tính', 'laptop-may-tinh', 0, 2),
    ('Linh kiện & PC', 'linh-kien-pc', 0, 3),
    ('Thiết bị âm thanh', 'thiet-bi-am-thanh', 0, 4),
    ('Phụ kiện điện tử', 'phu-kien-dien-tu', 0, 5);
