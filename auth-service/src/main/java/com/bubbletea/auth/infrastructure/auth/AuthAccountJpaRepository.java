package com.bubbletea.auth.infrastructure.auth;

import com.bubbletea.auth.domain.auth.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthAccountJpaRepository
        extends JpaRepository<AuthAccount, Long> {

    Optional<AuthAccount> findByEmail(String email);

    Optional<AuthAccount> findByMemberId(Long memberId);

    boolean existsByEmail(String email);
}