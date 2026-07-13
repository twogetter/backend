package com.bubbletea.chat.domain.exception;

import com.bubbletea.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {

  DUPLICATE_ARTIST_ROOM(
      HttpStatus.CONFLICT,
      "CHAT-DUPLICATE-ROOM",
      "해당 아티스트의 채팅방이 이미 존재합니다."
  ),


  CHAT_ROOM_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "CHAT-NOTFOUND-ROOM",
      "해당 ID의 채팅방을 찾을 수 없습니다."
  );

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
