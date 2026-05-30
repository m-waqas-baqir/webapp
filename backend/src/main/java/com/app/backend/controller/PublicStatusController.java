package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.service.ApplicationStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Status", description = "Authenticated application status")
public class PublicStatusController {

    private final ApplicationStatusService applicationStatusService;

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Application status")
    public ApiResponse<Map<String, String>> status() {
        return ApiResponse.ok(applicationStatusService.getStatus());
    }
}
