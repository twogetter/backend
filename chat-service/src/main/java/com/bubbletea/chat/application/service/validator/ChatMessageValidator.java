package com.bubbletea.chat.application.service.validator;

import com.bubbletea.chat.domain.enums.ParticipantRole;

public interface ChatMessageValidator {

  void validate(Long roomId, Long senderId, ParticipantRole role);
}
