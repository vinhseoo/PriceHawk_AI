# PriceHawk AI — Development Plan & Progress Tracker

> Cập nhật trạng thái task ngay khi hoàn thành. Mỗi task phải có test trước khi đánh dấu ✅.
> Legend: ✅ Done | 🔄 In Progress | ⬜ Pending

---

## PHASE 1 — Project Foundation & Infrastructure
> Mục tiêu: Dựng bộ khung. Sau phase này, thêm service mới chỉ cần inherit.

- ✅ **1.1** Parent POM + Maven multi-module structure (`mvn clean install` pass)
- ✅ **1.2** `pricehawk-common` — ApiResponse, PageResponse, GlobalExceptionHandler, Exceptions, Enums (Platform, SourceType, ScraperTier), Event DTOs, Constants
- ✅ **1.3** `pricehawk-security-starter` — JwtTokenProvider, JwtProperties, SecurityAutoConfiguration
- ✅ **1.4** `pricehawk-data-starter` — BaseEntity (UUID + JPA auditing), BaseRepository, DataAutoConfiguration
- ✅ **1.5** `pricehawk-messaging-starter` — RabbitMQConfig (exchanges + queues + bindings), EventPublisher, MessagingAutoConfiguration
- ✅ **1.6** Python `shared/` — config.py, models.py (mirror Java DTOs), rabbitmq_client.py, exceptions.py, utils.py
- ✅ **1.7** `docker-compose.yml` — PostgreSQL (pgvector, init 3 DBs), Redis, RabbitMQ + `scripts/init-databases.sql`
- ✅ **1.8** `pricehawk-gateway` — Routing (6 services), JwtAuthGatewayFilter, CORS, application.yml
- ✅ **1.9** `pricehawk-user-service` — Scaffold: Application, SecurityConfig, application.yml, Flyway V1 migration
- ✅ **1.10** `pricehawk-catalog-service` — Scaffold: Application, application.yml, Flyway V1 migration (pgvector + full schema)
- ✅ **1.11** `pricehawk-notification-service` — Scaffold: Application, WebSocketConfig, application.yml
- ✅ **1.12** Scraper Service scaffold — FastAPI app, 3-tier structure (tier1/2/3), Alembic migration, SQLAlchemy models
- ✅ **1.13** AI Analyzer Service scaffold — FastAPI app, LLM clients (OpenAI + Anthropic), SemanticCache
- ✅ **1.14** Frontend scaffold — Next.js 14, TypeScript types, apiClient, Zustand stores, TanStack Query hooks, routing
- ✅ **1.15** CLAUDE.md rules — root + backend-java + backend-python + frontend

**Phase 1 completed:** 2026-05-22

---

## PHASE 2 — User Service (Auth + Profile + Wishlist)
> Mục tiêu: Register, login, JWT, profile, wishlist — 1 service xử lý hết.

### 2.1 Entities & Migration
- ⬜ **2.1.1** JPA Entities: `User`, `Role`, `UserRole`, `RefreshToken`, `Wishlist`, `WishlistItem`, `SearchHistory`
- ⬜ **2.1.2** Repositories: `UserRepository`, `RoleRepository`, `RefreshTokenRepository`, `WishlistRepository`
- ⬜ **2.1.3** MapStruct mappers: `UserMapper` (User ↔ UserDTO, UserDTO ↔ RegisterRequest)

### 2.2 Authentication
- ⬜ **2.2.1** `POST /api/v1/auth/register` — BCrypt hash, assign role USER, return tokens
- ⬜ **2.2.2** `POST /api/v1/auth/login` — verify password, generate access + refresh token
- ⬜ **2.2.3** `POST /api/v1/auth/refresh-token` — validate refresh token, issue new access token
- ⬜ **2.2.4** `POST /api/v1/auth/logout` — invalidate refresh token
- ⬜ **2.2.5** Test: register → login → refresh → logout flow (integration test với testcontainers)

### 2.3 OAuth2 Social Login
- ⬜ **2.3.1** `POST /api/v1/auth/oauth2/google` — verify Google idToken, auto-create/link account
- ⬜ **2.3.2** Test: mock Google token verification

### 2.4 User Profile & Wishlist
- ⬜ **2.4.1** `GET /api/v1/users/me` — lấy profile từ X-User-Id header
- ⬜ **2.4.2** `PUT /api/v1/users/me` — update fullName, avatar, preferences
- ⬜ **2.4.3** `GET/POST/DELETE /api/v1/users/me/wishlist` — CRUD wishlist items
- ⬜ **2.4.4** `GET /api/v1/users/me/history` — lịch sử tìm kiếm (phân trang)
- ⬜ **2.4.5** Test: profile CRUD, wishlist add/remove, search history pagination

### 2.5 Subscription & Rate Limiting
- ⬜ **2.5.1** Daily search count tracking: tăng counter, reset mỗi ngày
- ⬜ **2.5.2** Check limit middleware: FREE = 5/ngày, PREMIUM = unlimited
- ⬜ **2.5.3** Test: rate limit enforcement, daily reset logic

---

## PHASE 3 — Catalog Service
> Mục tiêu: Core data model Product → nhiều Seller Listings. Search, filter, price history.

### 3.1 Entities & Repositories
- ⬜ **3.1.1** JPA Entities: `Product`, `ProductSpec`, `SellerListing`, `PriceHistory`, `Category`, `Review`
- ⬜ **3.1.2** Repositories + Specifications cho dynamic filtering
- ⬜ **3.1.3** MapStruct mappers cho tất cả entities

### 3.2 Category API
- ⬜ **3.2.1** `GET /api/v1/products/categories` — category tree
- ⬜ **3.2.2** Test: category tree structure, parent-child relationships

### 3.3 Product APIs
- ⬜ **3.3.1** `GET /api/v1/products` — list với dynamic filter (brand, category, priceMin/Max, sort)
- ⬜ **3.3.2** `GET /api/v1/products/{slug}` — product detail
- ⬜ **3.3.3** `GET /api/v1/products/{id}/listings` — tất cả seller listings, sort by price/trust
- ⬜ **3.3.4** `GET /api/v1/products/{id}/listings/{listingId}/price-history` — biến động giá
- ⬜ **3.3.5** `GET /api/v1/products/{id}/reviews` — reviews (có filter isLikelyFake)
- ⬜ **3.3.6** `GET /api/v1/products/compare?ids=id1,id2,id3` — so sánh tối đa 3 sản phẩm
- ⬜ **3.3.7** Test: filter/sort correctness, price history query, compare response structure

### 3.4 Search APIs
- ⬜ **3.4.1** `GET /api/v1/search?q=text` — full-text search (PostgreSQL FTS + pg_trgm)
- ⬜ **3.4.2** `POST /api/v1/search/url` — submit URL → trigger scrape → return jobId
- ⬜ **3.4.3** `POST /api/v1/search/image` — upload ảnh → vector query (pgvector cosine)
- ⬜ **3.4.4** Test: text search ranking, URL submission flow, vector search results

### 3.5 RabbitMQ Consumers
- ⬜ **3.5.1** Consumer `product.scraped` → upsert Product + SellerListings + PriceHistory + gọi AI
- ⬜ **3.5.2** Consumer `analysis.completed` → update ai_summary, sentiment_score, trust_score
- ⬜ **3.5.3** Publish `price.updated` khi giá thay đổi so với lần trước
- ⬜ **3.5.4** Test: consumer idempotency (xử lý trùng message), price change detection

---

## PHASE 4 — Scraper Service (Python) — 3-Tier Architecture
> Mục tiêu: Scrape BẤT KỲ website nào — từ API cho sàn lớn đến AI cho website unknown.

### 4.1 Core Infrastructure
- ⬜ **4.1.1** RabbitMQ consumer cho `scrape.request.queue` — dispatch tới orchestrator
- ⬜ **4.1.2** Publish `product.scraped` sau khi scrape xong
- ⬜ **4.1.3** Job status updates (PENDING → IN_PROGRESS → COMPLETED/FAILED)
- ⬜ **4.1.4** Test: consumer dispatch, job lifecycle, error handling + retry

### 4.2 Tier 1 — Shopee (hoàn thiện)
- ⬜ **4.2.1** Multi-seller discovery: tìm tất cả shops bán cùng sản phẩm trên Shopee
- ⬜ **4.2.2** Review pagination: lấy đủ reviews (không chỉ trang 1)
- ⬜ **4.2.3** Proxy rotation + rate limiting
- ⬜ **4.2.4** Test: parse URL, extract product data, multi-seller list, rate limit behavior

### 4.3 Tier 1 — Lazada & Tiki
- ⬜ **4.3.1** `LazadaScraper` — reverse-engineered API endpoints
- ⬜ **4.3.2** `TikiScraper` — Tiki API + seller list
- ⬜ **4.3.3** Test: cả 2 scrapers với product URL thật

### 4.4 Tier 2 — ConfigBasedScraper (hoàn thiện)
- ⬜ **4.4.1** Verify 5 configs đã seed: TGDĐ, FPT Shop, CellphoneS, Phong Vũ, GearVN
- ⬜ **4.4.2** Test từng config với URL thật — verify extraction accuracy
- ⬜ **4.4.3** Admin API: POST /configs, PUT /configs/{id}, approve AI suggestions
- ⬜ **4.4.4** Test: create config → scrape URL → verify fields extracted correctly

### 4.5 Tier 3 — AIGenericScraper (hoàn thiện)
- ⬜ **4.5.1** Kết nối thật OpenAI client vào scraper service
- ⬜ **4.5.2** HTML cleaning: loại bỏ scripts/ads hiệu quả → giảm token
- ⬜ **4.5.3** Auto-generate config → lưu AI_GENERATED → admin approve flow
- ⬜ **4.5.4** Test: scrape URL unknown → extract data → verify config suggestion được tạo

### 4.6 Scheduled Re-scraping
- ⬜ **4.6.1** Cron job mỗi 12h: re-scrape tất cả active seller listings
- ⬜ **4.6.2** Test: scheduler trigger, chỉ scrape listings đang track, không duplicate jobs

---

## PHASE 5 — AI Analyzer Service (Python)
> Mục tiêu: Biến raw data thành insights — fake detection, sentiment, trust score.

### 5.1 Infrastructure
- ⬜ **5.1.1** RabbitMQ consumer `analysis.request.queue` → dispatch tới analyzers
- ⬜ **5.1.2** Publish `analysis.completed` sau khi xong
- ⬜ **5.1.3** Test: consumer, publish, semantic cache hit/miss

### 5.2 Review Analysis Pipeline
- ⬜ **5.2.1** `FakeReviewDetector` — rule-based (SHORT_5STAR, DUPLICATE, EMPTY) + LLM-assisted cho borderline cases
- ⬜ **5.2.2** Sentiment analysis: POSITIVE/NEGATIVE/NEUTRAL per review
- ⬜ **5.2.3** `ReviewSummarizer` — top 3 pros/cons + recommendation (Vietnamese)
- ⬜ **5.2.4** Test: fake detection accuracy trên sample data, summarizer output quality

### 5.3 Trust Score & Embeddings
- ⬜ **5.3.1** `TrustScoreCalculator` — wire vào real data (rating, fake_ratio, review_count, official_store)
- ⬜ **5.3.2** `EmbeddingGenerator` — text embedding qua OpenAI `text-embedding-3-small`
- ⬜ **5.3.3** Test: trust score range (0-1), embedding dimensions (1536), cache behavior

### 5.4 Product Matching
- ⬜ **5.4.1** `ProductMatcher` — TF-IDF name similarity + specs key matching
- ⬜ **5.4.2** Cross-source deduplication: tránh tạo duplicate Product records
- ⬜ **5.4.3** Test: same product từ Shopee + TGDĐ → match về 1 product_id

---

## PHASE 6 — Notification Service
> Mục tiêu: Real-time push + email alerts khi phân tích xong hoặc giá thay đổi.

- ⬜ **6.1** WebSocket STOMP setup + JWT auth handshake (validate X-User-Id từ header)
- ⬜ **6.2** Redis session store cho WebSocket connections
- ⬜ **6.3** Consumer `analysis.completed` → push WS notification đến user
- ⬜ **6.4** Consumer `price.updated` → check wishlist target_price → push alert
- ⬜ **6.5** Email digest (JavaMailSender): tổng hợp price alerts hàng tuần
- ⬜ **6.6** Test: WS connection lifecycle, price alert trigger, email template

---

## PHASE 7 — Frontend (Next.js)
> Mục tiêu: Web app hoàn chỉnh, responsive, SEO-friendly.

### 7.1 Core Layout & Auth
- ⬜ **7.1.1** Header component: logo, search bar, auth buttons, notification bell
- ⬜ **7.1.2** Login page: form (email/password) + Google OAuth button
- ⬜ **7.1.3** Register page: form + validation
- ⬜ **7.1.4** Auth flow: setAuth → store token → redirect, handle 401 globally
- ⬜ **7.1.5** Test: login/register forms, token persistence, redirect logic

### 7.2 Landing Page
- ⬜ **7.2.1** SearchHero: wire URL input → POST /scrape/url → poll job status → redirect to product
- ⬜ **7.2.2** Text search: wire → GET /search?q= → hiển thị kết quả
- ⬜ **7.2.3** Image upload: wire → POST /search/image

### 7.3 Product Detail Page ★ (Core)
- ⬜ **7.3.1** ProductHeader: tên, brand, thumbnail gallery, AI summary badge, lowest price
- ⬜ **7.3.2** SellerTable: tất cả sellers sort/filter, TierBadge (API/Config/AI), TrustBadge, affiliate link
- ⬜ **7.3.3** PriceHistoryChart: Recharts line chart per seller, 30d/90d/6m toggle
- ⬜ **7.3.4** ReviewAnalysis: summary + sentiment donut + fake alert + top reviews
- ⬜ **7.3.5** SpecsTable: JSONB specs + "So sánh" button
- ⬜ **7.3.6** Test: render với real API data, sort/filter SellerTable, chart date range

### 7.4 Compare & Dashboard
- ⬜ **7.4.1** Compare page: side-by-side 2-3 products (specs diff highlight)
- ⬜ **7.4.2** Dashboard: wishlist management, search history, subscription badge
- ⬜ **7.4.3** Admin page `/admin/scraper-configs`: list configs, review AI suggestions, test scrape

### 7.5 Real-time & SEO
- ⬜ **7.5.1** WebSocket: `useWebSocket` hook → toast notification khi analysis.completed
- ⬜ **7.5.2** Wishlist price alert toast khi price.updated
- ⬜ **7.5.3** SEO: dynamic meta per product, JSON-LD (Product + AggregateOffer + AggregateRating)
- ⬜ **7.5.4** Auto sitemap từ product slugs
- ⬜ **7.5.5** Test: meta tags correctness, JSON-LD structure, sitemap generation

---

## PHASE 8 — End-to-End Integration & Hardening
> Mục tiêu: Toàn bộ luồng chạy thông suốt từ URL paste đến hiển thị kết quả.

- ⬜ **8.1** E2E test: paste URL Shopee → scrape → analyze → hiển thị product detail
- ⬜ **8.2** E2E test: paste URL TGDĐ (Tier 2) → tương tự
- ⬜ **8.3** E2E test: paste URL unknown → Tier 3 AI → config suggestion tạo ra
- ⬜ **8.4** Load test: Gateway rate limiting (100 req/min anonymous)
- ⬜ **8.5** Security review: JWT validation, SQL injection, XSS headers
- ⬜ **8.6** Affiliate link tracking: verify UTM/affiliate params được append đúng

---

## PHASE 9 — Deployment MVP
> Mục tiêu: Deploy lên production, monitor, ship.

- ⬜ **9.1** Docker images: build + push tất cả 6 services lên registry
- ⬜ **9.2** `docker-compose.services.yml` production config (env vars, health checks, restart policies)
- ⬜ **9.3** GitHub Actions CI: test → build → push image trên merge to main
- ⬜ **9.4** Deploy lên VPS / Railway / Fly.io
- ⬜ **9.5** Frontend deploy lên Vercel + set NEXT_PUBLIC_API_URL
- ⬜ **9.6** Monitoring: logs correlation ID, `/actuator/health` checks, RabbitMQ alert
- ⬜ **9.7** Smoke test production: full E2E flow trên môi trường thật

---

## Cách cập nhật file này

Khi hoàn thành 1 task, đổi `⬜` thành `✅` và ghi ngày vào cuối dòng nếu cần.

```
⬜ **2.2.1** → ✅ **2.2.1** POST /auth/register — 2026-05-25
```

**QUAN TRỌNG:** Một task chỉ được đánh dấu ✅ khi:
1. Code đã implement xong
2. Test đã chạy pass (unit test hoặc manual verify theo test case mô tả)
3. Không có regression trên các task ✅ trước đó
