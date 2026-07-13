package com.bubbletea.auth.domain.auth;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenStore {

    void save(
            Long memberId,
            String refreshToken,
            Duration expiration
    );

    Optional<String> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}