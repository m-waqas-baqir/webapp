package com.app.backend.dto.excel;

import java.util.List;

public record BulkImportResult(int successCount, int failureCount, List<BulkImportRowError> errors) {}
