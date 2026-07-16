package com.bubbletea.notification.exception.dto;

import com.bubbletea.notification.exception.NotificationErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationErrorResponseDto(
    @Schema(description = "오류 코드", example = "NOTIFICATION_NOT_FOUND") String code,
    @Schema(description = "오류 메시지", example = "알림을 찾을 수 없습니다.") String message
) {

  public static NotificationErrorResponseDto from(NotificationErrorCode errorCode) {
    return new NotificationErrorResponseDto(errorCode.name(), errorCode.getMessage());
  }
}
