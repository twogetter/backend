package com.bubbletea.payment.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode {

    TEMP_ERROR_CODE(HttpStatus.NOT_FOUND, "404", "임시 에러코드입니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
