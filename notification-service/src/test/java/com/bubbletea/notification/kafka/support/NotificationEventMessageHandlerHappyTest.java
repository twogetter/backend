package com.bubbletea.notification.kafka.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.bubbletea.notification.application.dto.NotificationCreateCommandDto;
import com.bubbletea.notification.service.NotificationCreateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventMessageHandlerHappyTest {

  @Mock
  private NotificationCreateService notificationCreateService;

  @Nested
  @DisplayName("[HAPPY] Kafka 알림 이벤트 처리")
  class HandleEvent {

    @Test
    @DisplayName("JSON 이벤트를 명령으로 변환해 알림 생성을 요청한다")
    void handleValidEvent() {
      NotificationEventMessageHandler handler = new NotificationEventMessageHandler(
          new ObjectMapper(),
          notificationCreateService
      );
      String message = "{\"eventId\":\"event-1\",\"receiverId\":1}";

      handler.handle(message, TestEvent.class, event -> new NotificationCreateCommandDto(
          event.eventId(),
          event.receiverId(),
          "MEMBER_SIGNED_UP",
          Map.of(),
          null,
          Map.of()
      ));

      ArgumentCaptor<NotificationCreateCommandDto> commandCaptor =
          ArgumentCaptor.forClass(NotificationCreateCommandDto.class);
      then(notificationCreateService).should().create(commandCaptor.capture());
      assertThat(commandCaptor.getValue().eventId()).isEqualTo("event-1");
      assertThat(commandCaptor.getValue().receiverId()).isEqualTo(1L);
      assertThat(commandCaptor.getValue().notificationType()).isEqualTo("MEMBER_SIGNED_UP");
    }
  }

  private record TestEvent(String eventId, Long receiverId) {
  }
}
