package com.bubbletea.notification.service.dto;

import com.bubbletea.notification.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Slice;

public record NotificationListResponseDto(
    @Schema(description = "알림 목록") List<NotificationResponseDto> notifications,
    @Schema(description = "현재 페이지 번호", example = "0") int page,
    @Schema(description = "페이지 크기", example = "20") int size,
    @Schema(description = "다음 페이지 존재 여부", example = "true") boolean hasNext
) {

  public static NotificationListResponseDto from(Slice<Notification> notificationSlice) {
    return new NotificationListResponseDto(
        notificationSlice.getContent().stream()
            .map(NotificationResponseDto::from)
            .toList(),
        notificationSlice.getNumber(),
        notificationSlice.getSize(),
        notificationSlice.hasNext()
    );
  }
}
