package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.PageResponse;
import com.app.backend.dto.owners.OwnerRequest;
import com.app.backend.dto.owners.OwnerResponse;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.OwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

@RestController
@RequestMapping("/api/v1/owners")
@RequiredArgsConstructor
@Tag(
        name = "Owners",
        description = "Owner registry. DIRECTOR/ADMIN, or agents with OWNER_VIEW/OWNER_EDIT permissions."
)
@SecurityRequirement(name = "bearer-jwt")
public class OwnerController {

    private final OwnerService ownerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "List owners (paginated; active registry rows only)")
    public ApiResponse<PageResponse<OwnerResponse>> list(
            @ParameterObject
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(ownerService.list(principal, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.canViewOwnerRegistry(authentication)")
    @Operation(summary = "Get owner by id")
    public ApiResponse<OwnerResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(ownerService.get(principal, id));
    }

    @PostMapping
    @PreAuthorize("@access.canMutateOwnerRegistry(authentication)")
    @Operation(summary = "Create owner (full payload; CNIC unique)")
    public ApiResponse<OwnerResponse> create(
            @Valid @RequestBody OwnerRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(ownerService.create(principal, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@access.canMutateOwnerRegistry(authentication)")
    @Operation(summary = "Update owner")
    public ApiResponse<OwnerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody OwnerRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(ownerService.update(principal, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.canMutateOwnerRegistry(authentication)")
    @Operation(summary = "Delete owner")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ownerService.delete(principal, id);
        return ApiResponse.ok("Deleted", null);
    }
}
