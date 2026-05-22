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
