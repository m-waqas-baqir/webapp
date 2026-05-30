package com.app.backend.dto.excel;

/** Row-level failure during Excel bulk import (1-based sheet row index including header row). */
public record BulkImportRowError(int row, String message) {}
