package com.bubbletea.chat.infrastructure.kafka.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bubbletea.chat.domain.enums.MessageType;
import com.bubbletea.chat.domain.event.ChatPublishedEvent;
import com.bubbletea.chat.infrastructure.kafka.config.ChatKafkaTopics;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
class ChatEventPublisherTest {

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks
  private ChatEventPublisher chatEventPublisher;

  @Captor
  private ArgumentCaptor<Message<ChatPublishedEvent>> messageCaptor;

  @Test
  @DisplayName("채팅 이벤트를 성공적으로 발행한다")
  void publish_Success() {
    // given
    ChatPublishedEvent event = ChatPublishedEvent.of("event-id", 1L, "prod", "msg",
        MessageType.TEXT, LocalDateTime.now());

    RecordMetadata recordMetadata = new RecordMetadata(
        new TopicPartition(ChatKafkaTopics.CHAT_PUBLISHED, 0), 0L, 0, 0L, 0, 0);
    SendResult<String, Object> sendResult = mock(SendResult.class);
    when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

    when(kafkaTemplate.send(any(Message.class)))
        .thenReturn(CompletableFuture.completedFuture(sendResult));

    // when
    chatEventPublisher.publish(event);

    // then
    verify(kafkaTemplate).send(messageCaptor.capture());
    Message<ChatPublishedEvent> capturedMessage = messageCaptor.getValue();

    assertThat(capturedMessage.getPayload()).isEqualTo(event);
    assertThat(capturedMessage.getHeaders().get(KafkaHeaders.TOPIC))
        .isEqualTo(ChatKafkaTopics.CHAT_PUBLISHED);
    assertThat(capturedMessage.getHeaders().get("domainName")).isEqualTo("chat");
  }

  @Test
  @DisplayName("채팅 이벤트 발행 실패 시 예외 처리가 동작한다")
  void publish_Fail() {
    // given
    ChatPublishedEvent event = ChatPublishedEvent.of("event-id", 1L, "prod", "msg",
        MessageType.TEXT, LocalDateTime.now());

    CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Kafka error"));

    when(kafkaTemplate.send(any(Message.class))).thenReturn(failedFuture);

    // when
    chatEventPublisher.publish(event);

    // then
    verify(kafkaTemplate).send(any(Message.class));
  }
}
