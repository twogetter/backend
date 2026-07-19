package com.bubbletea.chat.domain.repository;

import com.bubbletea.chat.domain.entity.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  long countByRoomIdAndSenderIdAndCreatedAtGreaterThanEqual(Long roomId, Long senderId,
      LocalDateTime since);

  // ARTIST: 첫 페이지 (커서 없음)
  List<ChatMessage> findByRoomIdOrderByIdDesc(Long roomId, Pageable pageable);

  // ARTIST: 이전 페이지 (커서 있음)
  List<ChatMessage> findByRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long cursorId,
      Pageable pageable);

  // FAN: 아티스트 메시지 + 본인 메시지 조회 (커서 없음)
  @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId " +
      "AND (m.senderType = 'ARTIST' OR (m.senderType = 'FAN' AND m.senderId = :userId)) " +
      "ORDER BY m.id DESC")
  List<ChatMessage> findFanMessages(
      @Param("roomId") Long roomId,
      @Param("userId") Long userId,
      Pageable pageable
  );

  // FAN: 아티스트 메시지 + 본인 메시지 조회 (커서 있음)
  @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId AND m.id < :cursorId " +
      "AND (m.senderType = 'ARTIST' OR (m.senderType = 'FAN' AND m.senderId = :userId)) " +
      "ORDER BY m.id DESC")
  List<ChatMessage> findFanMessagesWithCursor(
      @Param("roomId") Long roomId,
      @Param("userId") Long userId,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );
}

