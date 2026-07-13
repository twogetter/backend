package com.bubbletea.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "notification_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "title_template", nullable = false, length = 255)
  private String titleTemplate;

  @Column(name = "contents_template", nullable = false, columnDefinition = "TEXT")
  private String contentsTemplate;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "variables", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> variables = new LinkedHashMap<>();

  @Column(name = "notification_type", nullable = false, unique = true, length = 100)
  private String notificationType;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Builder
  private NotificationTemplate(
      String titleTemplate,
      String contentsTemplate,
      Map<String, Object> variables,
      String notificationType
  ) {
    this.titleTemplate = titleTemplate;
    this.contentsTemplate = contentsTemplate;
    this.variables = variables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(variables);
    this.notificationType = notificationType;

    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }
}
