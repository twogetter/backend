package com.bubbletea.notification.kafka.consumer;

import com.bubbletea.notification.kafka.config.KafkaTopicConfig;
import com.bubbletea.notification.kafka.dto.MemberAuthLoginEventDto;
import com.bubbletea.notification.kafka.dto.MemberDeleteAccountEventDto;
import com.bubbletea.notification.kafka.dto.MemberPasswordChangedEventDto;
import com.bubbletea.notification.kafka.dto.MemberSignedUpEventDto;
import com.bubbletea.notification.kafka.support.NotificationEventMessageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberNotificationEventConsumer {

  private final NotificationEventMessageHandler eventMessageHandler;

  @KafkaListener(topics = KafkaTopicConfig.MEMBER_SIGNED_UP_TOPIC)
  public void consumeMemberSignedUp(String message) {
    eventMessageHandler.handle(message, MemberSignedUpEventDto.class,
        MemberSignedUpEventDto::toCommand);
  }

  @KafkaListener(topics = KafkaTopicConfig.MEMBER_AUTH_LOGIN_TOPIC)
  public void consumeMemberAuthLogin(String message) {
    eventMessageHandler.handle(message, MemberAuthLoginEventDto.class,
        MemberAuthLoginEventDto::toCommand);
  }

  @KafkaListener(topics = KafkaTopicConfig.MEMBER_PASSWORD_CHANGED_TOPIC)
  public void consumeMemberPasswordChanged(String message) {
    eventMessageHandler.handle(message, MemberPasswordChangedEventDto.class,
        MemberPasswordChangedEventDto::toCommand);
  }

  @KafkaListener(topics = KafkaTopicConfig.MEMBER_DELETE_ACCOUNT_TOPIC)
  public void consumeMemberDeleteAccount(String message) {
    eventMessageHandler.handle(message, MemberDeleteAccountEventDto.class,
        MemberDeleteAccountEventDto::toCommand);
  }
}
