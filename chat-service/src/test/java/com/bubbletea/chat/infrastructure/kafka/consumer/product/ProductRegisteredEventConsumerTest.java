package com.bubbletea.chat.infrastructure.kafka.consumer.product;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bubbletea.chat.application.service.ChatRoomService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductRegisteredEventConsumerTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private ChatRoomService chatRoomService;

  @InjectMocks
  private ProductRegisteredEventConsumer productRegisteredEventConsumer;

  @Test
  @DisplayName("메시지를 정상적으로 수신하면 채팅방 생성을 시도한다")
  void consume_Success() throws JsonProcessingException {
    // given
    String message = "{\"artistId\":1}";
    Long artistId = 1L;
    ProductRegisteredEvent event = new ProductRegisteredEvent(artistId, java.time.LocalDateTime.now());

    when(objectMapper.readValue(message, ProductRegisteredEvent.class)).thenReturn(event);
    when(chatRoomService.createChatRoom(artistId)).thenReturn(100L);

    // when
    productRegisteredEventConsumer.consume(message);

    // then
    verify(objectMapper).readValue(message, ProductRegisteredEvent.class);
    verify(chatRoomService).createChatRoom(artistId);
  }

  @Test
  @DisplayName("메시지 역직렬화 실패 시 RuntimeException을 던진다")
  void consume_DeserializeFail() throws JsonProcessingException {
    // given
    String message = "invalid_json";
    when(objectMapper.readValue(eq(message), eq(ProductRegisteredEvent.class)))
        .thenThrow(new JsonProcessingException("parse error") {});

    // when & then
    assertThatThrownBy(() -> productRegisteredEventConsumer.consume(message))
        .isInstanceOf(RuntimeException.class);
  }
}
