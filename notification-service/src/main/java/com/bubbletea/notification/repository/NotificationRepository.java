package com.bubbletea.notification.repository;

import com.bubbletea.notification.entity.Notification;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  boolean existsByEventIdAndReceiverId(String eventId, Long receiverId);

  Slice<Notification> findAllByReceiverId(Long receiverId, Pageable pageable);

  Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);
}
