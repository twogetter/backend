package com.bubbletea.notification.kafka.support;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bubbletea.notification.application.dto.NotificationCreateCommandDto;
import com.bubbletea.notification.service.NotificationCreateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventMessageHandlerExceptionTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private NotificationCreateService notificationCreateService;

  @Nested
  @DisplayName("[EXCEPTION] Kafka 알림 이벤트 처리")
  class HandleEventException {

    @Test
    @DisplayName("JSON 파싱에 실패하면 알림을 생성하지 않는다")
    void malformedJson() throws JsonProcessingException {
      given(objectMapper.readValue("invalid-json", TestEvent.class))
          .willThrow(new JsonProcessingException("invalid json") {
          });
      NotificationEventMessageHandler handler =
          new NotificationEventMessageHandler(objectMapper, notificationCreateService);

      handler.handle("invalid-json", TestEvent.class, event -> validCommand());

      then(notificationCreateService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("필수 값이 없는 명령이면 알림을 생성하지 않는다")
    void invalidCommand() throws JsonProcessingException {
      TestEvent event = new TestEvent(null, 1L);
      given(objectMapper.readValue("{}", TestEvent.class)).willReturn(event);
      NotificationEventMessageHandler handler =
          new NotificationEventMessageHandler(objectMapper, notificationCreateService);

      handler.handle("{}", TestEvent.class, ignored -> new NotificationCreateCommandDto(
          null,
          1L,
          "MEMBER_SIGNED_UP",
          Map.of(),
          null,
          Map.of()
      ));

      then(notificationCreateService).shouldHaveNoInteractions();
    }
  }

  private NotificationCreateCommandDto validCommand() {
    return new NotificationCreateCommandDto(
        "event-1",
        1L,
        "MEMBER_SIGNED_UP",
        Map.of(),
        null,
        Map.of()
    );
  }

  private record TestEvent(String eventId, Long receiverId) {
  }
}
