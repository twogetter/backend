package com.bubbletea.notification.kafka.consumer;

import com.bubbletea.notification.kafka.config.KafkaTopicConfig;
import com.bubbletea.notification.kafka.dto.PaymentCompleteEventDto;
import com.bubbletea.notification.kafka.dto.PaymentFailEventDto;
import com.bubbletea.notification.kafka.support.NotificationEventMessageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentNotificationEventConsumer {

  private final NotificationEventMessageHandler eventMessageHandler;

  @KafkaListener(topics = KafkaTopicConfig.PAYMENT_COMPLETE_TOPIC)
  public void consumePaymentComplete(String message) {
    eventMessageHandler.handle(message, PaymentCompleteEventDto.class,
        PaymentCompleteEventDto::toCommand);
  }

  @KafkaListener(topics = KafkaTopicConfig.PAYMENT_FAIL_TOPIC)
  public void consumePaymentFail(String message) {
    eventMessageHandler.handle(message, PaymentFailEventDto.class, PaymentFailEventDto::toCommand);
  }
}
