package com.bubbletea.chat.domain.repository;

import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select cp from ChatParticipant cp where cp.roomId = :roomId and cp.userId = :userId")
  Optional<ChatParticipant> findByRoomIdAndUserIdForUpdate(@Param("roomId") Long roomId, @Param("userId") Long userId);

  Optional<ChatParticipant> findByRoomIdAndUserId(Long roomId, Long userId);

  boolean existsByRoomIdAndUserId(Long roomId, Long userId);

  List<ChatParticipant> findAllByRoomId(Long roomId);

  List<ChatParticipant> findAllByRoomIdAndStatus(Long roomId, ParticipantStatus status);

  List<ChatParticipant> findAllByUserIdAndStatus(Long userId, ParticipantStatus status);

  List<ChatParticipant> findAllByUserIdAndRoleAndStatus(Long userId, ParticipantRole role, ParticipantStatus status);
}
