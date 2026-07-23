package com.bubbletea.chat.domain.event;

import com.bubbletea.chat.domain.entity.ChatMessage;
import com.bubbletea.chat.domain.enums.ParticipantRole;

public record ChatMessageSavedEvent(
    ChatMessage savedMessage,
    ParticipantRole role,
    String nickname
) {

}
