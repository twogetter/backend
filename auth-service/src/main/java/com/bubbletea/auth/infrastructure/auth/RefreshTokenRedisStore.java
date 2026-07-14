package com.bubbletea.auth.infrastructure.auth;

import com.bubbletea.auth.domain.auth.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisStore
        implements RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(
            Long memberId,
            String refreshToken,
            Duration expiration
    ) {
        redisTemplate.opsForValue().set(
                createKey(memberId),
                refreshToken,
                expiration
        );
    }

    @Override
    public Optional<String> findByMemberId(Long memberId) {
        String refreshToken =
                redisTemplate.opsForValue().get(
                        createKey(memberId)
                );

        return Optional.ofNullable(refreshToken);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        redisTemplate.delete(createKey(memberId));
    }

    private String createKey(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}