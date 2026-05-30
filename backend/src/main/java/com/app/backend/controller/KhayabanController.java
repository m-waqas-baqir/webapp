package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.PageResponse;
import com.app.backend.dto.excel.BulkImportResult;
import com.app.backend.dto.plots.KhayabanRequest;
import com.app.backend.dto.plots.KhayabanResponse;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.KhayabanService;
import com.app.backend.service.excel.ExcelTemplateGeneratorService;
import com.app.backend.service.excel.KhayabanExcelBulkService;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/khayabans")
@RequiredArgsConstructor
@Tag(name = "Khayabans", description = "Streets/blocks within a phase. Filter by phaseId. Mutations: DIRECTOR/ADMIN.")
@SecurityRequirement(name = "bearer-jwt")
public class KhayabanController {

    private final KhayabanService khayabanService;
    private final KhayabanExcelBulkService khayabanExcelBulkService;
    private final ExcelTemplateGeneratorService excelTemplateGeneratorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "List khayabans (paginated; optional phaseId filter)")
    public ApiResponse<PageResponse<KhayabanResponse>> list(
            @Parameter(description = "Restrict to this phase")
            @RequestParam(required = false) Long phaseId,
            @ParameterObject
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return ApiResponse.ok(khayabanService.list(Optional.ofNullable(phaseId), pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Export khayabans to Excel (.xlsx)")
    public ResponseEntity<byte[]> exportExcel(@AuthenticationPrincipal UserPrincipal principal) throws IOException {
        byte[] body = khayabanExcelBulkService.exportExcel(principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"khayabans-export.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Download pre-formatted khayaban import template (.xlsx) with reference data and validation")
    public ResponseEntity<byte[]> downloadImportTemplate() throws IOException {
        byte[] body = excelTemplateGeneratorService.khayabanTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"khayabans-import-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Get khayaban by id")
    public ApiResponse<KhayabanResponse> get(@Parameter(description = "Khayaban id") @PathVariable Long id) {
        return ApiResponse.ok(khayabanService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Create khayaban")
    public ApiResponse<KhayabanResponse> create(
            @Valid @RequestBody KhayabanRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(khayabanService.create(principal, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Update khayaban")
    public ApiResponse<KhayabanResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody KhayabanRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(khayabanService.update(principal, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Delete khayaban (only if no plots)")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        khayabanService.delete(principal, id);
        return ApiResponse.ok("Deleted", null);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Bulk import khayabans from Excel (.xlsx)")
    public ApiResponse<BulkImportResult> importExcel(
            @Parameter(description = "Excel workbook (.xlsx)") @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(khayabanExcelBulkService.importExcel(principal, file));
    }
}
