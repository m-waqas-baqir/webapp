/** Matches backend {@link com.app.backend.dto.excel.BulkImportResult}. */
export interface BulkImportResult {
  successCount: number;
  failureCount: number;
  errors: BulkImportRowError[];
}

export interface BulkImportRowError {
  row: number;
  message: string;
}
