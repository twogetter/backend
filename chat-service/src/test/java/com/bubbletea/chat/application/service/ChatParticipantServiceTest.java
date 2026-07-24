package com.bubbletea.chat.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.entity.ChatPeriod;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.domain.repository.ChatPeriodRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatParticipantServiceTest {

  @Mock
  private ChatParticipantRepository chatParticipantRepository;

  @Mock
  private ChatPeriodRepository chatPeriodRepository;

  @InjectMocks
  private ChatParticipantService chatParticipantService;

  @Test
  @DisplayName("팬이 신규 가입 시 참여자가 활성화되고 새로운 구독 기간이 저장된다")
  void save_NewParticipant_ShouldCreateChatPeriod() {
    // Given
    Long roomId = 1L;
    Long fanId = 2L;
    LocalDateTime startedAt = LocalDateTime.now();

    when(chatParticipantRepository.findByRoomIdAndUserId(roomId, fanId)).thenReturn(
        Optional.empty());

    ChatParticipant mockParticipant = ChatParticipant.createFanParticipant(roomId, fanId);
    ReflectionTestUtils.setField(mockParticipant, "id", 100L);
    when(chatParticipantRepository.save(any(ChatParticipant.class))).thenReturn(mockParticipant);

    // When
    Long participantId = chatParticipantService.save(roomId, fanId, startedAt);

    // Then
    assertThat(participantId).isEqualTo(100L);

    ArgumentCaptor<ChatPeriod> periodCaptor = ArgumentCaptor.forClass(ChatPeriod.class);
    verify(chatPeriodRepository, times(1)).save(periodCaptor.capture());

    ChatPeriod savedPeriod = periodCaptor.getValue();
    assertThat(savedPeriod.getRoomId()).isEqualTo(roomId);
    assertThat(savedPeriod.getFanId()).isEqualTo(fanId);
    assertThat(savedPeriod.getStartedAt()).isEqualTo(startedAt);
    assertThat(savedPeriod.getEndedAt()).isNull();
    assertThat(savedPeriod.getStatus()).isEqualTo(ParticipantStatus.ACTIVE);
  }

  @Test
  @DisplayName("팬이 구독을 종료하면 참여자가 비활성화되고 해당 구독 기간이 만료 처리된다")
  void delete_Participant_ShouldExpireChatPeriod() {
    // Given
    Long roomId = 1L;
    Long fanId = 2L;
    LocalDateTime endedAt = LocalDateTime.now();
    LocalDateTime startedAt = endedAt.minusDays(1);

    ChatParticipant mockParticipant = ChatParticipant.createFanParticipant(roomId, fanId);
    when(chatParticipantRepository.findByRoomIdAndUserId(roomId, fanId)).thenReturn(
        Optional.of(mockParticipant));

    ChatPeriod mockPeriod = ChatPeriod.builder()
        .roomId(roomId)
        .fanId(fanId)
        .startedAt(startedAt)
        .status(ParticipantStatus.ACTIVE)
        .build();

    when(
        chatPeriodRepository.findTopByRoomIdAndFanIdAndStatusAndStartedAtLessThanEqualOrderByStartedAtDesc(
            roomId, fanId, ParticipantStatus.ACTIVE, endedAt)).thenReturn(Optional.of(mockPeriod));

    // When
    chatParticipantService.delete(roomId, fanId, endedAt);

    // Then
    assertThat(mockParticipant.getStatus()).isEqualTo(ParticipantStatus.INACTIVE);
    assertThat(mockPeriod.getStatus()).isEqualTo(ParticipantStatus.INACTIVE);
    assertThat(mockPeriod.getEndedAt()).isEqualTo(endedAt);
  }

  @Test
  @DisplayName("재구독 이후 뒤늦게 수신된 이전 구독 종료 이벤트는 현재 활성 상태를 변경하지 않고 무시된다")
  void delete_StaleExpiredEvent_ShouldIgnoreAndKeepActive() {
    // Given
    Long roomId = 1L;
    Long fanId = 2L;

    LocalDateTime staleEndedAt = LocalDateTime.now().minusHours(1);

    when(
        chatPeriodRepository.findTopByRoomIdAndFanIdAndStatusAndStartedAtLessThanEqualOrderByStartedAtDesc(
            roomId, fanId, ParticipantStatus.ACTIVE, staleEndedAt)).thenReturn(Optional.empty());

    // When
    chatParticipantService.delete(roomId, fanId, staleEndedAt);

    // Then
    verify(chatParticipantRepository, never()).findByRoomIdAndUserId(anyLong(), anyLong());
  }

  @Test
  @DisplayName("이미 활성화된 팬이 다시 입장하려고 하면 예외가 발생한다")
  void save_DuplicateParticipant_ThrowsException() {
    // Given
    Long roomId = 1L;
    Long fanId = 2L;
    LocalDateTime startedAt = LocalDateTime.now();

    ChatParticipant activeParticipant = ChatParticipant.createFanParticipant(roomId, fanId);
    when(chatParticipantRepository.findByRoomIdAndUserId(roomId, fanId))
        .thenReturn(Optional.of(activeParticipant));

    // When & Then
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> chatParticipantService.save(roomId, fanId, startedAt))
        .isInstanceOf(com.bubbletea.common.exception.AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", com.bubbletea.chat.domain.exception.ChatErrorCode.DUPLICATE_PARTICIPANT);
  }

  @Test
  @DisplayName("비활성화된 팬이 다시 입장하면 활성화되고 구독 이력이 새로 생성된다")
  void save_InactiveParticipant_ShouldReactivate() {
    // Given
    Long roomId = 1L;
    Long fanId = 2L;
    LocalDateTime startedAt = LocalDateTime.now();

    ChatParticipant inactiveParticipant = ChatParticipant.createFanParticipant(roomId, fanId);
    inactiveParticipant.deactivate();
    ReflectionTestUtils.setField(inactiveParticipant, "id", 200L);

    when(chatParticipantRepository.findByRoomIdAndUserId(roomId, fanId))
        .thenReturn(Optional.of(inactiveParticipant));

    // When
    Long participantId = chatParticipantService.save(roomId, fanId, startedAt);

    // Then
    assertThat(participantId).isEqualTo(200L);
    assertThat(inactiveParticipant.getStatus()).isEqualTo(ParticipantStatus.ACTIVE);

    ArgumentCaptor<ChatPeriod> periodCaptor = ArgumentCaptor.forClass(ChatPeriod.class);
    verify(chatPeriodRepository, times(1)).save(periodCaptor.capture());
    assertThat(periodCaptor.getValue().getStartedAt()).isEqualTo(startedAt);
  }

  @Test
  @DisplayName("참여자의 마지막 읽은 메시지 ID를 정상적으로 업데이트한다")
  void updateLastReadId_Success() {
    // Given
    Long roomId = 1L;
    Long fanId = 2L;
    Long lastReadId = 50L;

    ChatParticipant activeParticipant = ChatParticipant.createFanParticipant(roomId, fanId);
    when(chatParticipantRepository.findByRoomIdAndUserId(roomId, fanId))
        .thenReturn(Optional.of(activeParticipant));

    // When
    chatParticipantService.updateLastReadId(roomId, fanId, lastReadId);

    // Then
    assertThat(activeParticipant.getLastReadId()).isEqualTo(lastReadId);
  }

  @Test
  @DisplayName("참여자가 존재하지 않거나 비활성화 상태에서 업데이트 시도 시 예외가 발생한다")
  void updateLastReadId_ParticipantNotFound() {
    // Given
    Long roomId = 1L;
    Long fanId = 2L;
    Long lastReadId = 50L;

    when(chatParticipantRepository.findByRoomIdAndUserId(roomId, fanId))
        .thenReturn(Optional.empty());

    // When & Then
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> chatParticipantService.updateLastReadId(roomId, fanId, lastReadId))
        .isInstanceOf(com.bubbletea.common.exception.AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", com.bubbletea.chat.domain.exception.ChatErrorCode.PARTICIPANT_NOT_FOUND);
  }
}
