package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.PageResponse;
import com.app.backend.dto.users.UserSummaryResponse;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import com.app.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user-directory")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Directory lookups for staff (e.g. assign agents).")
@SecurityRequirement(name = "bearer-jwt")
public class UserDirectoryController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "List users by role (for assignment dropdowns)")
    public ApiResponse<PageResponse<UserSummaryResponse>> listByRole(
            @RequestParam UserRole role,
            @ParameterObject
            @PageableDefault(size = 100, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(userRepository.findByRole(role, pageable).map(UserDirectoryController::toSummary)));
    }

    private static UserSummaryResponse toSummary(User u) {
        return UserSummaryResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole())
                .build();
    }
}
