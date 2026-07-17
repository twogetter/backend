package com.bubbletea.payment.global.exception;

import com.bubbletea.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    TOSS_PAYMENT_REJECTED(HttpStatus.BAD_REQUEST, "400", "카드 정보를 확인하세요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "401", "유효하지 않은 토큰입니다"),
    DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "결제 수단 저장 실패"),
    ALREADY_CONNECTED(HttpStatus.CONFLICT, "409", "이미 연동된 계정입니다"),
    PAYMENT_METHOD_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "결제 수단을 찾을 수 없습니다"),
    USER_BRANDPAY_AUTH_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "사용자 브랜드페이 인증 정보를 찾을 수 없습니다"),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "403", "접근 권한이 없습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "사용자를 찾을 수 없습니다"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "결제 정보를 찾을 수 없습니다"),
    EXTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "외부 서버 오류"),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "400", "결제 상태가 올바르지 않습니다"),
    INVALID_CANCEL_AMOUNT(HttpStatus.BAD_REQUEST, "400", "취소 금액이 올바르지 않습니다"),
    PAYMENT_CANCEL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500", "결제 취소 처리 중 오류가 발생했습니다"),
    CANCEL_UNKNOWN_HOLD(HttpStatus.BAD_REQUEST, "400", "취소 상태가 알 수 없는 보류 상태입니다. 시간이 지난 후 다시 확인해주세요."),
    PAYMENT_CANCEL_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "결제 취소 정보를 찾을 수 없습니다")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
