package com.bubbletea.chat.application.service;

import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.entity.ChatPeriod;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.domain.repository.ChatPeriodRepository;
import com.bubbletea.common.exception.AppException;
import java.time.LocalDateTime;
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
  private final ChatPeriodRepository chatPeriodRepository;

  @Transactional
  public Long save(Long roomId, Long fanId, LocalDateTime startedAt) {
    Long participantId = chatParticipantRepository.findByRoomIdAndUserId(roomId, fanId)
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

    ChatPeriod chatPeriod = ChatPeriod.create(roomId, fanId, startedAt);
    chatPeriodRepository.save(chatPeriod);
    log.info("구독 이력이 생성되었습니다. roomId={}, fanId={}, startedAt={}", roomId, fanId, startedAt);

    return participantId;
  }

  @Transactional
  public void delete(Long roomId, Long fanId, LocalDateTime endedAt) {
    ChatPeriod chatPeriod = chatPeriodRepository
        .findTopByRoomIdAndFanIdAndStatusAndStartedAtLessThanEqualOrderByStartedAtDesc(
            roomId, fanId, ParticipantStatus.ACTIVE, endedAt)
        .orElse(null);

    if (chatPeriod == null) {
      log.warn("유효하지 않거나 지연 수신된 구독 종료 이벤트입니다. roomId={}, fanId={}, endedAt={}", roomId, fanId, endedAt);
      return;
    }

    chatPeriod.deactivate(endedAt);
    log.info("구독 이력이 종료되었습니다. roomId={}, fanId={}, endedAt={}", roomId, fanId, endedAt);

    chatParticipantRepository.findByRoomIdAndUserId(roomId, fanId)
        .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
        .ifPresent(participant -> {
          participant.deactivate();
          log.info("팬이 퇴장하였습니다. roomId={}, fanId={}", roomId, fanId);
        });
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

