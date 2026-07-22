package com.bubbletea.chat.domain.enums;

import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.common.exception.AppException;

public enum ParticipantRole {
  ARTIST,
  FAN;

  public static ParticipantRole from(String roleHeader) {
    if (roleHeader == null) {
      throw new AppException(ChatErrorCode.INVALID_ROLE);
    }
    if ("ARTIST".equalsIgnoreCase(roleHeader) || "BUSINESS".equalsIgnoreCase(roleHeader)) {
      return ARTIST;
    }
    if ("FAN".equalsIgnoreCase(roleHeader) || "USER".equalsIgnoreCase(roleHeader)) {
      return FAN;
    }
    throw new AppException(ChatErrorCode.INVALID_ROLE);
  }
}
