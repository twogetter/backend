package com.bubbletea.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
    name = "notifications",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_notifications_event_receiver",
            columnNames = {"event_id", "receiver_id"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "receiver_id", nullable = false)
  private Long receiverId;

  @Column(name = "event_id", nullable = false, columnDefinition = "TEXT")
  private String eventId;

  @Column(name = "notification_type", nullable = false, length = 100)
  private String notificationType;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "contents", nullable = false, columnDefinition = "TEXT")
  private String contents;

  @Column(name = "link_url", columnDefinition = "TEXT")
  private String linkUrl;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload = new LinkedHashMap<>();

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Builder
  private Notification(
      Long receiverId,
      String eventId,
      String notificationType,
      String title,
      String contents,
      String linkUrl,
      Map<String, Object> payload
  ) {
    this.receiverId = receiverId;
    this.eventId = eventId;
    this.notificationType = notificationType;
    this.title = title;
    this.contents = contents;
    this.linkUrl = linkUrl;
    this.payload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    this.createdAt = LocalDateTime.now();
  }

  public void read(LocalDateTime readAt) {
    if (this.readAt == null) {
      this.readAt = readAt;
    }
  }
}
