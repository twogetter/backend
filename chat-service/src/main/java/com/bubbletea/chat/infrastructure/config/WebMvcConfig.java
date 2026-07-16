package com.bubbletea.chat.infrastructure.config;

import com.bubbletea.chat.infrastructure.security.SecurityContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final SecurityContextInterceptor securityContextInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(securityContextInterceptor)
        .addPathPatterns("/api/**");
  }
}
