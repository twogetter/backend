package com.bubbletea.chat.infrastructure.security;

import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.common.exception.AppException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

  private static final String BEARER_PREFIX = "Bearer ";
  private final JwtProvider jwtProvider;

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
        StompHeaderAccessor.class);

    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      String authHeader = accessor.getFirstNativeHeader("Authorization");
      log.info("WebSocket CONNECT 시도 - Authorization header 수신");

      if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
        log.warn("WebSocket CONNECT 실패 - Authorization 헤더가 누락되었거나 Bearer 타입이 아닙니다.");
        throw new AppException(ChatErrorCode.UNAUTHORIZED);
      }

      String token = authHeader.substring(BEARER_PREFIX.length());

      if (!jwtProvider.isValid(token)) {
        log.warn("WebSocket CONNECT 실패 - JWT 토큰이 유효하지 않습니다.");
        throw new AppException(ChatErrorCode.UNAUTHORIZED);
      }

      try {
        jwtProvider.validateAccessToken(token);
      } catch (IllegalArgumentException e) {
        log.warn("WebSocket CONNECT 실패 - Access Token 검증에 실패했습니다: {}", e.getMessage());
        throw new AppException(ChatErrorCode.UNAUTHORIZED);
      }

      Long userId = jwtProvider.getMemberId(token);
      String roleStr = jwtProvider.getRole(token);
      ParticipantRole role = ParticipantRole.from(roleStr);
      String nickname = jwtProvider.getNickname(token);

      StompPrincipal principal = new StompPrincipal(userId, role, nickname);
      accessor.setUser(principal);

      Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
      if (sessionAttributes != null) {
        sessionAttributes.put("userId", userId);
        sessionAttributes.put("role", role);
        sessionAttributes.put("nickname", nickname);
      }

      log.info("WebSocket CONNECT 성공 - userId: {}, role: {}, nickname: {}", userId, role, nickname);
    }

    return message;
  }
}
