package com.app.backend.service.excel;

import com.app.backend.dto.excel.BulkImportResult;
import com.app.backend.dto.excel.BulkImportRowError;
import com.app.backend.entity.Khayaban;
import com.app.backend.entity.UserRole;
import com.app.backend.excel.ExcelParseException;
import com.app.backend.excel.ExcelSheetReader;
import com.app.backend.excel.ExcelWorkbookWriter;
import com.app.backend.excel.ExcelSheetReader.ParsedRow;
import com.app.backend.repository.KhayabanRepository;
import com.app.backend.repository.KhayabanSpecifications;
import com.app.backend.repository.SoftDeleteSpecifications;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.excel.catalog.KhayabanImportCatalog;
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
public class KhayabanExcelBulkService {

    public static final Set<String> IMPORT_HEADERS = Set.of("name", "phasename");

    private static final List<String> EXPORT_HEADERS = List.of("name", "phaseName");

    private final KhayabanImportRowService khayabanImportRowService;
    private final KhayabanRepository khayabanRepository;
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
        KhayabanImportCatalog catalog = excelImportCatalogLoader.loadKhayabanCatalog();

        for (ParsedRow pr : rows) {
            if (KhayabanImportCatalog.isTemplateSampleRow(pr.valuesByNormalizedHeader())) {
                continue;
            }
            try {
                khayabanImportRowService.importRow(actor, pr.valuesByNormalizedHeader(), catalog);
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
        Specification<Khayaban> spec = Specification.where(SoftDeleteSpecifications.<Khayaban>notDeleted())
                .and(KhayabanSpecifications.phaseIdEquals(null));

        List<List<Object>> data = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, 500);
        Page<Khayaban> page;
        do {
            page = khayabanRepository.findAll(spec, pageable);
            for (Khayaban k : page.getContent()) {
                String phaseName = k.getPhase() != null ? k.getPhase().getName() : "";
                data.add(List.of(k.getName(), phaseName));
            }
            pageable = page.nextPageable();
        } while (page.hasNext());

        return ExcelWorkbookWriter.writeSheet("Khayabans", EXPORT_HEADERS, data);
    }
}
