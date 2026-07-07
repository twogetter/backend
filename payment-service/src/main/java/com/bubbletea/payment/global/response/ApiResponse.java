package com.bubbletea.payment.global.response;

import com.bubbletea.payment.global.exception.PaymentErrorCode;
import java.time.LocalDateTime;

public record ApiResponse<T> (
        String status,
        String message,
        T data,
        PaymentErrorCode error,
        LocalDateTime timestamp

) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>( "SUCCESS","API 요청에 성공했습니다", data, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>("SUCCESS","API 요청에 성공했습니다", null, null, LocalDateTime.now());
    }

    public static ApiResponse<?> error(PaymentErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null, errorCode, LocalDateTime.now());
    }

}
