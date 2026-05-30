package com.app.backend.controller.advice;

import com.app.backend.dto.ApiResponse;
import com.app.backend.excel.ExcelParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExcelBulkImportExceptionHandler {

    @ExceptionHandler(ExcelParseException.class)
    public ResponseEntity<ApiResponse<Void>> handleParse(ExcelParseException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ex.getMessage(), null, "EXCEL_PARSE_ERROR"));
    }
}
