package com.bubbletea.notification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode {

  INVALID_RECEIVER_ID(HttpStatus.BAD_REQUEST, "receiverId는 1 이상이어야 합니다."),
  INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "페이지 요청 값이 올바르지 않습니다."),
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
  NOTIFICATION_TEMPLATE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "알림 템플릿을 찾을 수 없습니다."),
  NOTIFICATION_TEMPLATE_VARIABLE_NOT_FOUND(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "알림 템플릿 변수를 찾을 수 없습니다."
  ),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알림 서버 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String message;
}
