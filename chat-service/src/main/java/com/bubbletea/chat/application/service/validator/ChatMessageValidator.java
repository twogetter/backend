package com.bubbletea.chat.application.service.validator;

public interface ChatMessageValidator {

  void validate(Long roomId, Long senderId);
}
