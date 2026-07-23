package com.bubbletea.chat.infrastructure.security;

import com.bubbletea.chat.application.service.validator.ActiveParticipantValidator;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.common.exception.AppException;
import java.security.Principal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private static final Pattern SUB_DEST_PATTERN = Pattern.compile(
      "^/sub/rooms/(\\d+)/(artist|fan)$");

  private final JwtProvider jwtProvider;
  private final ActiveParticipantValidator activeParticipantValidator;

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
        StompHeaderAccessor.class);

    if (accessor == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      handleConnect(accessor);
    } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      handleSubscribe(accessor);
    }

    return message;
  }

  private void handleConnect(StompHeaderAccessor accessor) {
    String authHeader = accessor.getFirstNativeHeader("Authorization");
    log.info("WebSocket CONNECT 시도");

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      log.warn("WebSocket CONNECT 실패 - Authorization 헤더 누락");
      throw new AppException(ChatErrorCode.UNAUTHORIZED);
    }

    String token = authHeader.substring(BEARER_PREFIX.length());

    if (!jwtProvider.isValid(token)) {
      log.warn("WebSocket CONNECT 실패 - JWT 토큰 유효하지 않음");
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

  private void handleSubscribe(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    Principal user = accessor.getUser();

    if (destination == null || !(user instanceof StompPrincipal principal)) {
      log.warn("WebSocket SUBSCRIBE 실패 - 미인증 유저 또는 destination 누락");
      throw new AppException(ChatErrorCode.UNAUTHORIZED);
    }

    Matcher matcher = SUB_DEST_PATTERN.matcher(destination);
    if (matcher.matches()) {
      Long roomId = Long.valueOf(matcher.group(1));
      String targetGroup = matcher.group(2);

      if (principal.role() == ParticipantRole.FAN && !"artist".equals(targetGroup)) {
        log.warn("WebSocket SUBSCRIBE 거부 - 팬은 /artist 대역만 구독할 수 있습니다. dest: {}", destination);
        throw new AppException(ChatErrorCode.UNAUTHORIZED);
      }
      if (principal.role() == ParticipantRole.ARTIST && !"fan".equals(targetGroup)) {
        log.warn("WebSocket SUBSCRIBE 거부 - 아티스트는 /fan 대역만 구독할 수 있습니다. dest: {}", destination);
        throw new AppException(ChatErrorCode.UNAUTHORIZED);
      }

      activeParticipantValidator.validate(roomId, principal.userId(), principal.role());
      log.info("WebSocket SUBSCRIBE 성공 - userId: {}, roomId: {}, destination: {}",
          principal.userId(), roomId, destination);
    }
  }
}
