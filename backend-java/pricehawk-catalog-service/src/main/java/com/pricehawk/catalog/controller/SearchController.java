package com.pricehawk.catalog.controller;

import com.pricehawk.catalog.dto.request.TextSearchRequest;
import com.pricehawk.catalog.dto.request.UrlSearchRequest;
import com.pricehawk.catalog.dto.response.ScrapeJobDTO;
import com.pricehawk.catalog.dto.response.SearchResultDTO;
import com.pricehawk.catalog.service.SearchService;
import com.pricehawk.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Full-text product search.
     * GET /api/v1/search?query=iphone+14&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<SearchResultDTO>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var request = new TextSearchRequest(query, page, size);
        return ResponseEntity.ok(ApiResponse.ok(searchService.searchByText(request)));
    }

    /**
     * Submit a product URL to be scraped.
     * Returns 202 Accepted with a job ID — client polls for the result.
     * X-User-Id is injected by the API Gateway after JWT validation (nullable for anonymous).
     */
    @PostMapping("/url")
    public ResponseEntity<ApiResponse<ScrapeJobDTO>> submitUrl(
            @Valid @RequestBody UrlSearchRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.ok(searchService.submitUrlForScraping(request, userId)));
    }
}
