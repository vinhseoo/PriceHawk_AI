# PriceHawk AI — Project Blueprint & Technical Documentation (v3)

> **AI Shopping Assistant & Aggregator**
> Hệ thống hỗ trợ người dùng tìm kiếm, so sánh, đánh giá sản phẩm đa nền tảng bằng AI.

---

## MỤC LỤC

1. [Tổng quan dự án](#1-tổng-quan-dự-án)
2. [Kiến trúc hệ thống & Mô tả Services](#2-kiến-trúc-hệ-thống--mô-tả-services)
3. [Công nghệ sử dụng (Tech Stack)](#3-công-nghệ-sử-dụng-tech-stack)
4. [Cấu trúc dự án & Base/Common Layer](#4-cấu-trúc-dự-án--basecommon-layer)
5. [Database Design](#5-database-design)
6. [Chi tiết các Phase phát triển](#6-chi-tiết-các-phase-phát-triển)
7. [Frontend — Web Application](#7-frontend--web-application)
8. [Coding Conventions & Rules](#8-coding-conventions--rules)
9. [Deployment & Infrastructure](#9-deployment--infrastructure)

---

## 1. Tổng quan dự án

### 1.1. Bài toán (Problem Statement)

Người tiêu dùng hiện nay đối mặt với 4 vấn đề lớn khi mua sắm online:

- **Bội thực thông tin, phân tán nguồn bán**: Cùng 1 sản phẩm xuất hiện ở hàng chục nơi — Shopee, Lazada, Tiki, Thế Giới Di Động, FPT Shop, CellphoneS, Phong Vũ, GearVN và hàng trăm website bán hàng khác. Mỗi nơi giá khác nhau, chương trình khuyến mãi khác nhau.
- **Cùng 1 sàn, hàng chục seller bán cùng sản phẩm**: Trên Shopee, cùng 1 model laptop có 50+ shop bán với giá chênh 500K–3 triệu. Shop nào uy tín? Shop nào chính hãng? Không có cách biết nhanh.
- **Ma trận review ảo**: Seeding, bot, review mua tạo đánh giá giả. Người dùng không phân biệt được.
- **Sale ảo / Giá ảo**: Tăng giá trước khi "giảm giá". Không có dữ liệu lịch sử giá để kiểm chứng.

### 1.2. Giải pháp (Solution)

PriceHawk AI là nền tảng trợ lý mua sắm thông minh:

- **Input đa phương thức**: Nhận URL sản phẩm (từ BẤT KỲ website nào), hình ảnh chụp, hoặc từ khóa.
- **Intelligent Scraper**: Không giới hạn ở một danh sách cố định các website. Hệ thống có thể tự hiểu cấu trúc BẤT KỲ trang web bán hàng nào nhờ AI, đồng thời có scraper chuyên biệt cho các nền tảng lớn để đảm bảo chất lượng + tốc độ.
- **Deep Review Analysis**: AI phân tích review, lọc seeding/bot, tóm tắt ưu nhược điểm thực.
- **Cross-platform & Cross-seller Comparison**: So sánh giá giữa các sàn, giữa các seller trên cùng 1 sàn, và giữa các website bán lẻ.
- **Trust Score**: Chấm điểm uy tín từng seller dựa trên dữ liệu thực.
- **Price History & Alert**: Theo dõi biến động giá, cảnh báo khi giá tốt.

### 1.3. Nguồn dữ liệu — Thiết kế 3 tầng (3-Tier Data Source)

Thay vì cứng danh sách website, hệ thống thiết kế theo 3 tầng ưu tiên:

**Tier 1 — API-based Scrapers** (chất lượng cao nhất, nhanh nhất):
- Các sàn TMĐT lớn có API nội bộ hoặc public: Shopee, Lazada, Tiki.
- Scraper gọi trực tiếp API → dữ liệu structured (JSON) → parse nhanh, chính xác.
- Hỗ trợ multi-seller discovery (tìm tất cả shops bán cùng sản phẩm).

**Tier 2 — Config-based Scrapers** (chất lượng cao, admin thêm mới được):
- Các website bán hàng phổ biến đã được cấu hình sẵn CSS selectors / XPath.
- Config lưu trong DB, admin có thể thêm website mới qua giao diện quản trị mà KHÔNG cần deploy lại code.
- Ví dụ ban đầu: Thế Giới Di Động, FPT Shop, CellphoneS, Phong Vũ, GearVN, Hnam, Amazon...
- Mở rộng: bất kỳ ai cũng có thể contribute config cho website mới.

**Tier 3 — AI-powered Generic Scraper** (linh hoạt nhất, hoạt động với mọi website):
- Khi user paste URL từ một website CHƯA CÓ trong Tier 1 & Tier 2.
- Hệ thống dùng Playwright render full page → trích xuất HTML → gửi cho LLM.
- LLM phân tích cấu trúc trang, tự động nhận diện và trích xuất: tên sản phẩm, giá, specs, ảnh, reviews.
- Kết quả extraction có thể được admin review và lưu lại thành config Tier 2 cho lần sau (auto-learning).

```
User paste URL
    │
    ▼
URL Parser → Detect domain
    │
    ├── Domain ∈ Tier 1 (Shopee/Lazada/Tiki)?
    │       → Gọi API-based scraper (nhanh, chính xác)
    │
    ├── Domain ∈ Tier 2 (có config trong DB)?
    │       → Gọi Config-based scraper (dùng CSS selectors từ DB)
    │
    └── Domain không nhận diện?
            → Gọi AI Generic scraper (Playwright + LLM extraction)
            → Kết quả tốt? → Gợi ý admin lưu config → Promote lên Tier 2
```

### 1.4. Khái niệm cốt lõi: Product → Seller Listing

```
Product (Sản phẩm chuẩn hóa)
  └── "iPhone 16 Pro Max 256GB Titan Đen"
       │
       ├── Seller Listing 1: Shopee — "Minh Tuấn Mobile"        — 28.500.000đ
       ├── Seller Listing 2: Shopee — "Di Động Việt Official"    — 28.990.000đ
       ├── Seller Listing 3: Shopee — "CellphoneS Official"      — 29.190.000đ
       ├── Seller Listing 4: Lazada — "Apple Official Store"      — 29.490.000đ
       ├── Seller Listing 5: thegioididong.com                    — 28.990.000đ + trả góp 0%
       ├── Seller Listing 6: fptshop.com.vn                       — 28.790.000đ + tặng AirPods
       ├── Seller Listing 7: cellphones.com.vn                    — 28.490.000đ
       ├── Seller Listing 8: phongvu.vn                           — 29.090.000đ
       └── Seller Listing 9: https://random-shop.com/iphone-16   — 27.990.000đ (AI scraped)
```

Mỗi **Seller Listing** = 1 offer, có giá riêng, seller riêng, review riêng, trust score riêng. Listing thứ 9 đến từ một website bất kỳ mà AI tự extract được.

### 1.5. MVP Scope — Ngách khởi điểm

Giai đoạn đầu tập trung vào **sản phẩm công nghệ** (Laptop, PC, linh kiện, điện thoại):
- Thông số kỹ thuật rõ ràng, dễ cấu trúc hóa.
- Giá trị cao → user sẵn sàng research kỹ.
- Nhiều nguồn data (sàn TMĐT + chuỗi bán lẻ + website nhỏ).
- Nhu cầu so sánh cực lớn.

### 1.6. Mô hình kiếm tiền (Monetization)

| Mô hình | Mô tả | Giai đoạn |
|---------|-------|-----------|
| Affiliate Marketing | Tracking link vào nút "Đến nơi bán", hoa hồng khi user mua | MVP |
| Freemium Subscription | 5 tra cứu/ngày miễn phí, Pro không giới hạn + nâng cao | Growth |
| B2B Data API | Dữ liệu biến động giá, xu hướng bán cho brand/retailer | Scale |
| Sponsored Placement | Shop/retailer uy tín trả phí highlight | Scale |

---

## 2. Kiến trúc hệ thống & Mô tả Services

### 2.1. Service Map — 6 Services

Giảm từ 7 xuống 6 bằng cách gộp Auth + User thành 1 service duy nhất.

```
                         ┌──────────────────┐
                         │   Client (Web)    │
                         │   Next.js         │
                         └────────┬─────────┘
                                  │
                         ┌────────▼─────────┐
                         │   API Gateway     │
                         │ (Spring Cloud GW) │
                         │ Rate Limit + JWT  │
                         └────────┬─────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
     ┌────────▼────────┐ ┌───────▼───────┐ ┌─────────▼────────┐
     │  User Service   │ │ Catalog       │ │  Notification    │
     │  (Java)         │ │ Service       │ │  Service         │
     │  Auth + Profile │ │ (Java)        │ │  (Java)          │
     │  + Wishlist     │ │ Products +    │ │  WebSocket +     │
     │  + History      │ │ Sellers +     │ │  Email           │
     └────────────────┘ │ Search        │ └──────────────────┘
                         └───────┬───────┘
                                 │
                          ┌──────▼───────┐
                          │  RabbitMQ    │
                          └──┬───────┬───┘
                             │       │
                    ┌────────▼──┐ ┌──▼──────────┐
                    │ Scraper   │ │ AI Analyzer │
                    │ Service   │ │ Service     │
                    │ (Python)  │ │ (Python)    │
                    └───────────┘ └─────────────┘
```

### 2.2. Chi tiết từng Service

---

#### SERVICE 1: API Gateway

| Thuộc tính | Giá trị |
|-----------|---------|
| Ngôn ngữ | Java |
| Framework | Spring Cloud Gateway |
| Port (Dev) | 8080 |
| Database | Không |

**Vai trò**: Cửa ngõ duy nhất, mọi request từ client đi qua đây.

**Trách nhiệm**:
- **Routing**: Điều hướng theo URL path tới đúng downstream service.
  - `/api/v1/auth/**` → User Service (8081)
  - `/api/v1/users/**` → User Service (8081)
  - `/api/v1/products/**` → Catalog Service (8082)
  - `/api/v1/search/**` → Catalog Service (8082)
  - `/api/v1/scrape/**` → Scraper Service (8083)
  - `/api/v1/analysis/**` → AI Analyzer Service (8084)
  - `/ws/**` → Notification Service (8085)
- **Rate Limiting** (Redis): 100 req/phút anonymous, 500 req/phút authenticated.
- **JWT Validation**: Giải mã token → forward `X-User-Id`, `X-User-Roles` headers xuống downstream. Token invalid → reject ngay.
- **CORS**: Quản lý tập trung.
- **Request Logging**: Log request ID, latency, status code.

---

#### SERVICE 2: User Service (Auth + Profile + Wishlist)

| Thuộc tính | Giá trị |
|-----------|---------|
| Ngôn ngữ | Java |
| Framework | Spring Boot + Spring Security |
| Port (Dev) | 8081 |
| Database | PostgreSQL (user_db) |

**Vai trò**: Quản lý toàn bộ vòng đời người dùng — từ đăng ký, đăng nhập, đến dữ liệu cá nhân hóa. Gộp Auth + Profile vào 1 service vì:
- Ở quy mô MVP, tách nhỏ quá chỉ thêm overhead (2 DB, 2 deployment, feign call qua lại).
- Auth data và User data luôn cần nhau (login xong lấy profile ngay, register xong tạo profile ngay).
- Khi nào traffic auth thực sự lớn đến mức cần scale riêng thì mới tách — premature optimization là kẻ thù.

**Trách nhiệm**:
- **Authentication**:
  - Đăng ký email + password (BCrypt hash, assign role USER).
  - Đăng nhập → cấp JWT access token (15 phút) + refresh token (7 ngày).
  - Token refresh.
  - OAuth2 Social Login (Google, Facebook): nhận idToken → verify → auto-create/link account.
  - Password reset (qua email).
- **Authorization**: Role-based (USER, PREMIUM_USER, ADMIN).
- **User Profile**: CRUD thông tin cá nhân (fullName, avatar, preferences).
- **Wishlist**: Danh sách sản phẩm đang canh giá. Mỗi item gắn product_id + target_price.
- **Search History**: Lưu lịch sử tra cứu (query type, value, timestamp).
- **Subscription**: Quản lý gói Free/Premium, đếm lượt tra cứu/ngày.

**Giao tiếp**:
- Nhận request từ Gateway.
- Được gọi bởi Notification Service (OpenFeign) để lấy email/preferences.

---

#### SERVICE 3: Catalog Service

| Thuộc tính | Giá trị |
|-----------|---------|
| Ngôn ngữ | Java |
| Framework | Spring Boot + Spring Data JPA |
| Port (Dev) | 8082 |
| Database | PostgreSQL (catalog_db) + pgvector |

**Vai trò**: Kho dữ liệu sản phẩm trung tâm. Service phức tạp nhất — handle mô hình **Product → nhiều Seller Listings** từ nhiều nguồn khác nhau.

**Trách nhiệm**:
- **Product Master Data**: 1 sản phẩm thực = 1 record, dù xuất hiện ở 50 seller khác nhau.
- **Seller Listing Management**: Mỗi product có nhiều `seller_listings`, mỗi listing = 1 offer từ 1 seller/website cụ thể.
- **Product Specs** (JSONB): Thông số kỹ thuật linh hoạt.
- **Price History**: Ghi lịch sử giá riêng cho từng seller listing.
- **Category Tree**: Quản lý cây danh mục sản phẩm.
- **Search & Filtering**: Full-text search + JSONB query + dynamic filters.
- **AI Results Storage**: Lưu ai_summary, sentiment_score, trust_score.
- **Consume events** từ RabbitMQ: `product.scraped` (data từ Scraper), `analysis.completed` (kết quả AI).

---

#### SERVICE 4: Scraper Service — ★ INTELLIGENT 3-TIER ARCHITECTURE

| Thuộc tính | Giá trị |
|-----------|---------|
| Ngôn ngữ | **Python** |
| Framework | **FastAPI** + Scrapy + Playwright |
| Port (Dev) | 8083 |
| Database | PostgreSQL (scraper_db) |

**Vai trò**: Thu thập dữ liệu sản phẩm từ BẤT KỲ website nào, không giới hạn danh sách cố định.

**Lý do chọn Python**: Scrapy ecosystem mạnh nhất, Playwright stealth mode tốt nhất, cộng đồng scraping + AI lớn nhất.

##### Tier 1 — API-based Scrapers (hardcoded, chất lượng cao nhất)

Dành cho các sàn TMĐT lớn có API endpoints hoặc internal API đã được reverse-engineer:

- **ShopeeScraper**: Gọi Shopee internal API → product data + tất cả sellers + reviews.
- **LazadaScraper**: Gọi Lazada API endpoints → product + seller list.
- **TikiScraper**: Gọi Tiki API → product + sellers.

Đặc điểm Tier 1:
- Dữ liệu structured (JSON) → parse nhanh, chính xác 99%+.
- Hỗ trợ **multi-seller discovery** — tự động tìm tất cả shops bán cùng sản phẩm trên sàn.
- Hardcoded trong code vì logic phức tạp, cần maintain riêng khi sàn thay đổi API.

##### Tier 2 — Config-based Scrapers (dynamic, admin quản lý)

Dành cho các website bán hàng có cấu trúc HTML ổn định. **KHÔNG hardcode trong code** — config lưu trong DB.

**Bảng `scraper_configs` trong scraper_db**:
```
id | domain               | name          | config (JSONB)
---+----------------------+---------------+--------------------------------------
1  | thegioididong.com    | TGDĐ          | { selectors: { product_name: "h1.product-name",
   |                      |               |     price: ".box-price .product-price",
   |                      |               |     original_price: ".box-price .old-price",
   |                      |               |     specs: ".parameter-list li",
   |                      |               |     reviews: ".comment-list .comment-item",
   |                      |               |     review_text: ".comment-content",
   |                      |               |     review_rating: ".comment-star .star-on",
   |                      |               |     images: ".gallery-image img",
   |                      |               |     promotion: ".box-promotion li"
   |                      |               |   },
   |                      |               |   type: "static", -- static = HTTP GET, dynamic = Playwright
   |                      |               |   pagination: { review_url: "/ajax/review?id={id}&page={page}" }
   |                      |               | }
2  | fptshop.com.vn       | FPT Shop      | { selectors: { ... }, type: "static" }
3  | cellphones.com.vn    | CellphoneS    | { selectors: { ... }, type: "dynamic" }
4  | phongvu.vn           | Phong Vũ      | { selectors: { ... }, type: "static" }
5  | gearvn.com           | GearVN        | { selectors: { ... }, type: "static" }
6  | hnam.com.vn          | Hnam Mobile   | { selectors: { ... }, type: "dynamic" }
```

**ConfigBasedScraper** — Generic scraper engine:
```python
class ConfigBasedScraper(BaseScraper):
    """Scraper sử dụng config từ DB. Không cần viết code riêng cho từng website."""

    def __init__(self, config: ScraperConfig):
        self.config = config  # Load từ DB dựa trên domain

    async def scrape_product(self, url: str) -> ScrapedProductData:
        if self.config.type == "static":
            html = await self.http_get(url)  # Simple HTTP GET
        else:
            html = await self.playwright_render(url)  # Headless browser

        soup = BeautifulSoup(html, 'html.parser')
        return ScrapedProductData(
            name=self.extract(soup, self.config.selectors.product_name),
            price=self.extract_price(soup, self.config.selectors.price),
            original_price=self.extract_price(soup, self.config.selectors.original_price),
            specs=self.extract_specs(soup, self.config.selectors.specs),
            images=self.extract_images(soup, self.config.selectors.images),
            # ...
        )
```

**Admin có thể thêm website mới qua API**:
```
POST /api/v1/scrape/configs
{
  "domain": "nguyenkim.com",
  "name": "Nguyễn Kim",
  "config": {
    "selectors": {
      "product_name": "h1.product-title",
      "price": ".product-price .final-price",
      ...
    },
    "type": "static"
  }
}
```
→ Ngay lập tức hệ thống có thể scrape nguyenkim.com mà không cần deploy lại code.

##### Tier 3 — AI Generic Scraper (zero-config, hoạt động với BẤT KỲ website)

Khi user paste URL từ website chưa có trong Tier 1 và Tier 2:

```python
class AIGenericScraper(BaseScraper):
    """Scraper dùng LLM để hiểu cấu trúc BẤT KỲ trang web nào."""

    async def scrape_product(self, url: str) -> ScrapedProductData:
        # 1. Render page bằng Playwright (đảm bảo JS-rendered content)
        html = await self.playwright_render(url)

        # 2. Clean HTML: loại bỏ scripts, styles, quảng cáo → giữ content chính
        cleaned_html = self.clean_html(html)

        # 3. Nếu HTML quá dài → dùng heuristic cắt giữ phần product-related
        #    (tìm vùng chứa price patterns, heading lớn, image gallery...)
        focused_html = self.focus_product_area(cleaned_html)

        # 4. Gửi cho LLM extract thông tin
        prompt = f"""
        Analyze this HTML from an e-commerce product page.
        Extract the following information as JSON:
        - product_name: tên sản phẩm
        - price: giá hiện tại (số, VND)
        - original_price: giá gốc trước giảm (nếu có)
        - brand: thương hiệu
        - specs: dict thông số kỹ thuật (key-value pairs)
        - image_urls: list URLs ảnh sản phẩm
        - seller_name: tên shop/website bán
        - promotion: chương trình khuyến mãi (nếu có)

        Only return valid JSON.

        HTML content:
        {focused_html[:15000]}  # Limit token usage
        """

        result = await self.llm_client.chat(
            system="You are a product data extraction expert.",
            user=prompt,
            response_format="json"
        )

        return ScrapedProductData(**result)

    async def auto_generate_config(self, url: str, extracted_data: ScrapedProductData):
        """
        Sau khi AI extract thành công, tự động sinh CSS selectors
        để lần sau dùng Tier 2 (nhanh hơn + rẻ hơn, không cần gọi LLM).
        """
        html = await self.playwright_render(url)
        prompt = f"""
        Given this HTML and the extracted product data below,
        generate CSS selectors that can reliably extract each field.
        Return as JSON: {{ "product_name": "css_selector", "price": "css_selector", ... }}

        Product data: {extracted_data.model_dump_json()}
        HTML: {html[:15000]}
        """
        selectors = await self.llm_client.chat(system="...", user=prompt, response_format="json")

        # Lưu vào bảng scraper_configs với status = "AI_GENERATED"
        # Admin review → approve → promote thành Tier 2
        return selectors
```

**Luồng xử lý URL tổng quát**:
```python
class ScraperOrchestrator:
    """Điều phối chọn đúng tier scraper cho từng URL."""

    async def scrape(self, url: str) -> ScrapeResult:
        domain = extract_domain(url)

        # Tier 1: Check API-based scrapers
        tier1_scraper = self.tier1_registry.get(domain)
        if tier1_scraper:
            return await tier1_scraper.scrape(url, discover_sellers=True)

        # Tier 2: Check config-based scrapers (từ DB)
        config = await self.config_repo.find_by_domain(domain)
        if config and config.is_active:
            scraper = ConfigBasedScraper(config)
            return await scraper.scrape(url)

        # Tier 3: AI generic scraper (mọi website khác)
        ai_scraper = AIGenericScraper(self.llm_client)
        result = await ai_scraper.scrape_product(url)

        # Tự động sinh config để lần sau dùng Tier 2
        suggested_config = await ai_scraper.auto_generate_config(url, result)
        await self.config_repo.save_suggested(domain, suggested_config, status="AI_GENERATED")

        return result
```

##### Trách nhiệm chung của Scraper Service

- **ScraperOrchestrator**: Nhận URL → detect domain → chọn đúng Tier → execute.
- **Multi-seller Discovery** (chỉ Tier 1): Tìm tất cả sellers bán cùng sản phẩm trên sàn TMĐT.
- **Anti-bot**: Proxy pool rotation, user-agent rotation, request delay, Playwright stealth.
- **Job Management**: Track trạng thái mỗi scrape job (PENDING → IN_PROGRESS → COMPLETED/FAILED).
- **Scheduled Re-scraping**: Cron job cập nhật giá mỗi 12h cho products đang track.
- **Config Admin API**: CRUD scraper configs, review AI-generated configs.

**REST API** (FastAPI):
```
POST   /api/v1/scrape/url                 — Trigger scrape 1 URL
POST   /api/v1/scrape/search              — Search + scrape all sellers (Tier 1)
GET    /api/v1/scrape/jobs/{job_id}        — Check job status
GET    /api/v1/scrape/configs              — List all scraper configs (admin)
POST   /api/v1/scrape/configs              — Add new config (admin)
PUT    /api/v1/scrape/configs/{id}         — Update config (admin)
GET    /api/v1/scrape/configs/suggestions  — List AI-generated configs chờ review (admin)
POST   /api/v1/scrape/configs/{id}/approve — Approve AI config → promote to Tier 2 (admin)
POST   /api/v1/scrape/test                — Test scrape 1 URL + preview result (admin debug)
```

**Message Queue**:
- **Consume**: `scrape.request.queue`
- **Publish**: `product.scraped` (raw data), `scrape.failed`

---

#### SERVICE 5: AI Analyzer Service

| Thuộc tính | Giá trị |
|-----------|---------|
| Ngôn ngữ | **Python** |
| Framework | **FastAPI** |
| Port (Dev) | 8084 |
| Database | Redis (semantic cache) |

**Vai trò**: Bộ não AI — biến raw data thành insights.

**Trách nhiệm**:
- **Review Sentiment Analysis**: Classify POSITIVE / NEGATIVE / NEUTRAL.
- **Seeding/Bot Detection**:
  - Rule-based (miễn phí): review quá ngắn + 5 sao, trùng nội dung, reviewer spam.
  - LLM-assisted: phân tích pattern phức tạp hơn.
- **Review Summarization**: LLM tóm tắt → "Top 3 ưu điểm, Top 3 nhược điểm, Nên/Không nên mua".
- **Trust Score Calculation**: Tính điểm uy tín từng seller (weighted formula).
- **Embedding Generation**: Text embedding (OpenAI) + Image embedding (CLIP) cho vector search.
- **Product Matching / Deduplication**: So khớp sản phẩm cross-source → tránh duplicate.
- **Semantic Caching**: Hash prompt → check Redis → call LLM if miss → cache.

**REST API** (FastAPI):
```
POST /api/v1/analysis/reviews        — Phân tích batch reviews
POST /api/v1/analysis/trust-score    — Tính trust score
POST /api/v1/analysis/embeddings     — Tạo embedding (text hoặc image)
POST /api/v1/analysis/match-product  — So khớp sản phẩm
```

**Message Queue**:
- **Consume**: `analysis.request.queue`
- **Publish**: `analysis.completed`

---

#### SERVICE 6: Notification Service

| Thuộc tính | Giá trị |
|-----------|---------|
| Ngôn ngữ | Java |
| Framework | Spring Boot + WebSocket + STOMP |
| Port (Dev) | 8085 |
| Database | Redis (session store) |

**Vai trò**: Real-time notification + email alerts.

**Trách nhiệm**:
- **WebSocket**: Push notification khi phân tích xong, khi giá giảm.
- **Price Alert**: So sánh giá mới với wishlist target_price → notify.
- **Email Digest**: Tổng hợp biến động giá hàng tuần/ngày.

**Consume**: `analysis.completed`, `price.updated` từ RabbitMQ.

---

### 2.3. Tổng hợp Service Map

| # | Service | Lang | Framework | Port | Database | MQ Role |
|---|---------|------|-----------|------|----------|---------|
| 1 | API Gateway | Java | Spring Cloud Gateway | 8080 | — | — |
| 2 | User Service | Java | Spring Boot + Security | 8081 | PostgreSQL (user_db) | — |
| 3 | Catalog Service | Java | Spring Boot + JPA | 8082 | PostgreSQL (catalog_db) + pgvector | Consumer |
| 4 | Scraper Service | Python | FastAPI + Scrapy + Playwright | 8083 | PostgreSQL (scraper_db) | Producer + Consumer |
| 5 | AI Analyzer | Python | FastAPI | 8084 | Redis (cache) | Producer + Consumer |
| 6 | Notification | Java | Spring Boot + WebSocket | 8085 | Redis (sessions) | Consumer |

### 2.4. Luồng xử lý End-to-End

```
1. User paste URL (bất kỳ website nào) vào search bar
2. Frontend → POST /api/v1/scrape/url → Gateway → Scraper Service
3. Scraper Service (ScraperOrchestrator):
   a. Parse URL → extract domain
   b. Chọn Tier:
      - shopee.vn → Tier 1 ShopeeScraper (API + multi-seller discovery)
      - thegioididong.com → Tier 2 ConfigBasedScraper (CSS selectors từ DB)
      - unknown-shop.com → Tier 3 AIGenericScraper (Playwright + LLM)
   c. Execute scrape → lấy product data + seller listing(s) + reviews
   d. Publish "product.scraped" event
   e. Trả về { jobId, status: "ANALYZING" }
4. Catalog Service (consume "product.scraped"):
   a. Gọi AI Analyzer → match product (đã tồn tại chưa?)
   b. Upsert Product + SellerListing(s) + PriceHistory
   c. Publish "analysis.request"
5. AI Analyzer (consume "analysis.request"):
   a. Sentiment + Fake detection + Summarization + Trust Score + Embeddings
   b. Publish "analysis.completed"
6. Catalog Service (consume "analysis.completed"):
   a. Update product + seller_listings với AI results
7. Notification Service (consume "analysis.completed"):
   a. Push WebSocket → "Phân tích hoàn tất!"
8. Frontend nhận WS → fetch product detail → hiển thị
```

---

## 3. Công nghệ sử dụng (Tech Stack)

### 3.1. Backend — Java Services (Gateway, User, Catalog, Notification)

| Công nghệ | Version | Vai trò |
|-----------|---------|---------|
| Java | 21 LTS | Virtual Threads, LTS support |
| Spring Boot | 3.3.x | Core framework |
| Spring Cloud Gateway | 2024.x | API Gateway (reactive) |
| Spring Security | 6.x | JWT + OAuth2 |
| Spring Data JPA | 3.3.x | ORM + Specification |
| OpenFeign | 4.x | Inter-service HTTP client |
| MapStruct | 1.5.x | Entity ↔ DTO mapper |
| Lombok | 1.18.x | Boilerplate reduction |
| Flyway | 10.x | DB migration |
| Spring WebSocket | 6.x | Real-time push |

### 3.2. Backend — Python Services (Scraper, AI Analyzer)

| Công nghệ | Version | Vai trò |
|-----------|---------|---------|
| Python | 3.12+ | Runtime |
| FastAPI | 0.110+ | Web framework |
| Scrapy | 2.11+ | Scraping framework (Tier 1) |
| Playwright | 1.44+ | Headless browser (Tier 2 dynamic + Tier 3) |
| BeautifulSoup4 | 4.12+ | HTML parser (Tier 2 static) |
| Pika | 1.3+ | RabbitMQ client |
| OpenAI SDK | 1.30+ | LLM + Embeddings |
| Anthropic SDK | 0.25+ | LLM fallback |
| Redis (redis-py) | 5.0+ | Semantic cache |
| Pydantic | 2.7+ | Validation + serialization |
| SQLAlchemy | 2.0+ | ORM (scraper_db) |
| Alembic | 1.13+ | DB migration |

### 3.3. Data Layer

| Công nghệ | Vai trò |
|-----------|---------|
| PostgreSQL 16 | Primary DB (user_db, catalog_db, scraper_db) |
| pgvector | Vector search (extension catalog_db) |
| Redis 7 | Cache, rate limit, session, semantic cache |
| Elasticsearch (sau) | Full-text search nâng cao khi scale |

### 3.4. Messaging

| Công nghệ | Vai trò |
|-----------|---------|
| RabbitMQ 3.13 | Message broker (Java: spring-amqp, Python: pika) |

### 3.5. Frontend

| Công nghệ | Vai trò |
|-----------|---------|
| Next.js 14+ | SSR cho SEO, App Router |
| TypeScript | Type safety |
| Tailwind CSS + ShadcnUI | Styling + Components |
| Recharts | Charts (giá, sentiment) |
| TanStack Query | Server state + caching |
| Zustand | Global state |
| Socket.io Client | WebSocket real-time |

### 3.6. DevOps

| Công nghệ | Vai trò |
|-----------|---------|
| Docker + Compose | Containerization |
| GitHub Actions | CI/CD |
| Fly.io / Railway / VPS | Hosting MVP |
| Cloudflare R2 | Object storage |

---

## 4. Cấu trúc dự án & Base/Common Layer

### 4.1. Monorepo Structure

```
pricehawk-ai/
│
├── docker-compose.yml                     ← Infrastructure (PG, Redis, RabbitMQ)
├── docker-compose.services.yml            ← All services
├── scripts/
│   ├── init-databases.sql                 ← CREATE DATABASE user_db, catalog_db, scraper_db
│   └── start-dev.sh
│
├── backend-java/                          ← JAVA SERVICES (Maven multi-module)
│   ├── pom.xml                            ← Parent POM
│   ├── pricehawk-common/                  ← ★ Shared: DTOs, exceptions, constants, events, utils
│   ├── pricehawk-security-starter/        ← ★ Shared: JWT, auth filter, SecurityUtils
│   ├── pricehawk-data-starter/            ← ★ Shared: BaseEntity, BaseRepo, JPA auditing
│   ├── pricehawk-messaging-starter/       ← ★ Shared: RabbitMQ config, EventPublisher
│   ├── pricehawk-gateway/                 ← API Gateway
│   ├── pricehawk-user-service/            ← User Service (auth + profile + wishlist)
│   ├── pricehawk-catalog-service/         ← Catalog Service
│   └── pricehawk-notification-service/    ← Notification Service
│
├── backend-python/                        ← PYTHON SERVICES
│   ├── shared/                            ← ★ Shared: config, models, rabbitmq, utils
│   │   ├── __init__.py
│   │   ├── config.py                      ← Env vars, constants
│   │   ├── rabbitmq_client.py             ← Publisher/Consumer base classes
│   │   ├── models.py                      ← Pydantic event models (match Java DTOs)
│   │   ├── exceptions.py
│   │   └── utils.py
│   │
│   ├── scraper-service/
│   │   ├── Dockerfile
│   │   ├── requirements.txt
│   │   ├── alembic/                       ← DB migrations
│   │   └── app/
│   │       ├── main.py                    ← FastAPI entry
│   │       ├── api/
│   │       │   ├── scrape_routes.py       ← Scrape endpoints
│   │       │   └── config_routes.py       ← Scraper config admin endpoints
│   │       ├── core/
│   │       │   ├── orchestrator.py        ← ★ ScraperOrchestrator (tier routing)
│   │       │   └── base_scraper.py        ← Abstract BaseScraper
│   │       ├── tier1/                     ← API-based scrapers
│   │       │   ├── shopee_scraper.py
│   │       │   ├── lazada_scraper.py
│   │       │   └── tiki_scraper.py
│   │       ├── tier2/                     ← Config-based generic scraper
│   │       │   └── config_scraper.py      ← ★ ConfigBasedScraper (reads config from DB)
│   │       ├── tier3/                     ← AI-powered generic scraper
│   │       │   └── ai_scraper.py          ← ★ AIGenericScraper (Playwright + LLM)
│   │       ├── services/
│   │       │   ├── job_service.py
│   │       │   ├── proxy_service.py
│   │       │   └── config_service.py      ← CRUD scraper configs
│   │       ├── models/                    ← SQLAlchemy models
│   │       ├── consumers/                 ← RabbitMQ consumers
│   │       └── config/
│   │
│   └── ai-service/
│       ├── Dockerfile
│       ├── requirements.txt
│       └── app/
│           ├── main.py
│           ├── api/
│           ├── analyzers/
│           │   ├── sentiment_analyzer.py
│           │   ├── fake_review_detector.py
│           │   ├── review_summarizer.py
│           │   ├── trust_score_calculator.py
│           │   ├── embedding_generator.py
│           │   └── product_matcher.py
│           ├── llm/
│           │   ├── base_client.py
│           │   ├── openai_client.py
│           │   ├── anthropic_client.py
│           │   └── semantic_cache.py
│           ├── consumers/
│           └── config/
│
└── frontend/                              ← NEXT.JS WEB APP
    ├── package.json
    ├── next.config.js
    ├── tailwind.config.ts
    └── src/
        ├── app/                           ← Pages (App Router)
        │   ├── page.tsx                   ← Landing / Search
        │   ├── products/[slug]/page.tsx   ← Product detail
        │   ├── compare/page.tsx           ← Comparison
        │   ├── auth/login/page.tsx
        │   ├── auth/register/page.tsx
        │   └── dashboard/...
        ├── components/
        │   ├── ui/                        ← ShadcnUI
        │   ├── layout/                    ← Header, Footer
        │   ├── search/                    ← SearchBar, URLInput, ImageUpload
        │   ├── product/                   ← ProductCard, SellerTable, SpecsTable
        │   ├── charts/                    ← PriceHistoryChart, SentimentDonut
        │   ├── reviews/                   ← ReviewSummary, FakeAlert, ReviewList
        │   └── common/                    ← Loading, Pagination, Badges
        ├── hooks/                         ← useWebSocket, useProductDetail...
        ├── lib/                           ← API client, auth helpers
        ├── stores/                        ← Zustand stores
        └── types/                         ← TypeScript types
```

### 4.2. Shared Modules — Java

*(Giữ nguyên từ v2 — ApiResponse, PageResponse, GlobalExceptionHandler, MessageQueueConstants, BaseEntity, BaseRepository, JwtTokenProvider, EventPublisher... Tham khảo v2 section 4.2 cho code mẫu chi tiết)*

**Bổ sung Enums cho 3-Tier Scraper**:
```java
public enum Platform {
    // Tier 1: Sàn TMĐT (API-based)
    SHOPEE, LAZADA, TIKI,
    // Tier 2+3: Bất kỳ website nào (domain string)
    OTHER
}

public enum SourceType {
    MARKETPLACE,   // Sàn TMĐT (multi-seller)
    RETAILER,      // Website bán hàng (1 seller = chính website đó)
    UNKNOWN        // AI scraped, chưa xác định rõ
}

public enum ScraperTier {
    API_BASED,     // Tier 1
    CONFIG_BASED,  // Tier 2
    AI_GENERIC     // Tier 3
}
```

**Bổ sung Event DTO**:
```java
@Data @Builder
public class ScrapeResultEvent {
    private String jobId;
    private String domain;             // domain gốc (VD: "thegioididong.com")
    private String platform;           // SHOPEE/LAZADA/TIKI hoặc "OTHER"
    private String sourceType;         // MARKETPLACE/RETAILER/UNKNOWN
    private String scraperTier;        // API_BASED/CONFIG_BASED/AI_GENERIC
    private ScrapedProductData productData;
    private List<ScrapedSellerListing> sellerListings;
}
```

### 4.3. Shared Module — Python

*(Giữ nguyên từ v2 — config.py, models.py, rabbitmq_client.py... Tham khảo v2 section 4.3)*

**Bổ sung models cho Tier 2 config**:
```python
# shared/models.py — bổ sung
class ScraperConfig(BaseModel):
    id: str
    domain: str
    name: str
    selectors: dict  # CSS selectors cho từng field
    type: str        # "static" (HTTP GET) hoặc "dynamic" (Playwright)
    pagination: dict | None = None
    is_active: bool = True
    status: str = "ACTIVE"  # ACTIVE, AI_GENERATED (chờ review), DISABLED
```

### 4.4. Dependency Rules

```
JAVA: common → security-starter, data-starter, messaging-starter → service modules
PYTHON: shared → scraper-service, ai-service
CROSS: Java ↔ Python qua RabbitMQ (JSON) hoặc REST (HTTP JSON)
```

- Shared modules KHÔNG depend vào service modules.
- Service modules KHÔNG depend lẫn nhau.
- Code vào shared khi ≥ 2 service cần.

---

## 5. Database Design

### 5.1. User Service Database (`user_db`)

```sql
-- Users (auth + profile gộp chung)
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Auth fields
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),                -- NULL nếu OAuth2
    auth_provider   VARCHAR(20) DEFAULT 'LOCAL', -- LOCAL, GOOGLE, FACEBOOK
    provider_id     VARCHAR(255),
    -- Profile fields
    full_name       VARCHAR(255),
    avatar_url      VARCHAR(500),
    -- Subscription
    subscription_plan VARCHAR(20) DEFAULT 'FREE',
    daily_search_count INT DEFAULT 0,
    daily_search_reset_at TIMESTAMP,
    -- Preferences
    preferences     JSONB DEFAULT '{}',
    -- Meta
    is_active       BOOLEAN DEFAULT TRUE,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE roles (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE  -- USER, PREMIUM_USER, ADMIN
);

CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE wishlists (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(255) DEFAULT 'My Wishlist',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE wishlist_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wishlist_id  UUID NOT NULL REFERENCES wishlists(id) ON DELETE CASCADE,
    product_id   UUID NOT NULL,
    target_price DECIMAL(15,2),
    created_at   TIMESTAMP DEFAULT NOW(),
    UNIQUE(wishlist_id, product_id)
);

CREATE TABLE search_histories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    query_type  VARCHAR(20) NOT NULL,       -- URL, IMAGE, TEXT
    query_value TEXT NOT NULL,
    result_count INT,
    created_at  TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_search_histories_user ON search_histories(user_id, created_at DESC);
```

### 5.2. Catalog Service Database (`catalog_db`)

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE categories (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name      VARCHAR(255) NOT NULL,
    slug      VARCHAR(255) NOT NULL UNIQUE,
    parent_id UUID REFERENCES categories(id),
    level     INT DEFAULT 0,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(500) NOT NULL,
    slug            VARCHAR(500) NOT NULL UNIQUE,
    brand           VARCHAR(255),
    description     TEXT,
    thumbnail_url   VARCHAR(500),
    category_id     UUID REFERENCES categories(id),
    -- AI fields
    ai_summary      TEXT,
    sentiment_score DECIMAL(3,2),
    total_reviews   INT DEFAULT 0,
    real_review_ratio DECIMAL(3,2),
    -- Best deal
    lowest_price    DECIMAL(15,2),
    lowest_price_seller VARCHAR(255),
    lowest_price_source VARCHAR(255),      -- domain hoặc platform name
    -- Vector
    name_embedding  vector(1536),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_products_embedding ON products
    USING ivfflat (name_embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand ON products(brand);

CREATE TABLE product_specs (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    specs      JSONB NOT NULL
);
CREATE INDEX idx_specs_gin ON product_specs USING gin(specs);

-- ★ Seller Listings — 1 product → nhiều sellers/websites
CREATE TABLE seller_listings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    -- Source
    domain              VARCHAR(255) NOT NULL,       -- "shopee.vn", "thegioididong.com", "random-shop.com"
    source_type         VARCHAR(20) NOT NULL,        -- MARKETPLACE, RETAILER, UNKNOWN
    scraper_tier        VARCHAR(20),                 -- API_BASED, CONFIG_BASED, AI_GENERIC
    -- Seller
    seller_name         VARCHAR(255) NOT NULL,
    seller_id           VARCHAR(255),
    seller_url          VARCHAR(1000),
    is_official_store   BOOLEAN DEFAULT FALSE,
    -- Listing
    external_url        VARCHAR(1000) NOT NULL,
    external_product_id VARCHAR(255),
    current_price       DECIMAL(15,2),
    original_price      DECIMAL(15,2),
    currency            VARCHAR(3) DEFAULT 'VND',
    promotion_info      TEXT,
    -- Trust & Quality
    trust_score         DECIMAL(3,2),
    review_count        INT DEFAULT 0,
    average_rating      DECIMAL(2,1),
    fake_review_ratio   DECIMAL(3,2),
    -- Scraping
    last_scraped_at     TIMESTAMP,
    scrape_status       VARCHAR(20) DEFAULT 'PENDING',
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_listings_product ON seller_listings(product_id);
CREATE INDEX idx_listings_domain ON seller_listings(domain);
CREATE INDEX idx_listings_price ON seller_listings(current_price);
CREATE INDEX idx_listings_trust ON seller_listings(trust_score DESC NULLS LAST);

CREATE TABLE price_history (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_listing_id UUID NOT NULL REFERENCES seller_listings(id) ON DELETE CASCADE,
    price             DECIMAL(15,2) NOT NULL,
    recorded_at       TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_price_history ON price_history(seller_listing_id, recorded_at DESC);

CREATE TABLE reviews (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_listing_id UUID NOT NULL REFERENCES seller_listings(id) ON DELETE CASCADE,
    reviewer_name     VARCHAR(255),
    rating            SMALLINT,
    content           TEXT,
    review_date       TIMESTAMP,
    sentiment         VARCHAR(10),
    is_likely_fake    BOOLEAN DEFAULT FALSE,
    fake_reason       VARCHAR(100),
    created_at        TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_reviews_listing ON reviews(seller_listing_id);
```

### 5.3. Scraper Service Database (`scraper_db`)

```sql
-- ★ Scraper Configs (Tier 2 — dynamic, admin quản lý)
CREATE TABLE scraper_configs (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain     VARCHAR(255) NOT NULL UNIQUE,      -- "thegioididong.com"
    name       VARCHAR(255) NOT NULL,              -- "Thế Giới Di Động"
    config     JSONB NOT NULL,                     -- { selectors: {...}, type: "static"/"dynamic" }
    status     VARCHAR(20) DEFAULT 'ACTIVE',       -- ACTIVE, AI_GENERATED, DISABLED
    is_active  BOOLEAN DEFAULT TRUE,
    success_count   INT DEFAULT 0,                 -- Tracking config reliability
    fail_count      INT DEFAULT 0,
    last_used_at    TIMESTAMP,
    created_by      VARCHAR(20) DEFAULT 'ADMIN',   -- ADMIN hoặc AI
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Scrape Jobs
CREATE TABLE scrape_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    url             VARCHAR(1000) NOT NULL,
    domain          VARCHAR(255) NOT NULL,
    scraper_tier    VARCHAR(20),                    -- API_BASED, CONFIG_BASED, AI_GENERIC
    status          VARCHAR(20) DEFAULT 'PENDING',
    discover_sellers BOOLEAN DEFAULT FALSE,
    sellers_found   INT DEFAULT 0,
    retry_count     INT DEFAULT 0,
    max_retries     INT DEFAULT 3,
    error_message   TEXT,
    raw_data        JSONB,
    requested_by    UUID,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_jobs_status ON scrape_jobs(status);

-- Proxy Pool
CREATE TABLE proxies (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host          VARCHAR(255) NOT NULL,
    port          INT NOT NULL,
    protocol      VARCHAR(10) DEFAULT 'HTTP',
    is_active     BOOLEAN DEFAULT TRUE,
    success_count INT DEFAULT 0,
    fail_count    INT DEFAULT 0,
    last_used_at  TIMESTAMP
);
```

---

## 6. Chi tiết các Phase phát triển

### PHASE 1: Project Foundation & Infrastructure

> **Mục tiêu**: Dựng bộ khung. Sau phase này, thêm service mới chỉ cần inherit.

**Task 1.1**: Parent POM + module structure. `mvn clean install` pass.
**Task 1.2**: `pricehawk-common` — ApiResponse, PageResponse, GlobalExceptionHandler, all Exceptions, Constants, Enums (Platform, SourceType, ScraperTier...), Event DTOs, Utils. Unit tests.
**Task 1.3**: `pricehawk-security-starter` — JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig, SecurityUtils.
**Task 1.4**: `pricehawk-data-starter` — BaseEntity (UUID + auditing), BaseRepository, JpaAuditingConfig, BaseSpecification.
**Task 1.5**: `pricehawk-messaging-starter` — RabbitMQConfig, EventPublisher, AbstractEventConsumer.
**Task 1.6**: Python `shared/` — config.py, models.py (match Java event DTOs), rabbitmq_client.py, exceptions.py, utils.py.
**Task 1.7**: `docker-compose.yml` — PostgreSQL (pgvector, init 3 DBs), Redis, RabbitMQ.
**Task 1.8**: API Gateway — routing (6 services), rate limiter (Redis), JWT filter, CORS.

---

### PHASE 2: User Service (Auth + Profile + Wishlist)

> **Mục tiêu**: Register, login, JWT, profile, wishlist. 1 service xử lý hết.

**Task 2.1**: Service setup, entities (User, Role, Wishlist, WishlistItem, SearchHistory), Flyway migrations.
**Task 2.2**: Auth — POST `/auth/register`, POST `/auth/login`, POST `/auth/refresh-token`. BCrypt, JWT, validation.
**Task 2.3**: OAuth2 Google — POST `/auth/oauth2/google`. Verify idToken, auto-create/link.
**Task 2.4**: Profile — GET/PUT `/users/me`. Wishlist CRUD. Search history.

---

### PHASE 3: Catalog Service

> **Mục tiêu**: Product + Seller Listing data model. Search, filter, price history.

**Task 3.1**: Entities (Product, ProductSpec, SellerListing, PriceHistory, Category, Review), Flyway + pgvector.
**Task 3.2**: Category tree API + seed data (Tech categories).
**Task 3.3**: Product APIs — list (dynamic filter), detail (+ all seller listings sorted by price), price history, reviews, compare.
**Task 3.4**: RabbitMQ consumers — process `product.scraped`, process `analysis.completed`.
**Task 3.5**: Search APIs — text search (PG FTS + Specification), URL submit (→ trigger scrape), image search (→ vector query).

---

### PHASE 4: Scraper Service (Python) — ★ 3-Tier Architecture

> **Mục tiêu**: Intelligent scraping — từ API-based cho sàn lớn đến AI-powered cho mọi website.

**Task 4.1**: FastAPI setup, Alembic (scraper_db), RabbitMQ consumer, basic REST endpoints.

**Task 4.2**: ScraperOrchestrator + BaseScraper interface.
- Orchestrator: URL → detect domain → chọn Tier → execute → publish result.

**Task 4.3**: Tier 1 — ShopeeScraper (MVP priority).
- Parse URL → Shopee API → product + multi-seller discovery + reviews.
- Proxy rotation + rate limiting.

**Task 4.4**: Tier 2 — ConfigBasedScraper engine.
- Load config từ DB by domain.
- Static mode (HTTP GET + BeautifulSoup) + Dynamic mode (Playwright + BeautifulSoup).
- Generic extraction logic dùng CSS selectors từ config.
- Admin CRUD API cho configs.

**Task 4.5**: Seed Tier 2 configs — tạo initial configs cho: TGDĐ, FPT Shop, CellphoneS, Phong Vũ, GearVN.
- Test từng config, verify extraction chính xác.

**Task 4.6**: Tier 1 — Lazada + Tiki scrapers.

**Task 4.7**: Tier 3 — AIGenericScraper.
- Playwright render → clean HTML → LLM extract product data.
- Auto-generate config → save as AI_GENERATED → admin review → approve to Tier 2.

**Task 4.8**: Scheduled re-scraping (cron mỗi 12h) + job management + retry logic.

---

### PHASE 5: AI Analyzer Service (Python)

> **Mục tiêu**: Biến raw data thành insights.

**Task 5.1**: FastAPI setup, LLM client (OpenAI primary + Anthropic fallback), semantic cache (Redis).
**Task 5.2**: Review pipeline — fake detection (rule-based → LLM), sentiment analysis, summarization.
**Task 5.3**: Trust Score Calculator — weighted formula, retailer bonus.
**Task 5.4**: Embedding generation — text (OpenAI) + image (CLIP). Vector search.
**Task 5.5**: Product Matching — TF-IDF + specs matching. Cross-source deduplication.

---

### PHASE 6: Notification Service

**Task 6.1**: WebSocket (STOMP) + JWT auth handshake + Redis sessions.
**Task 6.2**: Price Alert — consume `price.updated`, check wishlist targets, push/email.

---

### PHASE 7: Frontend (Next.js)

> **Mục tiêu**: Web app hoàn chỉnh, responsive, SEO-friendly.

**Task 7.1**: Next.js project setup, Tailwind + ShadcnUI, layout (Header, Footer).
**Task 7.2**: Landing page — Hero search bar (3 tabs: URL | Text | Image), trending, how it works.
**Task 7.3**: Auth pages — Login, Register, Google OAuth.
**Task 7.4**: Product Detail page — ★ Core page:
- Product header + AI summary badge.
- Seller Comparison Table (tất cả sellers, sort/filter by price/trust/platform).
- Price History Chart (Recharts, per seller).
- AI Review Analysis (summary, sentiment donut, fake alert).
- Specs table + Similar products.
**Task 7.5**: Compare page — side-by-side 2–3 products.
**Task 7.6**: Dashboard — Wishlists, History, Settings.
**Task 7.7**: WebSocket integration — real-time notifications.
**Task 7.8**: Responsive + PWA + SEO (meta tags, JSON-LD, sitemap).

---

## 7. Frontend — Web Application

### 7.1. Sitemap

```
/                           ← Landing + Search (hero section)
/products                   ← Browse by category
/products/[slug]            ← ★ Product detail (seller table, price chart, AI review)
/compare?ids=id1,id2        ← Side-by-side comparison
/auth/login                 ← Login
/auth/register              ← Register
/dashboard                  ← Overview
/dashboard/wishlists        ← Wishlist management
/dashboard/history          ← Search history
/dashboard/settings         ← Profile
/admin/scraper-configs      ← ★ Admin: manage scraper configs (Tier 2)
```

### 7.2. Product Detail Page — Layout chi tiết

**Section 1: Product Header**
- Tên, brand, category breadcrumb, ảnh gallery.
- AI badge: "⭐ 4.2/5 — 78% review thật — Nên mua nếu cần hiệu năng cao".
- Giá thấp nhất: "Từ 28.490.000đ tại CellphoneS".

**Section 2: ★ Seller Comparison Table**
```
| # | Nơi bán          | Nguồn   | Giá          | KM              | Uy tín    | Hành động     |
|---|-------------------|---------|--------------|-----------------|-----------|---------------|
| 1 | CellphoneS        | Website | 28.490.000đ  | Tặng case       | ⭐ 9.2/10 | [Đến nơi bán] |
| 2 | FPT Shop          | Website | 28.790.000đ  | Tặng AirPods    | ⭐ 9.0/10 | [Đến nơi bán] |
| 3 | Minh Tuấn Mobile  | Shopee  | 28.500.000đ  | Free ship        | ⭐ 8.5/10 | [Đến nơi bán] |
| 4 | TGDĐ              | Website | 28.990.000đ  | Trả góp 0%     | ⭐ 9.5/10 | [Đến nơi bán] |
| 5 | random-shop.com   | AI ✨   | 27.990.000đ  | —               | ⚠️ N/A    | [Đến nơi bán] |
```
- Badge "AI ✨" cho listing đến từ Tier 3 (AI scraped).
- Sort: giá, uy tín, rating. Filter: nguồn, chỉ official, price range.

**Section 3: Price History Chart**
- Recharts line chart, mỗi seller = 1 đường.
- 30d / 90d / 6m toggle.
- Cảnh báo sale ảo.

**Section 4: AI Review Analysis**
- Tóm tắt ưu/nhược (ai_summary).
- Sentiment donut chart.
- "Bóc phốt": % review giả, examples.
- Top reviews thật.

**Section 5: Specs + Similar**
- Specs table (từ JSONB). Nút "So sánh".
- Similar products carousel.

### 7.3. Admin — Scraper Config Management

```
/admin/scraper-configs
```
- Table list tất cả configs (domain, name, status, success/fail count).
- Filter: status (ACTIVE / AI_GENERATED / DISABLED).
- **AI_GENERATED configs** highlight → admin click review → xem selectors → test thử → approve/reject.
- Add new config form: nhập domain + CSS selectors + type (static/dynamic).
- Edit existing config.
- Test button: paste URL → dry-run scrape → preview kết quả.

### 7.4. Components

```
src/components/
├── ui/                     ← ShadcnUI (Button, Input, Table, Dialog...)
├── layout/                 ← Header, Footer, Sidebar, AdminLayout
├── search/                 ← SearchBar, URLInput, TextSearch, ImageUpload
├── product/                ← ProductCard, ProductHeader, SellerTable, SpecsTable, CompareView
├── charts/                 ← PriceHistoryChart, SentimentDonut
├── reviews/                ← ReviewSummary, FakeAlert, ReviewList, ReviewCard
├── notifications/          ← NotificationBell, PriceAlertBanner
├── admin/                  ← ScraperConfigTable, ConfigEditor, ConfigTester
└── common/                 ← Loading, Pagination, PlatformBadge, TrustBadge, TierBadge
```

### 7.5. API Client + Types

```typescript
// src/types/index.ts
interface Product {
  id: string;
  name: string;
  slug: string;
  brand: string;
  categoryId: string;
  aiSummary: string | null;
  sentimentScore: number | null;
  lowestPrice: number | null;
  lowestPriceSeller: string | null;
  lowestPriceSource: string | null;
}

interface SellerListing {
  id: string;
  productId: string;
  domain: string;              // "shopee.vn", "thegioididong.com", "random-shop.com"
  sourceType: 'MARKETPLACE' | 'RETAILER' | 'UNKNOWN';
  scraperTier: 'API_BASED' | 'CONFIG_BASED' | 'AI_GENERIC';
  sellerName: string;
  isOfficialStore: boolean;
  externalUrl: string;
  currentPrice: number;
  originalPrice: number | null;
  promotionInfo: string | null;
  trustScore: number | null;
  reviewCount: number;
  averageRating: number | null;
  fakeReviewRatio: number | null;
}

interface ScraperConfig {
  id: string;
  domain: string;
  name: string;
  config: { selectors: Record<string, string>; type: 'static' | 'dynamic' };
  status: 'ACTIVE' | 'AI_GENERATED' | 'DISABLED';
  successCount: number;
  failCount: number;
  createdBy: 'ADMIN' | 'AI';
}
```

### 7.6. SEO

- SSR cho product detail pages.
- Dynamic meta: title, description, og:image per product.
- JSON-LD: Product, AggregateOffer (nhiều sellers), AggregateRating.
- Auto sitemap từ product slugs.
- Clean URLs: `/products/iphone-16-pro-max-256gb`.

---

## 8. Coding Conventions & Rules

### 8.1. Naming

| Thành phần | Convention | Ví dụ |
|-----------|-----------|-------|
| Java Class | PascalCase | `SellerListingService` |
| Python Module | snake_case | `shopee_scraper.py` |
| REST path | kebab-case, plural | `/api/v1/seller-listings` |
| DB table | snake_case, plural | `seller_listings` |
| TS Component | PascalCase | `SellerTable.tsx` |
| TS hook | camelCase, "use" prefix | `useProductDetail` |

### 8.2. Architecture Rules

**Java**:
- Controller: validate → service → ApiResponse. NO logic.
- Entity never leaks past Service layer. Always DTO.
- Each service owns its DB. Cross-service = OpenFeign.
- All responses wrap in `ApiResponse<T>` or `PageResponse<T>`.

**Python**:
- FastAPI route: validate (Pydantic) → service → return. NO logic.
- All I/O uses Pydantic models.
- Scraper classes extend `BaseScraper`.
- LLM calls: always cache-first, always timeout + retry.

**Frontend**:
- Components < 200 lines.
- Data fetching only via TanStack Query hooks.
- Global state → Zustand. Server state → TanStack Query.
- All API types in `src/types/`.

---

## 9. Deployment & Infrastructure

### 9.1. Docker Compose (Local Dev)

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_USER: pricehawk
      POSTGRES_PASSWORD: pricehawk_secret
    ports: ["5432:5432"]
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-databases.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: pricehawk
      RABBITMQ_DEFAULT_PASS: pricehawk_secret
    ports: ["5672:5672", "15672:15672"]

volumes:
  postgres_data:
```

### 9.2. CI/CD

```
Push → Test (Java + Python + Frontend) → Build Docker → Push Registry → Deploy staging → Production
```

### 9.3. Monitoring

- Logging: SLF4J (Java), structlog (Python). Correlation ID.
- Metrics: Actuator + Micrometer → Prometheus → Grafana.
- Alerts: error rate, latency, scraper failures, LLM cost.

---

## Phụ lục: Build Priority

| # | Phase | Lý do |
|---|-------|-------|
| 1 | Phase 1 (Foundation) | Khung xương sống |
| 2 | Phase 3 (Catalog) | Core data model |
| 3 | Phase 4 (Scraper) | Không data = không hiển thị |
| 4 | Phase 5 (AI Analyzer) | USP hệ thống |
| 5 | Phase 2 (User) | Song song Phase 3–4 |
| 6 | Phase 6 (Notification) | Nice-to-have |
| 7 | Phase 7 (Frontend) | Song song từ Phase 3 |
