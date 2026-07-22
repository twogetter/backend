package com.bubbletea.chat.domain.event;

import com.bubbletea.chat.domain.entity.ChatMessage;
import com.bubbletea.chat.domain.enums.ParticipantRole;

/**
 * DB에 채팅 메시지가 성공적으로 저장되었음을 알리는 내부 도메인 이벤트입니다.
 */
public record ChatMessageSavedEvent(
    ChatMessage savedMessage,
    ParticipantRole role,
    String nickname
) {
}
