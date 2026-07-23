package com.bubbletea.chat.presentation.controller;

import com.bubbletea.chat.domain.enums.MessageType;
import com.bubbletea.chat.domain.event.ChatPublishedEvent;
import com.bubbletea.chat.infrastructure.kafka.producer.ChatEventPublisher;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats/mock-events")
@RequiredArgsConstructor
public class MockChatEventController {

  private final ChatEventPublisher chatEventPublisher;

  @PostMapping("/published")
  public ResponseEntity<Void> publishChatEvent() {

    String uniqueEventId = "chat:CHAT_PUBLISHED:mock:" + java.util.UUID.randomUUID();

    ChatPublishedEvent event = new ChatPublishedEvent(
        uniqueEventId,
        1L,
        "아이돌",
        "안녕하세요",
        MessageType.TEXT,
        LocalDateTime.now()
    );

    chatEventPublisher.publish(event);

    return ResponseEntity.ok().build();
  }
}
