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
      "해당 아티스트의 채팅방을 찾을 수 없습니다."
  ),

  PARTICIPANT_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "CHAT-NOTFOUND-PARTICIPANT",
      "채팅방 참여 정보를 찾을 수 없습니다."
  ),

  DUPLICATE_PARTICIPANT(
      HttpStatus.CONFLICT,
      "CHAT-DUPLICATE-PARTICIPANT",
      "이미 채팅방에 참여 중인 사용자입니다."
  ),

  INVALID_ROLE(
      HttpStatus.BAD_REQUEST,
      "CHAT-INVALID-ROLE",
      "유효하지 않은 참여자 역할입니다."
  ),

  UNAUTHORIZED(
      HttpStatus.UNAUTHORIZED,
      "CHAT-UNAUTHORIZED",
      "인증되지 않은 사용자이거나 토큰이 유효하지 않습니다."
  ),

  INVALID_SIZE(
      HttpStatus.BAD_REQUEST,
      "CHAT-INVALID-SIZE",
      "페이지 크기는 1에서 100 사이여야 합니다."
  ),

  EXCEEDED_DAILY_LIMIT(
      HttpStatus.BAD_REQUEST,
      "CHAT-EXCEEDED-DAILY_LIMIT",
      "일일 전송 횟수(5회)를 초과했습니다."
  );

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
