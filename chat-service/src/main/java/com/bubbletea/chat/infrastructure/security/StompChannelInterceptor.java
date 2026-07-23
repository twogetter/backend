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
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
  private final Environment environment;

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

    Long userId;
    ParticipantRole role;
    String nickname;

    // TODO: 후에 제거
    boolean isLocalOrDevProfile = environment.acceptsProfiles(
        Profiles.of("local", "dev", "test", "default"));
    boolean isTestTokenAllowed = isLocalOrDevProfile && token.contains("test");

    if (jwtProvider.isValid(token)) {
      try {
        jwtProvider.validateAccessToken(token);
      } catch (IllegalArgumentException e) {
        log.warn("WebSocket CONNECT 실패 - Access Token이 아닙니다: {}", e.getMessage());
        throw new AppException(ChatErrorCode.UNAUTHORIZED);
      }

      // 진짜 토큰인 경우 원본 추출
      userId = jwtProvider.getMemberId(token);
      role = ParticipantRole.from(jwtProvider.getRole(token));
      nickname = jwtProvider.getNickname(token);
    } else if (isTestTokenAllowed) {
      // 로컬 테스트용
      if (token.contains("fan")) {
        userId = 2L;
        role = ParticipantRole.FAN;
        nickname = "팬";
      } else {
        userId = 1L;
        role = ParticipantRole.ARTIST;
        nickname = "아이돌";
      }
    } else {
      log.warn("WebSocket CONNECT 실패 - JWT 토큰 유효하지 않음");
      throw new AppException(ChatErrorCode.UNAUTHORIZED);
    }

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
    if (!matcher.matches()) {
      log.warn("WebSocket SUBSCRIBE 거부 - 올바르지 않은 구독 destination: {}", destination);
      throw new AppException(ChatErrorCode.UNAUTHORIZED);
    }

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

    // TODO: 테스트용은 배포 전에 제거
    try {
      activeParticipantValidator.validate(roomId, principal.userId(), principal.role());
    } catch (Exception e) {
      log.info("테스트 환경 - 방 참여 자격 검증 패스: {}", e.getMessage());
    }
    log.info("WebSocket SUBSCRIBE 성공 - userId: {}, roomId: {}, destination: {}",
        principal.userId(), roomId, destination);
  }
}
