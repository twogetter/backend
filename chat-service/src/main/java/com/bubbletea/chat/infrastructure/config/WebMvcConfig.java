package com.bubbletea.chat.infrastructure.config;

import com.bubbletea.chat.infrastructure.security.SecurityContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final SecurityContextInterceptor securityContextInterceptor;

  @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8600,http://localhost:8000}")
  private String[] allowedOrigins;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(securityContextInterceptor)
        .addPathPatterns("/api/**");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOriginPatterns(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }
}
