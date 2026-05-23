package com.pricehawk.catalog.service;

import com.pricehawk.catalog.dto.request.TextSearchRequest;
import com.pricehawk.catalog.dto.request.UrlSearchRequest;
import com.pricehawk.catalog.dto.response.ProductSummaryDTO;
import com.pricehawk.catalog.dto.response.ScrapeJobDTO;
import com.pricehawk.catalog.dto.response.SearchResultDTO;
import com.pricehawk.catalog.mapper.CatalogMapper;
import com.pricehawk.catalog.repository.ProductRepository;
import com.pricehawk.common.constants.MessageQueueConstants;
import com.pricehawk.common.event.ScrapeRequestEvent;
import com.pricehawk.messaging.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ProductRepository productRepository;
    private final CatalogMapper catalogMapper;
    private final EventPublisher eventPublisher;

    /**
     * Full-text search using PostgreSQL tsvector.
     * Input query is sanitized to safe tsquery format (AND-joined terms).
     */
    @Transactional(readOnly = true)
    public SearchResultDTO searchByText(TextSearchRequest request) {
        String tsQuery = sanitizeToTsQuery(request.query());
        int offset = request.page() * request.size();

        List<ProductSummaryDTO> products = catalogMapper.toProductSummaryDTOList(
            productRepository.fullTextSearch(tsQuery, request.size(), offset)
        );
        long total = productRepository.countFullTextSearch(tsQuery);
        int totalPages = (int) Math.ceil((double) total / request.size());

        log.debug("FTS query='{}' tsQuery='{}' results={}", request.query(), tsQuery, total);

        return new SearchResultDTO(
            products,
            request.page(),
            request.size(),
            total,
            totalPages,
            (request.page() + 1) >= totalPages
        );
    }

    /**
     * Submit a product URL for scraping.
     * Publishes a ScrapeRequestEvent to the scrape exchange — Scraper Service picks it up.
     * Returns a job ID the client can use to poll for status.
     *
     * @param userId nullable — anonymous users may submit URLs too
     */
    public ScrapeJobDTO submitUrlForScraping(UrlSearchRequest request, String userId) {
        String jobId = UUID.randomUUID().toString();

        ScrapeRequestEvent event = ScrapeRequestEvent.builder()
            .jobId(jobId)
            .url(request.url())
            .userId(userId)
            .requestedAt(Instant.now())
            .build();

        eventPublisher.publish(
            MessageQueueConstants.SCRAPE_EXCHANGE,
            MessageQueueConstants.SCRAPE_REQUEST_KEY,
            event
        );

        log.info("Scrape job submitted: jobId={} url={} userId={}", jobId, request.url(), userId);

        return new ScrapeJobDTO(UUID.fromString(jobId), request.url(), "PENDING", event.getRequestedAt());
    }

    /**
     * Convert free-text query to PostgreSQL tsquery format.
     * Strips dangerous characters and joins tokens with & (AND semantics).
     * e.g. "iPhone 14 Pro" → "iphone & 14 & pro"
     */
    private String sanitizeToTsQuery(String query) {
        return query.trim()
            .replaceAll("[^a-zA-Z0-9\\sÀ-ỹ]", " ") // strip special chars, keep Vietnamese
            .trim()
            .replaceAll("\\s+", " ")
            .toLowerCase()
            .replace(" ", " & ");
    }
}
