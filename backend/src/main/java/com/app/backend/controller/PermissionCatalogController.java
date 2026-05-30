package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.admin.PermissionResponse;
import com.app.backend.entity.Permission;
import com.app.backend.repository.PermissionRepository;
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
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "Catalog of assignable permissions")
@SecurityRequirement(name = "bearer-jwt")
public class PermissionCatalogController {

    private final PermissionRepository permissionRepository;

    @GetMapping
    @PreAuthorize("@access.canAccessUserManagement(authentication)")
    @Operation(summary = "List all permission definitions")
    public ApiResponse<List<PermissionResponse>> list() {
        List<PermissionResponse> rows = permissionRepository.findAll().stream()
                .sorted((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()))
                .map(PermissionCatalogController::toDto)
                .toList();
        return ApiResponse.ok(rows);
    }

    private static PermissionResponse toDto(Permission p) {
        return PermissionResponse.builder()
                .code(p.getCode())
                .description(p.getDescription())
                .module(p.getModule())
                .build();
    }
}
