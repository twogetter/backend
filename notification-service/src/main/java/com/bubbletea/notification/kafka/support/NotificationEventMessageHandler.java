package com.bubbletea.notification.kafka.support;

import com.bubbletea.notification.application.dto.NotificationCreateCommandDto;
import com.bubbletea.notification.service.NotificationCreateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventMessageHandler {

  private final ObjectMapper objectMapper;
  private final NotificationCreateService notificationCreateService;

  public <T> void handle(
      String message,
      Class<T> eventClass,
      Function<T, NotificationCreateCommandDto> commandMapper
  ) {
    try {
      T event = objectMapper.readValue(message, eventClass);
      NotificationCreateCommandDto command = commandMapper.apply(event);

      if (!command.isValid()) {
        log.warn("잘못된 값의 알림 이벤트 전송. eventClass={}, message={}",
            eventClass.getSimpleName(), message);
        return;
      }

      notificationCreateService.create(command);

      log.info(
          "Notification command 생성 완료. eventId={}, receiverId={}, notificationType={}",
          command.eventId(),
          command.receiverId(),
          command.notificationType()
      );
    } catch (JsonProcessingException exception) {
      log.warn("알림 이벤트 파싱 실패. eventClass={}, message={}",
          eventClass.getSimpleName(), message, exception);
    }
  }
}
