package com.bubbletea.chat.domain.repository;

import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

  Optional<ChatParticipant> findByRoomIdAndUserId(Long roomId, Long userId);

  boolean existsByRoomIdAndUserId(Long roomId, Long userId);

  List<ChatParticipant> findAllByRoomId(Long roomId);

  List<ChatParticipant> findAllByRoomIdAndStatus(Long roomId, ParticipantStatus status);

  List<ChatParticipant> findAllByUserIdAndStatus(Long userId, ParticipantStatus status);

  List<ChatParticipant> findAllByUserIdAndRoleAndStatus(Long userId, ParticipantRole role, ParticipantStatus status);
}
