package com.bubbletea.notification.kafka.consumer;

import com.bubbletea.notification.kafka.config.KafkaTopicConfig;
import com.bubbletea.notification.kafka.dto.ProductActivationScheduledEventDto;
import com.bubbletea.notification.kafka.dto.ProductDeactivationScheduledEventDto;
import com.bubbletea.notification.kafka.dto.ProductDeletionScheduledEventDto;
import com.bubbletea.notification.kafka.dto.ProductOpenScheduledEventDto;
import com.bubbletea.notification.kafka.dto.ProductPriceChangeScheduledEventDto;
import com.bubbletea.notification.kafka.support.NotificationEventMessageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductNotificationEventConsumer {

  private final NotificationEventMessageHandler eventMessageHandler;

  @KafkaListener(topics = KafkaTopicConfig.PRODUCT_ACTIVATION_SCHEDULED_TOPIC)
  public void consumeProductActivationScheduled(String message) {
    eventMessageHandler.handle(message, ProductActivationScheduledEventDto.class,
        ProductActivationScheduledEventDto::toCommand);
  }

  @KafkaListener(topics = KafkaTopicConfig.PRODUCT_DELETION_SCHEDULED_TOPIC)
  public void consumeProductDeletionScheduled(String message) {
    eventMessageHandler.handle(message, ProductDeletionScheduledEventDto.class,
        ProductDeletionScheduledEventDto::toCommand);
  }

  @KafkaListener(topics = KafkaTopicConfig.PRODUCT_PRICE_CHANGE_SCHEDULED_TOPIC)
  public void consumeProductPriceChangeScheduled(String message) {
    eventMessageHandler.handle(message, ProductPriceChangeScheduledEventDto.class,
        ProductPriceChangeScheduledEventDto::toCommand);
  }

  @KafkaListener(topics = KafkaTopicConfig.PRODUCT_DEACTIVATION_SCHEDULED_TOPIC)
  public void consumeProductDeactivationScheduled(String message) {
    eventMessageHandler.handle(message, ProductDeactivationScheduledEventDto.class,
        ProductDeactivationScheduledEventDto::toCommand);
  }

  @KafkaListener(topics = KafkaTopicConfig.PRODUCT_OPEN_SCHEDULED_TOPIC)
  public void consumeProductOpenScheduled(String message) {
    eventMessageHandler.handle(message, ProductOpenScheduledEventDto.class,
        ProductOpenScheduledEventDto::toCommand);
  }
}
