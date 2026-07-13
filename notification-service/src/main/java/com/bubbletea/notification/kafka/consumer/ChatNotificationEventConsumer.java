package com.bubbletea.notification.kafka.consumer;

import com.bubbletea.notification.kafka.config.KafkaTopicConfig;
import com.bubbletea.notification.kafka.dto.ChatPublishedEventDto;
import com.bubbletea.notification.kafka.support.NotificationEventMessageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatNotificationEventConsumer {

  private final NotificationEventMessageHandler eventMessageHandler;

  @KafkaListener(topics = KafkaTopicConfig.CHAT_PUBLISHED_TOPIC)
  public void consumeChatPublished(String message) {
    eventMessageHandler.handle(message, ChatPublishedEventDto.class,
        ChatPublishedEventDto::toCommand);
  }
}
