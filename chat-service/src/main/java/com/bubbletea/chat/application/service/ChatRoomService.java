package com.bubbletea.chat.application.service;

import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import com.bubbletea.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatRoomService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomReader chatRoomReader;
  private final ChatRoomWriter chatRoomWriter;

  @Transactional
  public Long createChatRoom(Long artistId) {
    try {
      return chatRoomWriter.create(artistId);
    } catch (DataIntegrityViolationException e) {
      final Throwable cause = e.getMostSpecificCause();
      if (cause.getMessage() != null && cause.getMessage().contains("uk_chat_rooms_artist_id")) {
        log.info("기존 채팅방을 반환합니다. artistId={}", artistId);
        return chatRoomReader.getChatRoomId(artistId);
      }
      throw e;
    }
  }

  public ChatRoom getChatRoomByArtistId(final Long artistId) {
    return chatRoomRepository.findByArtistId(artistId)
        .orElseThrow(() -> new AppException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
  }
}
