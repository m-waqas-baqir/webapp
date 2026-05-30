package com.app.backend.service.excel;

import com.app.backend.dto.excel.BulkImportResult;
import com.app.backend.dto.excel.BulkImportRowError;
import com.app.backend.entity.Plot;
import com.app.backend.entity.UserRole;
import com.app.backend.excel.ExcelParseException;
import com.app.backend.excel.ExcelSheetReader;
import com.app.backend.excel.ExcelWorkbookWriter;
import com.app.backend.excel.ExcelSheetReader.ParsedRow;
import com.app.backend.repository.PlotRepository;
import com.app.backend.repository.PlotSpecifications;
import com.app.backend.repository.SoftDeleteSpecifications;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.excel.catalog.PlotImportCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlotExcelBulkService {

    public static final Set<String> IMPORT_HEADERS = Set.of(
            "plotnumber", "size", "price", "status", "phasename", "khayabanname", "ownername", "agentname");

    private static final List<String> EXPORT_HEADERS = List.of(
            "plotNumber", "size", "price", "status", "phaseName", "khayabanName", "ownerName", "agentName");

    private final PlotImportRowService plotImportRowService;
    private final PlotRepository plotRepository;
    private final ExcelImportCatalogLoader excelImportCatalogLoader;

    public BulkImportResult importExcel(UserPrincipal actor, MultipartFile file) {
        if (actor.getRole() == UserRole.AGENT) {
            throw new IllegalStateException("Bulk import is not available to AGENT");
        }
        if (file == null || file.isEmpty()) {
            throw new ExcelParseException("No file uploaded");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            throw new ExcelParseException("File must be an Excel .xlsx workbook");
        }

        final List<ParsedRow> rows;
        try {
            rows = ExcelSheetReader.readDataRows(file.getInputStream(), IMPORT_HEADERS);
        } catch (IOException e) {
            throw new ExcelParseException("Could not read Excel file", e);
        }

        int success = 0;
        int failure = 0;
        List<BulkImportRowError> errors = new ArrayList<>();
        PlotImportCatalog catalog = excelImportCatalogLoader.loadPlotCatalog();

        for (ParsedRow pr : rows) {
            if (PlotImportCatalog.isTemplateSampleRow(pr.valuesByNormalizedHeader())) {
                continue;
            }
            try {
                plotImportRowService.importRow(actor, pr.valuesByNormalizedHeader(), catalog);
                success++;
            } catch (Exception ex) {
                failure++;
                String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                errors.add(new BulkImportRowError(pr.excelRowNumber(), msg));
            }
        }

        return new BulkImportResult(success, failure, errors);
    }

    public byte[] exportExcel(UserPrincipal principal) throws IOException {
        Specification<Plot> spec = Specification.where(SoftDeleteSpecifications.<Plot>notDeleted())
                .and(PlotSpecifications.visibleTo(principal));

        List<List<Object>> data = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, 500);
        Page<Plot> page;
        do {
            page = plotRepository.findAll(spec, pageable);
            for (Plot p : page.getContent()) {
                String ownerCol = "";
                if (principal.getRole() != UserRole.AGENT && p.getOwner() != null) {
                    ownerCol = p.getOwner().getName();
                }
                String agentCol = p.getAssignedAgent() != null ? p.getAssignedAgent().getName() : "";
                data.add(List.of(
                        p.getPlotNumber(),
                        p.getSize(),
                        p.getPrice(),
                        p.getStatus().name(),
                        p.getPhase() != null ? p.getPhase().getName() : "",
                        p.getKhayaban() != null ? p.getKhayaban().getName() : "",
                        ownerCol,
                        agentCol
                ));
            }
            pageable = page.nextPageable();
        } while (page.hasNext());

        return ExcelWorkbookWriter.writeSheet("Plots", EXPORT_HEADERS, data);
    }
}
