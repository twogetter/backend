package com.bubbletea.product.infrastructure.kafka;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
public class KafkaOutboxMessageSenderTest {

    @InjectMocks
    private KafkaOutboxMessageSender kafkaOutboxMessageSender;

    @Mock
    private KafkaTemplate<String, String> outboxKafkaTemplate;

    @Mock
    private CompletableFuture<SendResult<String, String>> mockFuture;

    @Captor
    private ArgumentCaptor<Message<String>> messageCaptor;

    private static final String TOPIC = "notification.product.productOpenScheduled";
    private static final String PAYLOAD = "{\"productName\":\"버블티 딸기\"}";
    private static final Map<String, String> HEADERS = Map.of(
        "domainName", "product",
        "idempotencyKey", "reservation-1"
    );

    @Nested
    @DisplayName("send() 테스트")
    class Send {

        @Test
        @DisplayName("성공적으로 Message를 KafkaTemplate으로 전송한다.")
        void sendsMessageWithCorrectTopicAndPayload() {
            // given
            CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(null);
            given(outboxKafkaTemplate.send(any(Message.class))).willReturn(future);

            // when
            kafkaOutboxMessageSender.send(TOPIC, PAYLOAD, HEADERS);

            // then
            then(outboxKafkaTemplate).should().send(messageCaptor.capture());
            Message<String> captured = messageCaptor.getValue();

            assertThat(captured.getPayload()).isEqualTo(PAYLOAD);
            assertThat(captured.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo(TOPIC);
        }

        @Test
        @DisplayName("headers가 Message에 포함된다.")
        void includesHeadersInMessage() {
            // given
            CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(null);
            given(outboxKafkaTemplate.send(any(Message.class))).willReturn(future);

            // when
            kafkaOutboxMessageSender.send(TOPIC, PAYLOAD, HEADERS);

            // then
            then(outboxKafkaTemplate).should().send(messageCaptor.capture());
            Message<String> captured = messageCaptor.getValue();

            assertThat(captured.getHeaders().get("domainName")).isEqualTo("product");
            assertThat(captured.getHeaders().get("idempotencyKey")).isEqualTo("reservation-1");
        }

        @Test
        @DisplayName("InterruptedException 발생 시 Thread interrupt를 복원하고 예외를 던진다.")
        void interruptedException_restoresInterruptFlagAndThrows() throws Exception {
            // given
            given(mockFuture.get(anyLong(), any(TimeUnit.class)))
                .willThrow(new InterruptedException("인터럽트 발생"));
            given(outboxKafkaTemplate.send(any(Message.class))).willReturn(mockFuture);

            // when & then
            assertThatThrownBy(() -> kafkaOutboxMessageSender.send(TOPIC, PAYLOAD, HEADERS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kafka 발행 중단")
                .hasMessageContaining(TOPIC);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            Thread.interrupted();
        }

        @Test
        @DisplayName("기타 예외 (TimeoutException) 발생 시 예외를 던진다.")
        void timeoutException_throwsIllegalStateException() throws Exception {
            // given
            given(mockFuture.get(anyLong(), any(TimeUnit.class)))
                .willThrow(new TimeoutException("타임아웃 발생"));
            given(outboxKafkaTemplate.send(any(Message.class))).willReturn(mockFuture);

            // when & then
            assertThatThrownBy(() -> kafkaOutboxMessageSender.send(TOPIC, PAYLOAD, HEADERS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kafka 발행 실패")
                .hasMessageContaining(TOPIC);
        }

        @Test
        @DisplayName("기타 예외 (ExecutionException) 발생 시 예외를 던진다.")
        void executionException_throwsIllegalStateException() throws Exception {
            // given
            given(mockFuture.get(anyLong(), any(TimeUnit.class)))
                .willThrow(new ExecutionException("발행 실패", new RuntimeException("브로커 오류")));
            given(outboxKafkaTemplate.send(any(Message.class))).willReturn(mockFuture);

            // when & then
            assertThatThrownBy(() -> kafkaOutboxMessageSender.send(TOPIC, PAYLOAD, HEADERS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kafka 발행 실패")
                .hasMessageContaining(TOPIC);
        }

    }
}





