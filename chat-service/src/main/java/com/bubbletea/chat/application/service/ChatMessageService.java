package com.bubbletea.chat.application.service;

import com.bubbletea.chat.application.dto.ChatMessageResponseDto;
import com.bubbletea.chat.application.service.validator.ActiveParticipantValidator;
import com.bubbletea.chat.application.service.validator.FanMessageValidator;
import com.bubbletea.chat.domain.entity.ChatMessage;
import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.enums.ChatRoomStatus;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatMessageRepository;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import com.bubbletea.chat.presentation.controller.dto.ChatMessageCreateRequestDto;
import com.bubbletea.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatMessageService {

  private final ChatMessageRepository chatMessageRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final ActiveParticipantValidator activeParticipantValidator;
  private final FanMessageValidator fanMessageValidator;

  @Transactional
  public ChatMessageResponseDto save(
      Long roomId,
      Long senderId,
      ParticipantRole role,
      ChatMessageCreateRequestDto requestDto
  ) {
    ChatRoom room = chatRoomRepository.findById(roomId)
        .orElseThrow(() -> new AppException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    // 지워지지 않은 방인지?
    if (room.getStatus() != ChatRoomStatus.ACTIVE) {
      throw new AppException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
    }
    // ACTIVE 상태인 참여자인지?
    activeParticipantValidator.validate(roomId, senderId);
    // 팬이면 전송 5회 제한 확인
    if (role == ParticipantRole.FAN) {
      fanMessageValidator.validate(roomId, senderId);
    }

    ChatMessage message = ChatMessage.builder()
        .roomId(roomId)
        .senderId(senderId)
        .senderType(role)
        .content(requestDto.content())
        .messageType(requestDto.messageType())
        .build();

    ChatMessage savedMessage = chatMessageRepository.save(message);

    log.info("메시지 저장 성공! id={}, roomId={}, senderId={}", savedMessage.getId(), roomId, senderId);

    return ChatMessageResponseDto.from(savedMessage);
  }
}
