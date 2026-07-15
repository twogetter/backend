package com.bubbletea.notification.service;

import com.bubbletea.notification.application.dto.NotificationCreateCommandDto;
import com.bubbletea.notification.entity.Notification;
import com.bubbletea.notification.entity.NotificationTemplate;
import com.bubbletea.notification.exception.NotificationErrorCode;
import com.bubbletea.notification.exception.NotificationException;
import com.bubbletea.notification.repository.NotificationRepository;
import com.bubbletea.notification.repository.NotificationTemplateRepository;
import com.bubbletea.notification.service.dto.NotificationResponseDto;
import com.bubbletea.notification.service.event.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCreateService {

  private static final String EVENT_RECEIVER_UNIQUE_CONSTRAINT = "uk_notifications_event_receiver";

  private final NotificationRepository notificationRepository;
  private final NotificationTemplateRepository notificationTemplateRepository;
  private final NotificationTemplateRenderer notificationTemplateRenderer;
  private final ApplicationEventPublisher eventPublisher;
  private final TransactionOperations transactionOperations;

  public void create(NotificationCreateCommandDto command) {
    try {
      transactionOperations.executeWithoutResult(ignored -> createInTransaction(command));
    } catch (DataIntegrityViolationException exception) {
      if (isEventReceiverUniqueConstraintViolation(exception)) {
        log.info(
            "중복 알림 이벤트 저장 건너뜀. eventId={}, receiverId={}",
            command.eventId(),
            command.receiverId()
        );
        return;
      }
      throw exception;
    }
  }

  private void createInTransaction(NotificationCreateCommandDto command) {
    if (notificationRepository.existsByEventIdAndReceiverId(command.eventId(), command.receiverId())) {
      log.info(
          "중복 알림 이벤트 건너뜀. eventId={}, receiverId={}",
          command.eventId(),
          command.receiverId()
      );
      return;
    }

    NotificationTemplate template = notificationTemplateRepository
        .findByNotificationType(command.notificationType())
        .orElseThrow(() -> new NotificationException(
            NotificationErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND
        ));

    Notification notification = notificationRepository.saveAndFlush(Notification.builder()
        .receiverId(command.receiverId())
        .eventId(command.eventId())
        .notificationType(command.notificationType())
        .title(notificationTemplateRenderer.render(
            template.getTitleTemplate(),
            command.variables()
        ))
        .contents(notificationTemplateRenderer.render(
            template.getContentsTemplate(),
            command.variables()
        ))
        .linkUrl(command.linkUrl())
        .payload(command.payload())
        .build());

    eventPublisher.publishEvent(new NotificationCreatedEvent(
        notification.getReceiverId(),
        NotificationResponseDto.from(notification)
    ));
  }

  private boolean isEventReceiverUniqueConstraintViolation(DataIntegrityViolationException exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof ConstraintViolationException constraintViolationException) {
        return EVENT_RECEIVER_UNIQUE_CONSTRAINT.equals(
            constraintViolationException.getConstraintName()
        );
      }
      cause = cause.getCause();
    }
    return false;
  }
}
