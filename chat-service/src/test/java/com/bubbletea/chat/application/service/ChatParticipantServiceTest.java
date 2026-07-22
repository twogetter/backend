package com.bubbletea.chat.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    ChatParticipant mockParticipant = ChatParticipant.createFanParticipant(roomId, fanId);
    when(chatParticipantRepository.findByRoomIdAndUserId(roomId, fanId)).thenReturn(
        Optional.of(mockParticipant));

    ChatPeriod mockPeriod = ChatPeriod.builder()
        .roomId(roomId)
        .fanId(fanId)
        .startedAt(LocalDateTime.now().minusDays(1))
        .status(ParticipantStatus.ACTIVE)
        .build();
    when(
        chatPeriodRepository.findTopByRoomIdAndFanIdOrderByStartedAtDesc(roomId, fanId)).thenReturn(
        Optional.of(mockPeriod));

    // When
    chatParticipantService.delete(roomId, fanId, endedAt);

    // Then
    assertThat(mockParticipant.getStatus()).isEqualTo(ParticipantStatus.INACTIVE);
    assertThat(mockPeriod.getStatus()).isEqualTo(ParticipantStatus.INACTIVE);
    assertThat(mockPeriod.getEndedAt()).isEqualTo(endedAt);
  }
}
