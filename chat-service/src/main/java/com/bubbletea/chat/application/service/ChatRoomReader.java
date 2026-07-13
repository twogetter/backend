package com.bubbletea.chat.application.service;

import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import com.bubbletea.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomReader {

  private final ChatRoomRepository chatRoomRepository;

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public Long getChatRoomId(Long artistId) {
    return chatRoomRepository.findByArtistId(artistId)
        .map(ChatRoom::getId)
        .orElseThrow(() -> new AppException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
  }
}
