package com.bubbletea.product.application.exception;

import com.bubbletea.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductApplicationErrorCode implements ErrorCode {

    INVALID_CURSOR(
        HttpStatus.BAD_REQUEST,
        "PRODUCT-INVALID-CURSOR",
        "올바르지 않은 커서 값입니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
