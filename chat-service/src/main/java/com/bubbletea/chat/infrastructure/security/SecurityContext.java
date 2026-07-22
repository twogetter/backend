package com.bubbletea.chat.infrastructure.security;

import com.bubbletea.chat.domain.enums.ParticipantRole;

public record SecurityContext(
    Long userId,
    ParticipantRole role,
    String nickname
) {

}
