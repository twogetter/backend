package com.bubbletea.common.response;


import com.bubbletea.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    String status,
    String message,
    T data,
    String error,
    Instant timestamp
) {

    public enum ResponseStatus {
        SUCCESS, ERROR
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(
            ResponseStatus.SUCCESS.name(), message, data, null, Instant.now()
        );
    }
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "요청이 성공적으로 처리되었습니다.");
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(
            ResponseStatus.ERROR.name(), message, null, errorCode.getCode(), Instant.now()
        );
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }

}
