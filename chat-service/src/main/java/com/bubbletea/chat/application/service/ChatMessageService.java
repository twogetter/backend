package com.bubbletea.chat.application.service;

import com.bubbletea.chat.application.dto.ChatMessageResponseDto;
import com.bubbletea.chat.application.service.validator.ActiveParticipantValidator;
import com.bubbletea.chat.application.service.validator.FanMessageValidator;
import com.bubbletea.chat.domain.entity.ChatMessage;
import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.enums.ChatRoomStatus;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.event.ChatPublishedEvent;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatMessageRepository;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import com.bubbletea.chat.infrastructure.security.SecurityContext;
import com.bubbletea.chat.infrastructure.security.SecurityContextHolder;
import com.bubbletea.chat.infrastructure.kafka.producer.ChatEventPublisher;
import com.bubbletea.chat.presentation.controller.dto.ChatMessageCreateRequestDto;
import com.bubbletea.common.exception.AppException;
import com.bubbletea.common.utility.KafkaEventIdConverter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatMessageService {

  private final ChatMessageRepository chatMessageRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final ChatParticipantRepository chatParticipantRepository;
  private final ActiveParticipantValidator activeParticipantValidator;
  private final FanMessageValidator fanMessageValidator;
  private final SimpMessagingTemplate messagingTemplate;
  private final ChatEventPublisher chatEventPublisher;

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
    activeParticipantValidator.validate(roomId, senderId, role);
    // 팬이면 전송 5회 제한 확인
    if (role == ParticipantRole.FAN) {
      fanMessageValidator.validate(roomId, senderId, role);
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

    ChatMessageResponseDto responseDto = ChatMessageResponseDto.from(savedMessage);

    if (role == ParticipantRole.ARTIST) {
      // 아티스트가 보낸 메시지는 팬구독 대역으로 브로드캐스트
      messagingTemplate.convertAndSend("/sub/rooms/" + roomId + "/artist", responseDto);

      List<ChatParticipant> participants = chatParticipantRepository.findAllByRoomIdAndStatus(roomId, ParticipantStatus.ACTIVE);

      String artistName = "아티스트"; // 기본값이 아티스트임
      SecurityContext context = SecurityContextHolder.getContext();
      if (context != null && context.nickname() != null) {
        artistName = context.nickname();
      }

      String eventId = KafkaEventIdConverter.convert(
          "chat", 
          "CHAT_PUBLISHED", 
          savedMessage.getId(), 
          savedMessage.getCreatedAt()
      );

      for (ChatParticipant cp : participants) {
        if (cp.getRole() == ParticipantRole.FAN) {
          chatEventPublisher.publish(ChatPublishedEvent.of(
              eventId,
              cp.getUserId(),
              artistName,
              savedMessage.getContent(),
              savedMessage.getMessageType(),
              savedMessage.getCreatedAt()
          ));
        }
      }
    } else if (role == ParticipantRole.FAN) {
      messagingTemplate.convertAndSend("/sub/rooms/" + roomId + "/fan", responseDto);
    }

    return responseDto;
  }

  @Transactional
  public List<ChatMessageResponseDto> getAll(
      Long roomId,
      Long requesterId,
      ParticipantRole role,
      Long cursorId,
      int size
  ) {
    ChatRoom room = chatRoomRepository.findById(roomId)
        .orElseThrow(() -> new AppException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

    if (room.getStatus() != ChatRoomStatus.ACTIVE) {
      throw new AppException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    activeParticipantValidator.validate(roomId, requesterId, role);

    Pageable pageable = PageRequest.of(0, size);
    List<ChatMessage> messages;

    if (role == ParticipantRole.ARTIST) {
      if (cursorId == null) {
        messages = chatMessageRepository.findByRoomIdOrderByIdDesc(roomId, pageable);
      } else {
        messages = chatMessageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(roomId, cursorId,
            pageable);
      }
    } else {
      if (cursorId == null) {
        messages = chatMessageRepository.findFanMessages(roomId, requesterId, pageable);
      } else {
        messages = chatMessageRepository.findFanMessagesWithCursor(roomId, requesterId, cursorId,
            pageable);
      }
    }

    return messages.stream()
        .map(ChatMessageResponseDto::from)
        .toList();
  }
}
