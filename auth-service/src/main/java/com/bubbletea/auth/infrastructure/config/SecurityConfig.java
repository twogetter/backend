package com.bubbletea.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http
                /*
                 * JWT 기반 REST API이므로 서버 세션을 사용하지 않는다.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * 기본 Form Login과 HTTP Basic 인증을 사용하지 않는다.
                 */
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                /*
                 * 현재 MVP에서는 Refresh Token 쿠키의 SameSite 정책을
                 * 사용하고 있으며 별도 CSRF 토큰은 사용하지 않는다.
                 */
                .csrf(AbstractHttpConfigurer::disable)

                /*
                 * 회원가입, 로그인, 토큰 재발급, 로그아웃은
                 * 인증 전에도 접근할 수 있어야 한다.
                 *
                 * 그 외 Auth Service 엔드포인트는 기본 차단한다.
                 */
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers("/api/auth/**")
                                .permitAll()
                                .anyRequest()
                                .denyAll()
                )

                .build();
    }
}