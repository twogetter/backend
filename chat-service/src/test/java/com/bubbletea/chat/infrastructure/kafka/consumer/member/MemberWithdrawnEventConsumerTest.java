package com.bubbletea.chat.infrastructure.kafka.consumer.member;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberWithdrawnEventConsumerTest {

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private MemberWithdrawnEventConsumer memberWithdrawnEventConsumer;

  @Test
  @DisplayName("메시지를 정상적으로 수신하고 처리한다")
  void consume_Success() throws JsonProcessingException {
    // given
    String message = "{\"memberId\":1}";
    MemberWithdrawnEvent event = new MemberWithdrawnEvent("event-id", "WITHDRAWN", 1L, LocalDateTime.now());

    when(objectMapper.readValue(message, MemberWithdrawnEvent.class)).thenReturn(event);

    // when
    memberWithdrawnEventConsumer.consume(message);

    // then
    verify(objectMapper).readValue(message, MemberWithdrawnEvent.class);
  }

  @Test
  @DisplayName("메시지 역직렬화 실패 시 RuntimeException을 던진다")
  void consume_DeserializeFail() throws JsonProcessingException {
    // given
    String message = "invalid_json";
    when(objectMapper.readValue(eq(message), eq(MemberWithdrawnEvent.class)))
        .thenThrow(new JsonProcessingException("parse error") {});

    // when & then
    assertThatThrownBy(() -> memberWithdrawnEventConsumer.consume(message))
        .isInstanceOf(RuntimeException.class);
  }
}
