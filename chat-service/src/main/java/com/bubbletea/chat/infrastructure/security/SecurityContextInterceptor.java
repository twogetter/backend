package com.bubbletea.chat.infrastructure.security;

import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.common.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SecurityContextInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    String userIdHeader = request.getHeader("X-User-Id");
    String roleHeader = request.getHeader("X-User-Role");

    if (userIdHeader != null && roleHeader != null) {
      try {
        Long userId = Long.valueOf(userIdHeader);
        ParticipantRole role = ParticipantRole.from(roleHeader);
        SecurityContextHolder.setContext(new SecurityContext(userId, role));
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
