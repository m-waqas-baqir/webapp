package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.media.PropertyImageResponse;
import com.app.backend.entity.LinkedEntityType;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.PropertyImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/property-images")
@RequiredArgsConstructor
@Tag(name = "Property images", description = "Upload and manage plot/rental images (JPEG/PNG/GIF/WebP).")
@SecurityRequirement(name = "bearer-jwt")
public class PropertyImageController {

    private final PropertyImageService propertyImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Upload one image for a plot or rental property")
    public ApiResponse<PropertyImageResponse> upload(
            @RequestParam LinkedEntityType entityType,
            @RequestParam Long entityId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(propertyImageService.upload(principal, entityType, entityId, file));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "List images for an entity")
    public ApiResponse<List<PropertyImageResponse>> listForEntity(
            @RequestParam LinkedEntityType entityType,
            @RequestParam Long entityId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(propertyImageService.list(principal, entityType, entityId));
    }

    @GetMapping("/{id}/file")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Download image bytes (Authorization header required)")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PropertyImageService.ImageFilePayload payload = propertyImageService.loadFile(principal, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.resource());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Delete an uploaded image")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        propertyImageService.delete(principal, id);
        return ApiResponse.ok("Deleted", null);
    }
}
