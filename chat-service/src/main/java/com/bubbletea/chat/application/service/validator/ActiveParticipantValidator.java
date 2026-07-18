package com.bubbletea.chat.application.service.validator;

import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveParticipantValidator implements ChatMessageValidator {

  private final ChatParticipantRepository chatParticipantRepository;

  @Override
  public void validate(Long roomId, Long senderId) {
    ChatParticipant participant = chatParticipantRepository.findByRoomIdAndUserIdForUpdate(roomId, senderId)
        .orElseThrow(() -> new AppException(ChatErrorCode.PARTICIPANT_NOT_FOUND));

    if (participant.getStatus() != ParticipantStatus.ACTIVE) {
      throw new AppException(ChatErrorCode.PARTICIPANT_NOT_FOUND);
    }
  }
}
