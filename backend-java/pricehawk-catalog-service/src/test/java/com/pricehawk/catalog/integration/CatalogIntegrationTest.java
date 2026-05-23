package com.pricehawk.catalog.integration;

import com.pricehawk.catalog.domain.entity.Category;
import com.pricehawk.catalog.domain.entity.Product;
import com.pricehawk.catalog.domain.entity.SellerListing;
import com.pricehawk.catalog.domain.enums.ScrapeStatus;
import com.pricehawk.catalog.dto.request.CreateCategoryRequest;
import com.pricehawk.catalog.dto.request.CreateProductRequest;
import com.pricehawk.catalog.dto.response.CategoryDTO;
import com.pricehawk.catalog.dto.response.ProductDTO;
import com.pricehawk.catalog.repository.CategoryRepository;
import com.pricehawk.catalog.repository.ProductRepository;
import com.pricehawk.catalog.repository.SellerListingRepository;
import com.pricehawk.catalog.service.CategoryService;
import com.pricehawk.catalog.service.ProductService;
import com.pricehawk.messaging.publisher.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests: Spring context + real PostgreSQL (Testcontainers).
 * Validates Flyway migrations run cleanly + JPA mappings are correct.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Transactional  // Each test rolls back — no cross-test contamination
class CatalogIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
    )
    .withDatabaseName("catalog_db")
    .withUsername("pricehawk")
    .withPassword("pricehawk_secret");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // Mock EventPublisher so RabbitMQ connection is never attempted in tests
    @MockBean EventPublisher eventPublisher;

    @Autowired CategoryService categoryService;
    @Autowired ProductService productService;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired SellerListingRepository sellerListingRepository;

    @Test
    void flyway_migrationsRunCleanly_schemaIsValid() {
        // If we got here, all 3 Flyway migrations ran without error
        // V1: init schema, V2: unique index + FTS, V3: extended columns
        // The fact that JPA context started means entity mapping matches the schema
        assertThat(categoryRepository).isNotNull();
        assertThat(productRepository).isNotNull();
    }

    @Test
    void createCategory_persistsAndRetrievesBySlug() {
        CategoryDTO dto = categoryService.create(
            new CreateCategoryRequest("Điện thoại", "dien-thoai-test", null, 1));

        assertThat(dto.id()).isNotNull();
        assertThat(dto.slug()).isEqualTo("dien-thoai-test");
        assertThat(dto.level()).isEqualTo(0);

        CategoryDTO fetched = categoryService.getBySlug("dien-thoai-test");
        assertThat(fetched.name()).isEqualTo("Điện thoại");
    }

    @Test
    void createCategory_withParent_setsCorrectLevel() {
        CategoryDTO parent = categoryService.create(
            new CreateCategoryRequest("Electronics", "electronics-it", null, 0));

        CategoryDTO child = categoryService.create(
            new CreateCategoryRequest("Laptops", "laptops-it", parent.id(), 0));

        assertThat(child.level()).isEqualTo(1);
        assertThat(child.parentId()).isEqualTo(parent.id());
    }

    @Test
    void getRootCategories_returnsOnlyRootLevel() {
        categoryService.create(new CreateCategoryRequest("Root A", "root-a-it", null, 1));
        categoryService.create(new CreateCategoryRequest("Root B", "root-b-it", null, 2));

        List<CategoryDTO> roots = categoryService.getRootCategories();

        // Seeded categories (from V1) + our 2 new ones
        assertThat(roots).hasSizeGreaterThanOrEqualTo(2);
        assertThat(roots).allMatch(c -> c.level() == 0);
    }

    @Test
    void createProduct_persistsAndRetrievesById() {
        ProductDTO dto = productService.create(
            new CreateProductRequest("MacBook Pro 14", "macbook-pro-14-it", "Apple",
                "Laptop mỏng nhẹ", null, null));

        assertThat(dto.id()).isNotNull();
        assertThat(dto.name()).isEqualTo("MacBook Pro 14");
        assertThat(dto.slug()).isEqualTo("macbook-pro-14-it");

        ProductDTO fetched = productService.getById(dto.id());
        assertThat(fetched.brand()).isEqualTo("Apple");
    }

    @Test
    void createProduct_duplicateSlug_throwsConflict() {
        productService.create(
            new CreateProductRequest("Duplicate", "dup-slug-it", null, null, null, null));

        assertThatThrownBy(() -> productService.create(
            new CreateProductRequest("Duplicate 2", "dup-slug-it", null, null, null, null)))
            .hasMessageContaining("slug");
    }

    @Test
    void productRepository_fullTextSearch_returnsMatchingProducts() {
        // Create products with distinct names
        productRepository.save(Product.builder()
            .name("Samsung Galaxy S24 Ultra")
            .slug("samsung-galaxy-s24-ultra-it")
            .brand("Samsung")
            .build());
        productRepository.save(Product.builder()
            .name("iPhone 15 Pro Max")
            .slug("iphone-15-pro-max-it")
            .brand("Apple")
            .build());

        // Flush so that tsvector index picks up new rows
        productRepository.flush();

        // Search for Samsung — should NOT return iPhone
        List<Product> results = productRepository.fullTextSearch("samsung", 10, 0);
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(p -> p.getName().toLowerCase().contains("samsung"));
    }

    @Test
    void sellerListingRepository_findCheapestAvailable_returnsLowestPrice() {
        Product product = productRepository.save(Product.builder()
            .name("Test Phone")
            .slug("test-phone-cheapest-it")
            .build());

        // Create two listings with different prices
        sellerListingRepository.save(SellerListing.builder()
            .product(product)
            .domain("shopee.vn")
            .sellerName("Shopee Mall")
            .externalUrl("https://shopee.vn/p/1")
            .currentPrice(new BigDecimal("15000000"))
            .scrapeStatus(ScrapeStatus.COMPLETED)
            .isAvailable(true)
            .build());

        sellerListingRepository.save(SellerListing.builder()
            .product(product)
            .domain("tiki.vn")
            .sellerName("Tiki")
            .externalUrl("https://tiki.vn/p/1")
            .currentPrice(new BigDecimal("14500000"))
            .scrapeStatus(ScrapeStatus.COMPLETED)
            .isAvailable(true)
            .build());

        sellerListingRepository.flush();

        var cheapest = sellerListingRepository
            .findFirstByProductIdAndIsAvailableTrueOrderByCurrentPriceAsc(product.getId());

        assertThat(cheapest).isPresent();
        assertThat(cheapest.get().getCurrentPrice())
            .isEqualByComparingTo(new BigDecimal("14500000"));
        assertThat(cheapest.get().getDomain()).isEqualTo("tiki.vn");
    }
}
