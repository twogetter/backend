package com.bubbletea.chat.domain.entity;

import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "chat_participants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_user", columnNames = {"room_id", "user_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatParticipant extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "last_read_id") // 마지막으로 읽은 메시지 id임
  private Long lastReadId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ParticipantStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private ParticipantRole role;

  @Builder
  private ChatParticipant(Long roomId, Long userId, Long lastReadId, ParticipantStatus status,
      ParticipantRole role) {
    this.roomId = roomId;
    this.userId = userId;
    this.lastReadId = lastReadId;
    this.status = status;
    this.role = role;
  }

  public static ChatParticipant createArtistParticipant(Long roomId, Long artistId) {
    return ChatParticipant.builder()
        .roomId(roomId)
        .userId(artistId)
        .lastReadId(null)
        .role(ParticipantRole.ARTIST)
        .status(ParticipantStatus.ACTIVE)
        .build();
  }

  // TODO: 이건 다음 이슈에서 사용
  public static ChatParticipant createFanParticipant(Long roomId, Long userId) {
    return ChatParticipant.builder()
        .roomId(roomId)
        .userId(userId)
        .lastReadId(null)
        .role(ParticipantRole.FAN)
        .status(ParticipantStatus.ACTIVE)
        .build();
  }

  public void updateLastReadId(Long lastReadId) {
    if (lastReadId != null && (this.lastReadId == null || lastReadId > this.lastReadId)) {
      this.lastReadId = lastReadId;
    }
  }

  public void deactivate() {
    this.status = ParticipantStatus.INACTIVE;
  }

  public void activate() {
    this.status = ParticipantStatus.ACTIVE;
  }
}
