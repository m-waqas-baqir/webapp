package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.PageResponse;
import com.app.backend.dto.plots.PhaseRequest;
import com.app.backend.dto.plots.PhaseResponse;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.PhaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/phases")
@RequiredArgsConstructor
@Tag(name = "Phases", description = "Plot hierarchy: phases. Mutations: DIRECTOR/ADMIN.")
@SecurityRequirement(name = "bearer-jwt")
public class PhaseController {

    private final PhaseService phaseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "List phases (paginated & sortable)")
    public ApiResponse<PageResponse<PhaseResponse>> list(
            @ParameterObject
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return ApiResponse.ok(phaseService.list(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Get phase by id")
    public ApiResponse<PhaseResponse> get(@Parameter(description = "Phase id") @PathVariable Long id) {
        return ApiResponse.ok(phaseService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Create phase")
    public ApiResponse<PhaseResponse> create(
            @Valid @RequestBody PhaseRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(phaseService.create(principal, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Update phase")
    public ApiResponse<PhaseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PhaseRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(phaseService.update(principal, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Delete phase (only if no khayabans)")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        phaseService.delete(principal, id);
        return ApiResponse.ok("Deleted", null);
    }
}
