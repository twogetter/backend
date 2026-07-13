package com.bubbletea.chat.application.service;

import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomWriter {

  private final ChatRoomRepository chatRoomRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Long create(final Long artistId) {
    return chatRoomRepository.saveAndFlush(ChatRoom.create(artistId)).getId();
  }
}
