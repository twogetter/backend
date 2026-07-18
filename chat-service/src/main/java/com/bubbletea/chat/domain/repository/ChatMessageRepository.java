package com.bubbletea.chat.domain.repository;

import com.bubbletea.chat.domain.entity.ChatMessage;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  long countBySenderIdAndCreatedAtGreaterThanEqual(Long senderId, LocalDateTime since);
}
