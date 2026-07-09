package com.bubbletea.payment.global.exception;

import com.bubbletea.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    TEMP_ERROR_CODE(HttpStatus.NOT_FOUND, "404", "임시 에러코드입니다."),
    TOSS_API_ERROR(HttpStatus.BAD_REQUEST, "400", "토스 API 연동 실패"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "401", "유효하지 않은 토큰입니다"),
    DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "결제 수단 저장 실패"),
    ALREADY_CONNECTED(HttpStatus.CONFLICT, "409", "이미 연동된 계정입니다"),
    PAYMENT_METHOD_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "결제 수단을 찾을 수 없습니다"),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "403", "접근 권한이 없습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "사용자를 찾을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
