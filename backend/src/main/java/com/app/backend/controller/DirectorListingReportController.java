package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.listing.OwnerFullDetailResponse;
import com.app.backend.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports/listings")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Director-only full owner visibility")
@SecurityRequirement(name = "bearer-jwt")
public class DirectorListingReportController {

    private final ListingService listingService;

    @GetMapping("/owners")
    @PreAuthorize("hasRole('DIRECTOR')")
    @Operation(summary = "Full owner PII across all listings (DIRECTOR only)")
    public ApiResponse<List<OwnerFullDetailResponse>> ownerDetails() {
        return ApiResponse.ok(listingService.listOwnerDetailsForDirector());
    }
}
