package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.listing.ListingRequest;
import com.app.backend.dto.listing.ListingResponse;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
@Tag(name = "Listings", description = "RBAC: DIRECTOR/ADMIN manage; AGENT sees assigned listings only (no owner PII).")
@SecurityRequirement(name = "bearer-jwt")
public class ListingController {

    private final ListingService listingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "List listings (scoped for AGENT)")
    public ApiResponse<List<ListingResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(listingService.listFor(principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Get listing by id (AGENT: only if assigned)")
    public ApiResponse<ListingResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(listingService.getFor(principal, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Create listing (not available to AGENT)")
    public ApiResponse<ListingResponse> create(
            @Valid @RequestBody ListingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(listingService.create(principal, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Update listing (not available to AGENT)")
    public ApiResponse<ListingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ListingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(listingService.update(principal, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Delete listing (not available to AGENT)")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        listingService.delete(principal, id);
        return ApiResponse.ok("Deleted", null);
    }
}
