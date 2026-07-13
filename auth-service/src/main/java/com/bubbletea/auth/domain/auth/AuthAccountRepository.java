package com.bubbletea.auth.domain.auth;

import java.util.Optional;

public interface AuthAccountRepository {

    AuthAccount save(AuthAccount authAccount);

    Optional<AuthAccount> findByEmail(String email);

    Optional<AuthAccount> findByMemberId(Long memberId);

    boolean existsByEmail(String email);
}