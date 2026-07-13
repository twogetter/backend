package com.bubbletea.chat.application.service;

import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import com.bubbletea.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

  private final ChatRoomRepository chatRoomRepository;

  @Transactional
  public Long createChatRoom(final Long artistId) {
    validateDuplicateArtistRoom(artistId);

    final ChatRoom chatRoom = ChatRoom.create(artistId);
    final ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

    return savedChatRoom.getId();
  }

  public ChatRoom getChatRoomByArtistId(final Long artistId) {
    return chatRoomRepository.findByArtistId(artistId)
        .orElseThrow(() -> new AppException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
  }

  private void validateDuplicateArtistRoom(final Long artistId) {
    if (chatRoomRepository.existsByArtistId(artistId)) {
      throw new AppException(ChatErrorCode.DUPLICATE_ARTIST_ROOM);
    }
  }
}
