package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.auth.UserResponse;
import com.app.backend.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Current user")
@SecurityRequirement(name = "bearer-jwt")
public class MeController {

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Current authenticated user")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        var b = UserResponse.builder()
                .id(principal.getId())
                .name(principal.getName())
                .email(principal.getEmail())
                .role(principal.getRole())
                .active(principal.isActive());
        for (String p : principal.getPermissionCodes()) {
            b.permission(p);
        }
        UserResponse body = b.build();
        return ApiResponse.ok(body);
    }
}
