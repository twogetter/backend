package com.bubbletea.chat.application.service;

import com.bubbletea.chat.application.dto.ChatRoomResponseDto;
import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.enums.ChatRoomStatus;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatMessageRepository;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import com.bubbletea.chat.domain.repository.dto.UnreadCountDto;
import com.bubbletea.common.exception.AppException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatRoomService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatParticipantRepository chatParticipantRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final ChatRoomReader chatRoomReader;
  private final ChatRoomWriter chatRoomWriter;

  @Transactional
  public Long createChatRoom(Long artistId) {
    try {
      return chatRoomWriter.create(artistId);
    } catch (DataIntegrityViolationException e) {
      final Throwable cause = e.getMostSpecificCause();
      if (cause.getMessage() != null && cause.getMessage().contains("uk_chat_rooms_artist_id")) {
        log.info("기존 채팅방을 반환합니다. artistId={}", artistId);
        return chatRoomReader.getChatRoomId(artistId);
      }
      throw e;
    }
  }

  public ChatRoom getChatRoomByArtistId(final Long artistId) {
    return chatRoomRepository.findByArtistId(artistId)
        .orElseThrow(() -> new AppException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
  }

  public List<ChatRoomResponseDto> getAll(Long userId, ParticipantRole role) {
    List<ChatParticipant> participants = chatParticipantRepository.findAllByUserIdAndRoleAndStatus(
        userId, role, ParticipantStatus.ACTIVE
    );

    if (participants.isEmpty()) {
      return List.of();
    }

    List<Long> roomIds = participants.stream()
        .map(ChatParticipant::getRoomId)
        .toList();

    List<ChatRoom> rooms = chatRoomRepository.findAllByIdInAndStatus(roomIds,
        ChatRoomStatus.ACTIVE);

    Map<Long, ChatRoom> roomMap = rooms.stream()
        .collect(Collectors.toMap(ChatRoom::getId, Function.identity()));

    // 팬(FAN)인 경우에만 방별 안읽은 아티스트 메시지 개수를 조회합니다. (아티스트는 불필요)
    Map<Long, Long> unreadCountMap = Map.of();
    if (role == ParticipantRole.FAN) {
      List<UnreadCountDto> unreadCounts = chatMessageRepository.countUnreadMessagesForFan(userId);
      unreadCountMap = unreadCounts.stream()
          .collect(Collectors.toMap(
              UnreadCountDto::getRoomId,
              UnreadCountDto::getCount
          ));
    }

    final Map<Long, Long> finalUnreadCountMap = unreadCountMap;

    return participants.stream()
        .filter(p -> roomMap.containsKey(p.getRoomId()))
        .map(p -> {
          long unreadCount = finalUnreadCountMap.getOrDefault(p.getRoomId(), 0L);
          return ChatRoomResponseDto.from(roomMap.get(p.getRoomId()), p, unreadCount);
        })
        .toList();
  }
}
