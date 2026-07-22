package com.bubbletea.chat.infrastructure.security;

import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.common.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class SecurityContextInterceptor implements HandlerInterceptor {

  private final JwtProvider jwtProvider;

  @Override
  public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    String userIdHeader = request.getHeader("X-User-Id");
    String roleHeader = request.getHeader("X-User-Role");

    if (userIdHeader != null && roleHeader != null) {
      try {
        Long userId = Long.valueOf(userIdHeader);
        ParticipantRole role = ParticipantRole.from(roleHeader);
        
        String nickname = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
          String token = authHeader.substring(7);
          if (jwtProvider.isValid(token)) {
            nickname = jwtProvider.getNickname(token);
          }
        }

        SecurityContextHolder.setContext(new SecurityContext(userId, role, nickname));
      } catch (NumberFormatException e) {
        throw new AppException(ChatErrorCode.INVALID_ROLE);
      }
    }
    return true;
  }

  @Override
  public void afterCompletion(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler, Exception ex) {
    SecurityContextHolder.clear();
  }
}
