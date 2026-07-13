package com.bubbletea.chat.domain.entity;

import com.bubbletea.chat.domain.enums.ChatRoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "chat_rooms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_chat_rooms_artist_id", columnNames = "artist_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatRoom extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "artist_id", nullable = false)
  private Long artistId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ChatRoomStatus status;

  private ChatRoom(final Long artistId) {
    this.artistId = artistId;
    this.status = ChatRoomStatus.ACTIVE;
  }

  public static ChatRoom create(final Long artistId) {
    return new ChatRoom(artistId);
  }

  public void delete() {
    this.status = ChatRoomStatus.DELETED;
  }
}
