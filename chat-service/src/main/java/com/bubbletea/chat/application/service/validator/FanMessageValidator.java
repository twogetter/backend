package com.bubbletea.chat.application.service.validator;

import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatMessageRepository;
import com.bubbletea.common.exception.AppException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class FanMessageValidator implements ChatMessageValidator {

  private final ChatMessageRepository chatMessageRepository;

  public FanMessageValidator(ChatMessageRepository chatMessageRepository) {
    this.chatMessageRepository = chatMessageRepository;
  }

  // 버블 채팅 의미를 살리기 위해 일일 전송 횟수를 5회로 제한
  @Override
  public void validate(Long roomId, Long senderId, ParticipantRole role) {
    LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
    long messageCount = chatMessageRepository.countByRoomIdAndSenderIdAndCreatedAtGreaterThanEqual(roomId, senderId,
        startOfToday);
    if (messageCount >= 5) {
      throw new AppException(ChatErrorCode.EXCEEDED_DAILY_LIMIT);
    }
  }
}
