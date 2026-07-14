package com.bubbletea.notification.exception;

import com.bubbletea.notification.exception.dto.NotificationErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class NotificationExceptionHandler {

  @ExceptionHandler(NotificationException.class)
  public ResponseEntity<NotificationErrorResponseDto> handleNotificationException(
      NotificationException exception
  ) {
    NotificationErrorCode errorCode = exception.getErrorCode();

    return ResponseEntity.status(errorCode.getStatus())
        .body(NotificationErrorResponseDto.from(errorCode));
  }

  @ExceptionHandler({
      MethodArgumentTypeMismatchException.class,
      MissingServletRequestParameterException.class
  })
  public ResponseEntity<NotificationErrorResponseDto> handleInvalidRequestException(
      Exception exception
  ) {
    return ResponseEntity.status(NotificationErrorCode.INVALID_REQUEST.getStatus())
        .body(NotificationErrorResponseDto.from(NotificationErrorCode.INVALID_REQUEST));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<NotificationErrorResponseDto> handleException(Exception exception) {
    log.error("알림 서버 처리 중 예외 발생", exception);

    return ResponseEntity.status(NotificationErrorCode.INTERNAL_SERVER_ERROR.getStatus())
        .body(NotificationErrorResponseDto.from(NotificationErrorCode.INTERNAL_SERVER_ERROR));
  }
}
