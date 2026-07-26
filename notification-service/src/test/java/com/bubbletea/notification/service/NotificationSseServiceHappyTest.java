package com.bubbletea.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bubbletea.notification.service.dto.NotificationResponseDto;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationSseServiceHappyTest {

  private final NotificationSseService notificationSseService = new NotificationSseService();

  @Nested
  @DisplayName("[HAPPY] SSE 연결")
  class Connect {

    @Test
    @DisplayName("수신자의 SSE 연결을 등록하고 최초 연결 이벤트를 전송한다")
    void connect() {
      SseEmitter emitter = notificationSseService.connect(1L);

      assertThat(emitter.getTimeout()).isEqualTo(3_600_000L);
      assertThat(emitters().get(1L)).containsExactly(emitter);
      assertThat(earlySendCount(emitter)).isPositive();
    }

    @Test
    @DisplayName("같은 수신자의 여러 SSE 연결을 모두 유지한다")
    void connectMultipleDevices() {
      SseEmitter first = notificationSseService.connect(1L);
      SseEmitter second = notificationSseService.connect(1L);

      assertThat(emitters().get(1L)).containsExactlyInAnyOrder(first, second);
    }
  }

  @Nested
  @DisplayName("[HAPPY] SSE 이벤트 전송")
  class SendEvent {

    @Test
    @DisplayName("연결된 수신자의 모든 emitter에 알림 이벤트를 전송한다")
    void sendNotification() {
      SseEmitter first = notificationSseService.connect(1L);
      SseEmitter second = notificationSseService.connect(1L);
      int firstEventCount = earlySendCount(first);
      int secondEventCount = earlySendCount(second);

      notificationSseService.sendNotification(1L, notification());

      assertThat(earlySendCount(first)).isGreaterThan(firstEventCount);
      assertThat(earlySendCount(second)).isGreaterThan(secondEventCount);
    }

    @Test
    @DisplayName("연결된 emitter가 없으면 알림 전송을 건너뛴다")
    void skipWithoutConnection() {
      notificationSseService.sendNotification(1L, notification());

      assertThat(emitters()).isEmpty();
    }

    @Test
    @DisplayName("연결된 모든 emitter에 heartbeat를 전송한다")
    void sendHeartbeat() {
      SseEmitter first = notificationSseService.connect(1L);
      SseEmitter second = notificationSseService.connect(2L);
      int firstEventCount = earlySendCount(first);
      int secondEventCount = earlySendCount(second);

      notificationSseService.sendHeartbeat();

      assertThat(earlySendCount(first)).isGreaterThan(firstEventCount);
      assertThat(earlySendCount(second)).isGreaterThan(secondEventCount);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<Long, Set<SseEmitter>> emitters() {
    return (Map<Long, Set<SseEmitter>>) ReflectionTestUtils.getField(
        notificationSseService,
        "emitters"
    );
  }

  private int earlySendCount(SseEmitter emitter) {
    Set<?> earlySendAttempts = (Set<?>) ReflectionTestUtils.getField(emitter, "earlySendAttempts");
    return earlySendAttempts.size();
  }

  private NotificationResponseDto notification() {
    return new NotificationResponseDto(
        10L,
        "MEMBER_SIGNED_UP",
        "가입 완료",
        "가입을 환영합니다.",
        null,
        Map.of(),
        null,
        LocalDateTime.of(2026, 7, 20, 10, 0)
    );
  }
}
