package com.bubbletea.chat.application.dto;

import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.enums.ChatRoomStatus;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;

public record ChatRoomResponseDto(
    Long roomId,
    Long artistId,
    ChatRoomStatus status,
    Long lastReadId,
    ParticipantRole role,
    ParticipantStatus participantStatus
) {

  public static ChatRoomResponseDto from(ChatRoom room, ChatParticipant participant) {
    return new ChatRoomResponseDto(
        room.getId(),
        room.getArtistId(),
        room.getStatus(),
        participant.getLastReadId(),
        participant.getRole(),
        participant.getStatus()
    );
  }
}
