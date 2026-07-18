package com.bubbletea.chat.infrastructure.security;

public class SecurityContextHolder {

  private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();

  public static void setContext(SecurityContext context) {
    contextHolder.set(context);
  }

  public static SecurityContext getContext() {
    return contextHolder.get();
  }

  public static void clear() {
    contextHolder.remove();
  }
}
