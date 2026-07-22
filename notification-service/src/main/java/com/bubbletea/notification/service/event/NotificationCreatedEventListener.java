package com.bubbletea.notification.service.event;

import com.bubbletea.notification.service.NotificationSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationCreatedEventListener {

  private final NotificationSseService notificationSseService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendNotification(NotificationCreatedEvent event) {
    notificationSseService.sendNotification(event.receiverId(), event.notification());
  }
}
