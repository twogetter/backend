package com.bubbletea.notification.repository;

import com.bubbletea.notification.entity.NotificationTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

  Optional<NotificationTemplate> findByNotificationType(String notificationType);
}
