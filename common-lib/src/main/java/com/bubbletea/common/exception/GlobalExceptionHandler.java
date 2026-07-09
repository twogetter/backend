package com.bubbletea.common.exception;


import com.bubbletea.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException e) {
        log.warn("[exception] code={}, message={}", e.getErrorCode().getCode(), e.getMessage());

        return ResponseEntity
            .status(e.getErrorCode().getHttpStatus())
            .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

}
