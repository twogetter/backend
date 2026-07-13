package com.bubbletea.auth.infrastructure.auth;

import com.bubbletea.auth.domain.auth.AuthAccount;
import com.bubbletea.auth.domain.auth.AuthAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthAccountRepositoryImpl
        implements AuthAccountRepository {

    private final AuthAccountJpaRepository authAccountJpaRepository;

    @Override
    public AuthAccount save(AuthAccount authAccount) {
        return authAccountJpaRepository.save(authAccount);
    }

    @Override
    public Optional<AuthAccount> findByEmail(String email) {
        return authAccountJpaRepository.findByEmail(email);
    }

    @Override
    public Optional<AuthAccount> findByMemberId(Long memberId) {
        return authAccountJpaRepository.findByMemberId(memberId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return authAccountJpaRepository.existsByEmail(email);
    }
}