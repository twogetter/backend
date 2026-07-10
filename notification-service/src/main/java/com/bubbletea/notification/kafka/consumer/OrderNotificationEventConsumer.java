package com.bubbletea.notification.kafka.consumer;

import com.bubbletea.notification.kafka.config.KafkaTopicConfig;
import com.bubbletea.notification.kafka.dto.SubscribeRenewalEventDto;
import com.bubbletea.notification.kafka.support.NotificationEventMessageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderNotificationEventConsumer {

  private final NotificationEventMessageHandler eventMessageHandler;

  @KafkaListener(topics = KafkaTopicConfig.SUBSCRIBE_RENEWAL_TOPIC)
  public void consumeSubscribeRenewal(String message) {
    eventMessageHandler.handle(message, SubscribeRenewalEventDto.class,
        SubscribeRenewalEventDto::toCommand);
  }
}
