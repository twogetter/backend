package com.bubbletea.notification.service.dto;

import com.bubbletea.notification.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record NotificationResponseDto(
    @Schema(description = "알림 ID", example = "1") Long id,
    @Schema(description = "알림 유형", example = "NOTIFICATION_API_TEST") String notificationType,
    @Schema(description = "알림 제목", example = "새 알림이 도착했습니다.") String title,
    @Schema(description = "알림 내용", example = "알림 내용을 확인해 주세요.") String contents,
    @Schema(description = "이동할 링크", example = "/notifications/1", nullable = true) String linkUrl,
    @Schema(description = "비민감 부가 정보") Map<String, Object> payload,
    @Schema(description = "읽음 일시", example = "2026-07-14T10:30:00", nullable = true) LocalDateTime readAt,
    @Schema(description = "생성 일시", example = "2026-07-14T10:00:00") LocalDateTime createdAt
) {

  public static NotificationResponseDto from(Notification notification) {
    return new NotificationResponseDto(
        notification.getId(),
        notification.getNotificationType(),
        notification.getTitle(),
        notification.getContents(),
        notification.getLinkUrl(),
        new LinkedHashMap<>(notification.getPayload()),
        notification.getReadAt(),
        notification.getCreatedAt()
    );
  }
}
