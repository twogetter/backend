package com.bubbletea.notification.service;

import com.bubbletea.notification.entity.Notification;
import com.bubbletea.notification.exception.NotificationErrorCode;
import com.bubbletea.notification.exception.NotificationException;
import com.bubbletea.notification.repository.NotificationRepository;
import com.bubbletea.notification.service.dto.NotificationListResponseDto;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

  private static final int MAX_PAGE_SIZE = 100;

  private final NotificationRepository notificationRepository;

  public NotificationListResponseDto getNotifications(Long receiverId, int page, int size) {
    validateReceiverId(receiverId);
    validatePageRequest(page, size);

    PageRequest pageRequest = PageRequest.of(
        page,
        size,
        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
    );

    return NotificationListResponseDto.from(
        notificationRepository.findAllByReceiverId(receiverId, pageRequest)
    );
  }

  @Transactional
  public void readNotification(Long notificationId, Long receiverId) {
    validateReceiverId(receiverId);

    Notification notification = notificationRepository.findByIdAndReceiverId(notificationId,
            receiverId)
        .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

    notification.read(LocalDateTime.now());
  }

  private void validateReceiverId(Long receiverId) {
    if (receiverId == null || receiverId < 1) {
      throw new NotificationException(NotificationErrorCode.INVALID_RECEIVER_ID);
    }
  }

  private void validatePageRequest(int page, int size) {
    if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
      throw new NotificationException(NotificationErrorCode.INVALID_PAGE_REQUEST);
    }
  }
}
