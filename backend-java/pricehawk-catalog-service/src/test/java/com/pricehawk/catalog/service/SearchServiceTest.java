package com.pricehawk.catalog.service;

import com.pricehawk.catalog.dto.request.TextSearchRequest;
import com.pricehawk.catalog.dto.request.UrlSearchRequest;
import com.pricehawk.catalog.dto.response.ProductSummaryDTO;
import com.pricehawk.catalog.dto.response.ScrapeJobDTO;
import com.pricehawk.catalog.dto.response.SearchResultDTO;
import com.pricehawk.catalog.mapper.CatalogMapper;
import com.pricehawk.catalog.repository.ProductRepository;
import com.pricehawk.common.event.ScrapeRequestEvent;
import com.pricehawk.messaging.publisher.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CatalogMapper catalogMapper;
    @Mock EventPublisher eventPublisher;
    @InjectMocks SearchService searchService;

    @Test
    void searchByText_returnsResults() {
        var request = new TextSearchRequest("iphone 14", 0, 10);

        when(productRepository.fullTextSearch(anyString(), eq(10), eq(0)))
            .thenReturn(List.of());
        when(productRepository.countFullTextSearch(anyString())).thenReturn(0L);
        when(catalogMapper.toProductSummaryDTOList(any())).thenReturn(List.of());

        SearchResultDTO result = searchService.searchByText(request);

        assertThat(result.products()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.last()).isTrue();
    }

    @Test
    void searchByText_sanitizesQuery() {
        var request = new TextSearchRequest("iPhone 14!!", 0, 5);

        when(productRepository.fullTextSearch(anyString(), anyInt(), anyInt()))
            .thenReturn(List.of());
        when(productRepository.countFullTextSearch(anyString())).thenReturn(0L);
        when(catalogMapper.toProductSummaryDTOList(any())).thenReturn(List.of());

        searchService.searchByText(request);

        // Verify that the tsQuery sent to DB does not contain dangerous characters
        verify(productRepository).fullTextSearch(
            argThat(q -> !q.contains("!") && q.contains("&")),
            anyInt(), anyInt()
        );
    }

    @Test
    void submitUrlForScraping_publishesEvent_returnsPendingJob() {
        var request = new UrlSearchRequest("https://shopee.vn/test-product");

        ScrapeJobDTO job = searchService.submitUrlForScraping(request, "user-123");

        assertThat(job.url()).isEqualTo("https://shopee.vn/test-product");
        assertThat(job.status()).isEqualTo("PENDING");
        assertThat(job.jobId()).isNotNull();

        ArgumentCaptor<ScrapeRequestEvent> captor = ArgumentCaptor.forClass(ScrapeRequestEvent.class);
        verify(eventPublisher).publish(
            eq("pricehawk.scrape"),
            eq("scrape.request"),
            captor.capture()
        );
        assertThat(captor.getValue().getUrl()).isEqualTo("https://shopee.vn/test-product");
        assertThat(captor.getValue().getUserId()).isEqualTo("user-123");
    }

    @Test
    void submitUrlForScraping_anonymousUser_publishesWithNullUserId() {
        var request = new UrlSearchRequest("https://tiki.vn/product");

        searchService.submitUrlForScraping(request, null);

        ArgumentCaptor<ScrapeRequestEvent> captor = ArgumentCaptor.forClass(ScrapeRequestEvent.class);
        verify(eventPublisher).publish(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue().getUserId()).isNull();
    }
}
