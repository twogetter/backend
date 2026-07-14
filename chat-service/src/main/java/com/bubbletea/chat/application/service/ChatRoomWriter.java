package com.bubbletea.chat.application.service;

import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomWriter {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatParticipantRepository chatParticipantRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Long create(final Long artistId) {
    ChatRoom chatRoom = chatRoomRepository.saveAndFlush(ChatRoom.create(artistId));

    ChatParticipant participant = ChatParticipant.createArtistParticipant(chatRoom.getId(),
        artistId);
    chatParticipantRepository.save(participant);

    return chatRoom.getId();
  }
}
