package com.bubbletea.chat.domain.repository;

import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.enums.ChatRoomStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  Optional<ChatRoom> findByArtistId(final Long artistId);

  List<ChatRoom> findAllByIdInAndStatus(List<Long> ids, ChatRoomStatus status);
}
