package com.bubbletea.chat.infrastructure.security;

import com.bubbletea.chat.domain.enums.ParticipantRole;

import java.security.Principal;

public record StompPrincipal(
    Long userId,
    ParticipantRole role,
    String nickname
) implements Principal {

  @Override
  public String getName() {
    return String.valueOf(userId);
  }
}
