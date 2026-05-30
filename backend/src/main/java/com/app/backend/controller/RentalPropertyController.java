package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.PageResponse;
import com.app.backend.dto.excel.BulkImportResult;
import com.app.backend.dto.rentals.RentalPropertyRequest;
import com.app.backend.dto.rentals.RentalPropertyResponse;
import com.app.backend.entity.RentalPropertyStatus;
import com.app.backend.entity.RentalPropertyType;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.RentalPropertyService;
import com.app.backend.service.excel.ExcelTemplateGeneratorService;
import com.app.backend.service.excel.RentalExcelBulkService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/rental-properties")
@RequiredArgsConstructor
@Tag(name = "Rental properties", description = "RBAC: DIRECTOR/ADMIN full CRUD; AGENT sees assigned rentals only.")
@SecurityRequirement(name = "bearer-jwt")
public class RentalPropertyController {

    private final RentalPropertyService rentalPropertyService;
    private final RentalExcelBulkService rentalExcelBulkService;
    private final ExcelTemplateGeneratorService excelTemplateGeneratorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "List rental properties (paginated; filter by type, status; AGENT scoped)")
    public ApiResponse<PageResponse<RentalPropertyResponse>> list(
            @Parameter(description = "Filter by property type")
            @RequestParam(required = false) RentalPropertyType type,
            @Parameter(description = "Filter by listing status")
            @RequestParam(required = false) RentalPropertyStatus status,
            @Parameter(description = "Minimum rent (inclusive)")
            @RequestParam(required = false) BigDecimal minRent,
            @Parameter(description = "Maximum rent (inclusive)")
            @RequestParam(required = false) BigDecimal maxRent,
            @ParameterObject
            @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC)
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(rentalPropertyService.list(
                principal,
                Optional.ofNullable(type),
                Optional.ofNullable(status),
                Optional.ofNullable(minRent),
                Optional.ofNullable(maxRent),
                pageable
        ));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Export rental properties to Excel (.xlsx); scoped like list")
    public ResponseEntity<byte[]> exportExcel(@AuthenticationPrincipal UserPrincipal principal) throws IOException {
        byte[] body = rentalExcelBulkService.exportExcel(principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rentals-export.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Download pre-formatted rental import template (.xlsx) with reference data and validation")
    public ResponseEntity<byte[]> downloadImportTemplate() throws IOException {
        byte[] body = excelTemplateGeneratorService.rentalTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rentals-import-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Get rental property by id (AGENT: only if assigned)")
    public ApiResponse<RentalPropertyResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(rentalPropertyService.get(principal, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Create rental property (AGENT: draft without registry owner)")
    public ApiResponse<RentalPropertyResponse> create(
            @Valid @RequestBody RentalPropertyRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(rentalPropertyService.create(principal, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Update rental property (AGENT: only records they created)")
    public ApiResponse<RentalPropertyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RentalPropertyRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(rentalPropertyService.update(principal, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Delete rental property")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        rentalPropertyService.delete(principal, id);
        return ApiResponse.ok("Deleted", null);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Bulk import rental properties from Excel (.xlsx)")
    public ApiResponse<BulkImportResult> importExcel(
            @Parameter(description = "Excel workbook (.xlsx)") @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(rentalExcelBulkService.importExcel(principal, file));
    }
}
