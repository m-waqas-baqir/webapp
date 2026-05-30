package com.app.backend.service.excel;

import com.app.backend.dto.excel.BulkImportResult;
import com.app.backend.dto.excel.BulkImportRowError;
import com.app.backend.entity.RentalProperty;
import com.app.backend.entity.UserRole;
import com.app.backend.excel.ExcelParseException;
import com.app.backend.excel.ExcelSheetReader;
import com.app.backend.excel.ExcelWorkbookWriter;
import com.app.backend.excel.ExcelSheetReader.ParsedRow;
import com.app.backend.repository.RentalPropertyRepository;
import com.app.backend.repository.RentalPropertySpecifications;
import com.app.backend.repository.SoftDeleteSpecifications;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.excel.catalog.RentalImportCatalog;
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
public class RentalExcelBulkService {

    public static final Set<String> IMPORT_HEADERS = Set.of(
            "title", "type", "address", "rentamount", "status", "ownername", "agentname");

    private static final List<String> EXPORT_HEADERS = List.of(
            "title", "type", "address", "rentAmount", "status", "ownerName", "agentName");

    private final RentalImportRowService rentalImportRowService;
    private final RentalPropertyRepository rentalPropertyRepository;
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
        RentalImportCatalog catalog = excelImportCatalogLoader.loadRentalCatalog();

        for (ParsedRow pr : rows) {
            if (RentalImportCatalog.isTemplateSampleRow(pr.valuesByNormalizedHeader())) {
                continue;
            }
            try {
                rentalImportRowService.importRow(actor, pr.valuesByNormalizedHeader(), catalog);
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
        Specification<RentalProperty> spec = Specification.where(SoftDeleteSpecifications.<RentalProperty>notDeleted())
                .and(RentalPropertySpecifications.visibleTo(principal));

        List<List<Object>> data = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, 500);
        Page<RentalProperty> page;
        do {
            page = rentalPropertyRepository.findAll(spec, pageable);
            for (RentalProperty r : page.getContent()) {
                String ownerCol = "";
                if (principal.getRole() != UserRole.AGENT && r.getOwner() != null) {
                    ownerCol = r.getOwner().getName();
                }
                String agentCol = "";
                if (principal.getRole() != UserRole.AGENT && r.getAssignedAgent() != null) {
                    agentCol = r.getAssignedAgent().getName();
                }
                data.add(List.of(
                        r.getTitle(),
                        r.getType().name(),
                        r.getAddress(),
                        r.getRentAmount(),
                        r.getStatus().name(),
                        ownerCol,
                        agentCol
                ));
            }
            pageable = page.nextPageable();
        } while (page.hasNext());

        return ExcelWorkbookWriter.writeSheet("Rentals", EXPORT_HEADERS, data);
    }
}
