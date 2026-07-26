package com.bubbletea.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.bubbletea.notification.exception.NotificationErrorCode;
import com.bubbletea.notification.exception.NotificationException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationSseServiceExceptionTest {

  private final NotificationSseService notificationSseService = new NotificationSseService();

  @Nested
  @DisplayName("[EXCEPTION] SSE 연결")
  class ConnectException {

    @ParameterizedTest(name = "receiverId={0}")
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("수신자 ID가 유효하지 않으면 예외가 발생한다")
    void invalidReceiverId(Long receiverId) {
      NotificationException exception = catchThrowableOfType(
          NotificationException.class,
          () -> notificationSseService.connect(receiverId)
      );

      assertThat(exception.getErrorCode()).isEqualTo(NotificationErrorCode.INVALID_RECEIVER_ID);
      assertThat(emitters()).isEmpty();
    }

    @Test
    @DisplayName("SSE 연결 오류가 발생하면 emitter를 제거한다")
    void removeFailedEmitter() {
      SseEmitter emitter = notificationSseService.connect(1L);

      ReflectionTestUtils.invokeMethod(
          emitter,
          "initializeWithError",
          new IOException("connection closed")
      );

      assertThat(emitters()).doesNotContainKey(1L);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<Long, Set<SseEmitter>> emitters() {
    return (Map<Long, Set<SseEmitter>>) ReflectionTestUtils.getField(
        notificationSseService,
        "emitters"
    );
  }
}
