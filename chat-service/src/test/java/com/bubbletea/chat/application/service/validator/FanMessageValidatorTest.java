package com.bubbletea.chat.application.service.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatMessageRepository;
import com.bubbletea.common.exception.AppException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FanMessageValidatorTest {

  @Mock
  private ChatMessageRepository chatMessageRepository;

  @InjectMocks
  private FanMessageValidator fanMessageValidator;

  @Test
  @DisplayName("팬 전송 5번까지는 검증을 통과한다")
  void validate_AllowFifthMessage() {
    // given
    Long roomId = 1L;
    Long senderId = 2L;

    when(chatMessageRepository.countByRoomIdAndSenderIdAndCreatedAtGreaterThanEqual(eq(roomId), eq(senderId),
        any(LocalDateTime.class)))
        .thenReturn(4L);

    // when & then
    assertThatCode(() -> fanMessageValidator.validate(roomId, senderId, ParticipantRole.FAN))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("5회 전송 초과 시 EXCEEDED_DAILY_LIMIT 에러를 던진다")
  void validate_DenySixthMessage() {
    // given
    Long roomId = 1L;
    Long senderId = 2L;

    when(chatMessageRepository.countByRoomIdAndSenderIdAndCreatedAtGreaterThanEqual(eq(roomId), eq(senderId),
        any(LocalDateTime.class)))
        .thenReturn(5L);

    // when & then
    assertThatThrownBy(() -> fanMessageValidator.validate(roomId, senderId, ParticipantRole.FAN))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.EXCEEDED_DAILY_LIMIT);
  }
}
