package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.PageResponse;
import com.app.backend.dto.admin.ManagedUserDetailResponse;
import com.app.backend.dto.admin.ManagedUserListItemResponse;
import com.app.backend.dto.admin.UserCreateRequest;
import com.app.backend.dto.admin.UserPermissionsRequest;
import com.app.backend.dto.admin.UserUpdateRequest;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.UserManagementService;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User management", description = "Accounts and permission grants (staff only).")
@SecurityRequirement(name = "bearer-jwt")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    @PreAuthorize("@access.canAccessUserManagement(authentication)")
    @Operation(summary = "List users (paginated)")
    public ApiResponse<PageResponse<ManagedUserListItemResponse>> list(
            @ParameterObject
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(userManagementService.list(principal, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.canAccessUserManagement(authentication)")
    @Operation(summary = "Get user detail including permission codes")
    public ApiResponse<ManagedUserDetailResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(userManagementService.get(principal, id));
    }

    @PostMapping
    @PreAuthorize("@access.canAccessUserManagement(authentication)")
    @Operation(summary = "Create user account")
    public ApiResponse<ManagedUserDetailResponse> create(
            @Valid @RequestBody UserCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(userManagementService.create(principal, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@access.canAccessUserManagement(authentication)")
    @Operation(summary = "Update user account")
    public ApiResponse<ManagedUserDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(userManagementService.update(principal, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.canAccessUserManagement(authentication)")
    @Operation(summary = "Soft-deactivate user (sets active=false)")
    public ApiResponse<Void> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userManagementService.deactivate(principal, id);
        return ApiResponse.ok("Deactivated", null);
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("@access.canAssignPermissions(authentication)")
    @Operation(summary = "Replace permission grants for a user")
    public ApiResponse<Void> replacePermissions(
            @PathVariable Long id,
            @Valid @RequestBody UserPermissionsRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userManagementService.replacePermissions(principal, id, request);
        return ApiResponse.ok("Permissions updated", null);
    }
}
