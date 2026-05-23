# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PriceHawk AI — Multi-language microservices shopping assistant. Java (Gateway + 3 services) + Python (2 services) + Next.js frontend.

## Service Ports & Routing

| Service | Lang | Port | DB |
|---|---|---|---|
| API Gateway | Java | 8080 | — |
| User Service | Java | 8081 | PostgreSQL `user_db` |
| Catalog Service | Java | 8082 | PostgreSQL `catalog_db` + pgvector |
| Scraper Service | Python | 8083 | PostgreSQL `scraper_db` |
| AI Analyzer | Python | 8084 | Redis (cache only) |
| Notification | Java | 8085 | Redis (sessions) |

## Infrastructure Commands

```bash
# Start infrastructure (Postgres, Redis, RabbitMQ)
docker-compose up -d

# Check services
docker-compose ps

# Reset databases
docker-compose down -v && docker-compose up -d
```

Credentials: `pricehawk` / `pricehawk_secret`
RabbitMQ Management UI: http://localhost:15672

## Cross-Service Contracts

### RabbitMQ Events (NEVER change exchange/queue names without updating all consumers)

| Event | Exchange | Routing Key | Producer → Consumer |
|---|---|---|---|
| Scrape request | `pricehawk.scrape` | `scrape.request` | Catalog → Scraper |
| Product scraped | `pricehawk.scrape` | `product.scraped` | Scraper → Catalog |
| Analysis request | `pricehawk.analysis` | `analysis.request` | Catalog → AI |
| Analysis completed | `pricehawk.analysis` | `analysis.completed` | AI → Catalog, Notification |
| Price updated | `pricehawk.price` | `price.updated` | Catalog → Notification |

Event DTOs live in `backend-java/pricehawk-common/src/main/java/com/pricehawk/common/event/`.
Python mirrors are in `backend-python/shared/models.py`.

### Gateway Routes
All requests flow through port 8080:
- `/api/v1/auth/**`, `/api/v1/users/**` → User Service
- `/api/v1/products/**`, `/api/v1/search/**` → Catalog Service  
- `/api/v1/scrape/**` → Scraper Service
- `/api/v1/analysis/**` → AI Service
- `/ws/**` → Notification Service

## Dependency Rules

```
JAVA:   common → security-starter, data-starter, messaging-starter → service modules
PYTHON: shared → scraper-service, ai-service
CROSS:  Java ↔ Python via RabbitMQ (JSON) or REST (HTTP JSON)
```
- Shared modules NEVER depend on service modules.
- Services NEVER directly call each other's DB.
- Add code to shared only when ≥2 services need it.

## Scraper 3-Tier Architecture

Tier 1 (API-based): Shopee, Lazada, Tiki — hardcoded, fastest
Tier 2 (Config-based): CSS selectors stored in `scraper_db.scraper_configs` — admin-managed, no redeploy
Tier 3 (AI Generic): Playwright + LLM — any unknown website, auto-generates Tier 2 config

The `ScraperOrchestrator` in `backend-python/scraper-service/app/core/orchestrator.py` routes Tier 1→2→3.

## Production-Grade Standards — KHÔNG ĐƯỢC HẠ THẤP

> PriceHawk được xây dựng để chạy production thực tế. Mọi dòng code phải đạt chuẩn của các hệ thống lớn đang hoạt động, **không phải demo, không phải bài tập sinh viên.**

### Security — Không khoan nhượng

- **Authentication**: Luôn extract userId từ JWT đã verify (`@AuthenticationPrincipal`). **Tuyệt đối không trust plain header do client gửi** (ví dụ `X-User-Id`) trừ khi header đó được inject bởi API Gateway sau khi đã validate JWT.
- **Authorization**: Mọi endpoint cần auth phải kiểm tra ownership — user A không thể đọc/sửa data của user B dù có token hợp lệ.
- **Input validation**: Validate tất cả input tại boundary (controller/route), reject sớm trước khi vào service. Không trust bất kỳ dữ liệu nào từ client.
- **SQL injection**: Dùng parameterized queries / JPA / SQLAlchemy — không bao giờ nối chuỗi SQL thủ công.
- **Secrets**: Không hardcode secret/key trong code. Luôn đọc từ env var với default chỉ cho local dev.
- **Sensitive data**: Không log password, token, PII. Response không trả về `passwordHash` hay internal fields.
- **External calls**: Luôn set timeout. Validate response trước khi tin dùng (xem `OAuth2Service` — verify Google token trước khi tạo user).

### API Design

- **Idempotency**: POST tạo resource phải kiểm tra duplicate trước khi insert (không để double-insert).
- **HTTP status codes đúng**: 201 Created cho POST tạo mới, 204 No Content cho DELETE, 409 Conflict cho duplicate, 422 cho validation error business logic.
- **Pagination**: Mọi list endpoint phải hỗ trợ phân trang (`page`, `size`). Không bao giờ trả về unbounded list.
- **Versioning**: Tất cả API endpoints đi qua `/api/v1/`. Khi breaking change: tạo `/api/v2/`, không xóa `/api/v1/`.
- **Error response nhất quán**: Luôn wrap trong `ApiResponse` với `success`, `message`, `errorCode`. Client phải biết lỗi gì xảy ra.

### Data Layer

- **DB migrations**: Chỉ thay đổi schema qua Flyway (Java) hoặc Alembic (Python). **Không bao giờ chạy ALTER TABLE thủ công trên bất kỳ môi trường nào.**
- **Transactions**: Business logic liên quan đến nhiều bảng phải trong cùng 1 transaction (`@Transactional`). Partial update là bug.
- **N+1 queries**: Không để vòng lặp gọi DB. Dùng `JOIN FETCH`, batch query, hoặc đổi fetch strategy.
- **Index**: Mọi cột được dùng trong WHERE/ORDER BY phải có index trong migration.
- **Constraints**: Enforce data integrity tại DB level (NOT NULL, UNIQUE, FK) — không chỉ ở application level.

### Resilience

- **Timeout**: Mọi HTTP call ra ngoài (external API, inter-service) phải có connect timeout + read timeout.
- **Retry**: External calls có thể fail tạm thời — implement retry với exponential backoff cho critical paths.
- **Graceful degradation**: Nếu service phụ (AI, scraper) chậm/lỗi, core flow phải vẫn hoạt động được.
- **Không để exception trần**: Mọi exception đều phải được catch ở đúng layer, log đủ context, và trả về response có nghĩa.

### Observability

- **Structured logging**: Log đủ context — userId, requestId, action, duration. Không log chỉ `"Error occurred"`.
- **Log levels đúng**: DEBUG cho flow trace, INFO cho business event quan trọng, WARN cho recoverable error, ERROR cho unrecoverable.
- **Health checks**: Mọi service phải expose `/actuator/health` (Java) hoặc `/health` (Python) kiểm tra DB + dependencies.
- **Correlation ID**: Mọi request phải được track bằng 1 ID xuyên suốt các service.

### Code Quality

- **Không để dead code**: Xóa hẳn code không dùng, không comment out rồi để đó.
- **Không để magic number/string**: Đưa vào constant hoặc config.
- **Fail fast**: Validate điều kiện tiên quyết ngay đầu method — không để logic sai chạy sâu vào rồi mới lỗi.
- **Method size**: Nếu 1 method > 30 dòng, xem xét tách ra — mỗi method làm đúng 1 việc.

---

## Global Coding Rules

- All HTTP responses wrap in `ApiResponse<T>` (Java) or equivalent Pydantic schema (Python).
- Never put business logic in controllers/routes. Controller → Service → Repository only.
- Entity objects never leave the Service layer. Always convert to DTO before returning.
- Each service owns its own DB. Cross-service reads via REST (OpenFeign in Java) or HTTP.
- All environment variables have defaults for local dev. See `.env.example`.

## Mandatory Testing Rule — KHÔNG được bỏ qua

**Mọi task trong PLAN.md chỉ được đánh dấu ✅ khi đã test xong.** Không có ngoại lệ.

### Test theo loại thay đổi

| Loại thay đổi | Test bắt buộc |
|---|---|
| Java API endpoint mới | Unit test Service layer + manual `curl`/Postman verify response shape |
| Java Consumer (RabbitMQ) | Publish message thủ công → verify DB được update đúng |
| Python route mới | `pytest` test case + manual curl verify |
| Python scraper | Chạy thật với 1 URL thật → verify fields không null/rỗng |
| Frontend component | Render với real API data, kiểm tra edge cases (null, loading, error state) |
| DB migration | Chạy `flyway migrate` / `alembic upgrade head` trên DB sạch — không lỗi |
| Shared DTO/event thay đổi | Verify cả Java publisher lẫn Python consumer còn parse được |

### Quy trình trước khi báo hoàn thành

```
1. Chạy test của task vừa làm → PASS
2. Chạy lại test của các task liên quan → không REGRESSION
3. Đánh dấu ✅ trong PLAN.md
```

### Java — Test pattern
```java
// Service layer: mock repository, test business logic
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @InjectMocks AuthService authService;

    @Test
    void register_duplicateEmail_throwsConflict() { ... }
}

// Integration: dùng @SpringBootTest + Testcontainers PostgreSQL
```

### Python — Test pattern
```python
# pytest + httpx AsyncClient
async def test_scrape_url_returns_job_id(client: AsyncClient):
    resp = await client.post("/api/v1/scrape/url", json={"url": "https://shopee.vn/..."})
    assert resp.status_code == 202
    assert resp.json()["job_id"] is not None
```

### Frontend — Checklist thủ công
- [ ] Component render không crash với `null` data
- [ ] Loading state hiển thị đúng
- [ ] Error state hiển thị đúng
- [ ] Responsive trên mobile (375px)

---

## When Adding a New Feature

1. Check if the change affects shared contracts (events, DTOs) — update `pricehawk-common` and `backend-python/shared/models.py` together.
2. If adding a new scraper website (Tier 2), insert into `scraper_configs` table via SQL seed — no code change.
3. Database changes: always via Flyway migration (Java) or Alembic migration (Python). Never modify schema manually.
4. New API endpoints must follow path convention `/api/v1/{resource}/{action}`.
5. Cập nhật `PLAN.md` — đánh dấu ✅ sau khi test xong.
