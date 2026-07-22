package com.bubbletea.chat.domain.entity;

import com.bubbletea.chat.domain.enums.ParticipantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_periods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatPeriod extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  @Column(name = "fan_id", nullable = false)
  private Long fanId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ParticipantStatus status;

  @Column(name = "started_at", nullable = false)
  private LocalDateTime startedAt;

  @Column(name = "ended_at")
  private LocalDateTime endedAt;

  @Builder
  private ChatPeriod(Long roomId, Long fanId, ParticipantStatus status, LocalDateTime startedAt, LocalDateTime endedAt) {
    this.roomId = roomId;
    this.fanId = fanId;
    this.status = status;
    this.startedAt = startedAt;
    this.endedAt = endedAt;
  }

  public static ChatPeriod create(Long roomId, Long fanId, LocalDateTime startedAt) {
    return ChatPeriod.builder()
        .roomId(roomId)
        .fanId(fanId)
        .status(ParticipantStatus.ACTIVE)
        .startedAt(startedAt)
        .endedAt(null)
        .build();
  }

  public void deactivate(LocalDateTime endedAt) {
    this.status = ParticipantStatus.INACTIVE;
    this.endedAt = endedAt;
  }
}
