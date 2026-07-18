package com.bubbletea.chat.application.service.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.common.exception.AppException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiveParticipantValidatorTest {

  @Mock
  private ChatParticipantRepository chatParticipantRepository;

  @InjectMocks
  private ActiveParticipantValidator activeParticipantValidator;

  @Test
  @DisplayName("참여 정보가 있고 ACTIVE면 검증을 통과한다")
  void validate_Success() {
    // given
    Long roomId = 1L;
    Long senderId = 2L;
    ChatParticipant participant = ChatParticipant.builder()
        .roomId(roomId)
        .userId(senderId)
        .role(ParticipantRole.FAN)
        .status(ParticipantStatus.ACTIVE)
        .build();

    when(chatParticipantRepository.findByRoomIdAndUserIdForUpdate(roomId, senderId))
        .thenReturn(Optional.of(participant));

    // when & then
    assertThatCode(() -> activeParticipantValidator.validate(roomId, senderId))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("참여 정보가 없으면 PARTICIPANT_NOT_FOUND 에러를 던진다")
  void validate_NotFound() {
    // given
    Long roomId = 1L;
    Long senderId = 2L;

    when(chatParticipantRepository.findByRoomIdAndUserIdForUpdate(roomId, senderId))
        .thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> activeParticipantValidator.validate(roomId, senderId))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.PARTICIPANT_NOT_FOUND);
  }

  @Test
  @DisplayName("INACTIVE 상태면 PARTICIPANT_NOT_FOUND 에러를 던진다")
  void validate_Inactive() {
    // given
    Long roomId = 1L;
    Long senderId = 2L;
    ChatParticipant participant = ChatParticipant.builder()
        .roomId(roomId)
        .userId(senderId)
        .role(ParticipantRole.FAN)
        .status(ParticipantStatus.INACTIVE)
        .build();

    when(chatParticipantRepository.findByRoomIdAndUserIdForUpdate(roomId, senderId))
        .thenReturn(Optional.of(participant));

    // when & then
    assertThatThrownBy(() -> activeParticipantValidator.validate(roomId, senderId))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.PARTICIPANT_NOT_FOUND);
  }
}
