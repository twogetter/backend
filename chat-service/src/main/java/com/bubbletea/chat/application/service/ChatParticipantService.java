package com.bubbletea.chat.application.service;

import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatParticipantService {

  private final ChatParticipantRepository chatParticipantRepository;

  @Transactional
  public Long save(Long roomId, Long fanId) {
    return chatParticipantRepository.findByRoomIdAndUserId(roomId, fanId)
        .map(participant -> {
          if (participant.getStatus() == ParticipantStatus.ACTIVE) {
            throw new AppException(ChatErrorCode.DUPLICATE_PARTICIPANT);
          }
          participant.activate();
          log.info("기존 팬이 재입장합니다. roomId={}, fanId={}", roomId, fanId);
          return participant.getId();
        })
        .orElseGet(() -> {
          ChatParticipant participant = ChatParticipant.createFanParticipant(roomId, fanId);
          log.info("신규 팬이 입장합니다. roomId={}, fanId={}", roomId, fanId);
          return chatParticipantRepository.save(participant).getId();
        });
  }

  @Transactional
  public void delete(Long roomId, Long userId) {
    ChatParticipant participant = chatParticipantRepository.findByRoomIdAndUserId(roomId, userId)
        .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
        .orElseThrow(() -> new AppException(ChatErrorCode.PARTICIPANT_NOT_FOUND));

    participant.deactivate();
    log.info("팬이 퇴장하였습니다. roomId={}, userId={}", roomId, userId);
  }

  @Transactional
  public void updateLastReadId(Long roomId, Long userId, Long lastReadId) {
    ChatParticipant participant = getParticipantByRoomIdAndUserId(roomId, userId);
    participant.updateLastReadId(lastReadId);
  }

  private ChatParticipant getParticipantByRoomIdAndUserId(Long roomId, Long userId) {
    return chatParticipantRepository.findByRoomIdAndUserId(roomId, userId)
        .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
        .orElseThrow(() -> new AppException(ChatErrorCode.PARTICIPANT_NOT_FOUND));
  }
}

