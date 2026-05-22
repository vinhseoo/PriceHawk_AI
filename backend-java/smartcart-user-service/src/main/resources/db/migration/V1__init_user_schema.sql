CREATE TABLE users (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password_hash         VARCHAR(255),
    auth_provider         VARCHAR(20)  DEFAULT 'LOCAL',
    provider_id           VARCHAR(255),
    full_name             VARCHAR(255),
    avatar_url            VARCHAR(500),
    subscription_plan     VARCHAR(20)  DEFAULT 'FREE',
    daily_search_count    INT          DEFAULT 0,
    daily_search_reset_at TIMESTAMP,
    preferences           JSONB        DEFAULT '{}',
    is_active             BOOLEAN      DEFAULT TRUE,
    last_login_at         TIMESTAMP,
    created_at            TIMESTAMP    DEFAULT NOW(),
    updated_at            TIMESTAMP    DEFAULT NOW()
);

CREATE TABLE roles (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP   NOT NULL,
    created_at TIMESTAMP   DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

CREATE TABLE wishlists (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(255) DEFAULT 'My Wishlist',
    created_at TIMESTAMP    DEFAULT NOW()
);

CREATE TABLE wishlist_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wishlist_id  UUID           NOT NULL REFERENCES wishlists(id) ON DELETE CASCADE,
    product_id   UUID           NOT NULL,
    target_price DECIMAL(15, 2),
    created_at   TIMESTAMP DEFAULT NOW(),
    UNIQUE (wishlist_id, product_id)
);

CREATE TABLE search_histories (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    query_type   VARCHAR(20) NOT NULL,
    query_value  TEXT        NOT NULL,
    result_count INT,
    created_at   TIMESTAMP   DEFAULT NOW()
);

CREATE INDEX idx_search_histories_user ON search_histories(user_id, created_at DESC);

INSERT INTO roles (name) VALUES ('USER'), ('PREMIUM_USER'), ('ADMIN');
