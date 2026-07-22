package com.bubbletea.chat.infrastructure.security;

import com.bubbletea.chat.application.service.validator.ActiveParticipantValidator;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 웹소켓 연결(CONNECT) 시 JWT 토큰 검증 및
 * 구독(SUBSCRIBE) 시점의 방 참여자 자격/권한 검증을 담당하는 채널 인터셉터입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern SUB_DEST_PATTERN = Pattern.compile("^/sub/rooms/(\\d+)/(artist|fan)$");

    private final JwtProvider jwtProvider;
    private final ActiveParticipantValidator activeParticipantValidator;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // 1. CONNECT 시점 JWT 토큰 검증 및 Principal/세션 저장
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        }
        // 2. SUBSCRIBE 시점 방 참여 자격 및 역할별 구독 대역 권한 검증
        else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
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
            String targetGroup = matcher.group(2); // "artist" 또는 "fan"

            // 1. 역할(Role)별 구독 허용 대역 검증
            if (principal.role() == ParticipantRole.FAN && !"artist".equals(targetGroup)) {
                log.warn("WebSocket SUBSCRIBE 거부 - 팬은 /artist 대역만 구독할 수 있습니다. dest: {}", destination);
                throw new AppException(ChatErrorCode.UNAUTHORIZED);
            }
            if (principal.role() == ParticipantRole.ARTIST && !"fan".equals(targetGroup)) {
                log.warn("WebSocket SUBSCRIBE 거부 - 아티스트는 /fan 대역만 구독할 수 있습니다. dest: {}", destination);
                throw new AppException(ChatErrorCode.UNAUTHORIZED);
            }

            // 2. 해당 방의 ACTIVE 참여자 검증
            activeParticipantValidator.validate(roomId, principal.userId(), principal.role());
            log.info("WebSocket SUBSCRIBE 성공 - userId: {}, roomId: {}, destination: {}", principal.userId(), roomId, destination);
        }
    }
}
