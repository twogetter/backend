package com.bubbletea.chat.domain.repository;

import com.bubbletea.chat.domain.entity.ChatMessage;
import com.bubbletea.chat.domain.repository.dto.UnreadCountDto;
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

  // FAN: 구독 기간 내의 아티스트 메시지 + 본인 메시지 조회 (커서 없음)
  @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId " +
      "AND (m.senderType = 'ARTIST' OR (m.senderType = 'FAN' AND m.senderId = :userId)) " +
      "AND EXISTS (" +
      "  SELECT 1 FROM ChatPeriod p " +
      "  WHERE p.roomId = :roomId AND p.fanId = :userId " +
      "    AND m.createdAt >= p.startedAt " +
      "    AND (p.endedAt IS NULL OR m.createdAt <= p.endedAt)" +
      ") " +
      "ORDER BY m.id DESC")
  List<ChatMessage> findFanMessages(
      @Param("roomId") Long roomId,
      @Param("userId") Long userId,
      Pageable pageable
  );

  // FAN: 구독 기간 내의 아티스트 메시지 + 본인 메시지 조회 (커서 있음)
  @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId AND m.id < :cursorId " +
      "AND (m.senderType = 'ARTIST' OR (m.senderType = 'FAN' AND m.senderId = :userId)) " +
      "AND EXISTS (" +
      "  SELECT 1 FROM ChatPeriod p " +
      "  WHERE p.roomId = :roomId AND p.fanId = :userId " +
      "    AND m.createdAt >= p.startedAt " +
      "    AND (p.endedAt IS NULL OR m.createdAt <= p.endedAt)" +
      ") " +
      "ORDER BY m.id DESC")
  List<ChatMessage> findFanMessagesWithCursor(
      @Param("roomId") Long roomId,
      @Param("userId") Long userId,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );

  // FAN: 아티스트가 보낸 메시지 중 읽지 않은 메시지 개수 조회
  @Query("SELECT m.roomId as roomId, COUNT(m) as count " +
      "FROM ChatMessage m " +
      "JOIN ChatParticipant p ON m.roomId = p.roomId " +
      "WHERE p.userId = :userId AND p.role = 'FAN' AND p.status = 'ACTIVE' " +
      "AND m.senderType = 'ARTIST' " +
      "AND (p.lastReadId IS NULL OR m.id > p.lastReadId) " +
      "AND EXISTS (" +
      "  SELECT 1 FROM ChatPeriod cp " +
      "  WHERE cp.roomId = m.roomId AND cp.fanId = :userId " +
      "    AND m.createdAt >= cp.startedAt " +
      "    AND (cp.endedAt IS NULL OR m.createdAt <= cp.endedAt)" +
      ") " +
      "GROUP BY m.roomId")
  List<UnreadCountDto> countUnreadMessagesForFan(@Param("userId") Long userId);
}
