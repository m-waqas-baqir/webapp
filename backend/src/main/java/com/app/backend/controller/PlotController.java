package com.app.backend.controller;

import com.app.backend.dto.ApiResponse;
import com.app.backend.dto.PageResponse;
import com.app.backend.dto.plots.PlotRequest;
import com.app.backend.dto.plots.PlotResponse;
import com.app.backend.entity.PlotStatus;
import com.app.backend.dto.excel.BulkImportResult;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.PlotService;
import com.app.backend.service.excel.ExcelTemplateGeneratorService;
import com.app.backend.service.excel.PlotExcelBulkService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/plots")
@RequiredArgsConstructor
@Tag(name = "Plots", description = "RBAC: DIRECTOR/ADMIN full CRUD; AGENT sees assigned plots only.")
@SecurityRequirement(name = "bearer-jwt")
public class PlotController {

    private final PlotService plotService;
    private final PlotExcelBulkService plotExcelBulkService;
    private final ExcelTemplateGeneratorService excelTemplateGeneratorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "List plots (paginated; filter by phaseId, khayabanId, status; AGENT scoped)")
    public ApiResponse<PageResponse<PlotResponse>> list(
            @Parameter(description = "Filter by phase")
            @RequestParam(required = false) Long phaseId,
            @Parameter(description = "Filter by khayaban")
            @RequestParam(required = false) Long khayabanId,
            @Parameter(description = "Filter by plot status")
            @RequestParam(required = false) PlotStatus status,
            @Parameter(description = "Minimum price (inclusive)")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price (inclusive)")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Minimum size (inclusive)")
            @RequestParam(required = false) BigDecimal minSize,
            @Parameter(description = "Maximum size (inclusive)")
            @RequestParam(required = false) BigDecimal maxSize,
            @ParameterObject
            @PageableDefault(size = 20, sort = "plotNumber", direction = Sort.Direction.ASC)
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(plotService.list(
                principal,
                Optional.ofNullable(phaseId),
                Optional.ofNullable(khayabanId),
                Optional.ofNullable(status),
                Optional.ofNullable(minPrice),
                Optional.ofNullable(maxPrice),
                Optional.ofNullable(minSize),
                Optional.ofNullable(maxSize),
                pageable
        ));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Export plots to Excel (.xlsx); scoped like list")
    public ResponseEntity<byte[]> exportExcel(@AuthenticationPrincipal UserPrincipal principal) throws IOException {
        byte[] body = plotExcelBulkService.exportExcel(principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plots-export.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Download pre-formatted plot import template (.xlsx) with reference data and validation")
    public ResponseEntity<byte[]> downloadImportTemplate() throws IOException {
        byte[] body = excelTemplateGeneratorService.plotTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plots-import-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Get plot by id (AGENT: only if assigned)")
    public ApiResponse<PlotResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(plotService.get(principal, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Create plot (AGENT: self-assigned draft; cannot set registry owner)")
    public ApiResponse<PlotResponse> create(
            @Valid @RequestBody PlotRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(plotService.create(principal, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','AGENT')")
    @Operation(summary = "Update plot (AGENT: only records they created; cannot set registry owner)")
    public ApiResponse<PlotResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PlotRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(plotService.update(principal, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN') or @access.canDeletePlot(authentication, #id)")
    @Operation(summary = "Delete plot")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        plotService.delete(principal, id);
        return ApiResponse.ok("Deleted", null);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    @Operation(summary = "Bulk import plots from Excel (.xlsx)")
    public ApiResponse<BulkImportResult> importExcel(
            @Parameter(description = "Excel workbook (.xlsx)") @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(plotExcelBulkService.importExcel(principal, file));
    }
}
