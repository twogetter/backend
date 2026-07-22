package com.bubbletea.order.presentation;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.order.domain.exception.OrderErrorCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class OrderValidationExceptionHandler {

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
    log.warn("[validation] {}", e.getMessage());
    return ResponseEntity
        .status(OrderErrorCode.INVALID_REQUEST.getHttpStatus())
        .body(ApiResponse.error(OrderErrorCode.INVALID_REQUEST));
  }
}
