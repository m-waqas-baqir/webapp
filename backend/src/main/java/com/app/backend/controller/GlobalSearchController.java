package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.search.GlobalSearchResponse;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.GlobalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Cross-domain text search (owners, listings, plots, rentals, phases, khayabans)")
@SecurityRequirement(name = "bearer-jwt")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Global search across core entities")
    public ApiResponse<GlobalSearchResponse> search(
            @Parameter(description = "Free-text query (min 2 characters)")
            @RequestParam String q,
            @Parameter(description = "Max hits per entity type (1–25)")
            @RequestParam(defaultValue = "5") int limitPerType,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(globalSearchService.search(principal, q, limitPerType));
    }
}
