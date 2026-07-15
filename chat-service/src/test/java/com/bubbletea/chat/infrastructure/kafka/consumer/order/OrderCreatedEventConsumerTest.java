package com.bubbletea.chat.infrastructure.kafka.consumer.order;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bubbletea.chat.application.service.ChatParticipantService;
import com.bubbletea.chat.application.service.ChatRoomService;
import com.bubbletea.chat.domain.entity.ChatRoom;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderCreatedEventConsumerTest {

  @Spy
  private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Mock
  private ChatRoomService chatRoomService;

  @Mock
  private ChatParticipantService chatParticipantService;

  @InjectMocks
  private OrderCreatedEventConsumer orderCreatedEventConsumer;

  @Test
  @DisplayName("구독 생성 이벤트 수신시 해당 아티스트의 채팅방에 팬을 참여시킨다")
  void consume_OrderCreatedEvent() throws Exception {
    // given
    Long fanId = 2L;
    Long artistId = 1L;
    LocalDateTime startedAt = LocalDateTime.now();
    OrderCreatedEvent event = new OrderCreatedEvent(fanId, artistId, startedAt);
    String message = objectMapper.writeValueAsString(event);

    ChatRoom chatRoom = ChatRoom.create(artistId);
    ReflectionTestUtils.setField(chatRoom, "id", 100L);

    when(chatRoomService.getChatRoomByArtistId(artistId)).thenReturn(chatRoom);
    when(chatParticipantService.save(100L, fanId)).thenReturn(500L);

    // when
    orderCreatedEventConsumer.consume(message);

    // then
    verify(chatRoomService, times(1)).getChatRoomByArtistId(artistId);
    verify(chatParticipantService, times(1)).save(100L, fanId);
  }
}
