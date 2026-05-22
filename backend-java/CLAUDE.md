# Java Backend — CLAUDE.md

## Module Structure

Maven multi-module. Build order (enforced by Maven):
1. `pricehawk-common` — DTOs, exceptions, enums, event models, constants
2. `pricehawk-security-starter` — JWT provider, auth filter (auto-configured)
3. `pricehawk-data-starter` — BaseEntity (UUID + JPA auditing), BaseRepository (auto-configured)
4. `pricehawk-messaging-starter` — RabbitMQ config, queues, EventPublisher (auto-configured)
5. Service modules: `pricehawk-gateway`, `pricehawk-user-service`, `pricehawk-catalog-service`, `pricehawk-notification-service`

## Build Commands

```bash
cd backend-java

# Build all
mvn clean install -DskipTests

# Build single module
mvn clean install -pl pricehawk-user-service -am -DskipTests

# Run tests
mvn test -pl pricehawk-user-service

# Run a service
mvn spring-boot:run -pl pricehawk-user-service
```

## Architecture Pattern (ALL services must follow)

```
Controller → Service → Repository
     ↓           ↓
  @Valid     DTO ↔ Entity via MapStruct
```

- Controller: validates input (`@Valid`), calls service, wraps in `ApiResponse.ok()`. Zero logic.
- Service: business logic, transaction boundaries (`@Transactional`). Never returns Entity.
- Repository: extends `BaseRepository<Entity>`. Named queries or `JpaSpecificationExecutor` for dynamic filters.
- MapStruct mapper: `@Mapper(componentModel = "spring")` — auto-wired.

## Required Patterns

**Response wrapping — always:**
```java
// Controller method signature:
public ResponseEntity<ApiResponse<ProductDTO>> getProduct(@PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.ok(productService.findById(id)));
}
```

**Exception throwing — use BusinessException statics:**
```java
throw BusinessException.notFound("Product");
throw BusinessException.conflict("Email already registered");
```

**Entity base class — all entities extend:**
```java
@Entity
public class Product extends BaseEntity { ... }
// Gets: UUID id, Instant createdAt, Instant updatedAt — auto-managed
```

**Flyway migrations:** `src/main/resources/db/migration/V{n}__{description}.sql`
Never alter existing migration files. Add new ones for changes.

**Publishing events:**
```java
eventPublisher.publish(
    MessageQueueConstants.SCRAPE_EXCHANGE,
    MessageQueueConstants.SCRAPE_REQUEST_KEY,
    scrapeRequestEvent
);
```

## JWT Flow (Gateway handles it)

- Gateway validates JWT, injects `X-User-Id` and `X-User-Roles` headers.
- Downstream services trust these headers — they do NOT re-validate JWT.
- Only User Service needs `pricehawk-security-starter` for generating tokens.
- Other Java services just read `X-User-Id` header.

## application.yml Pattern

All services follow identical env var naming:
- DB: `${POSTGRES_HOST:localhost}`, `${POSTGRES_PORT:5432}`, `${POSTGRES_USER:pricehawk}`, `${POSTGRES_PASSWORD:pricehawk_secret}`
- Redis: `${REDIS_HOST:localhost}`, `${REDIS_PORT:6379}`
- RabbitMQ: `${RABBITMQ_HOST:localhost}`, `${RABBITMQ_PORT:5672}`, `${RABBITMQ_USER:pricehawk}`, `${RABBITMQ_PASSWORD:pricehawk_secret}`

## Page Responses

```java
// Service returns Page<DTO>:
Page<ProductDTO> page = productRepository.findAll(spec, pageable)
    .map(productMapper::toDTO);

// Controller wraps:
return ResponseEntity.ok(ApiResponse.ok(PageResponse.<ProductDTO>builder()
    .content(page.getContent())
    .page(page.getNumber())
    .size(page.getSize())
    .totalElements(page.getTotalElements())
    .totalPages(page.getTotalPages())
    .last(page.isLast())
    .build()));
```
