package com.bubbletea.auth.application.auth;

import com.bubbletea.auth.application.auth.command.LoginCommand;
import com.bubbletea.auth.application.auth.command.LogoutCommand;
import com.bubbletea.auth.application.auth.command.RefreshTokenCommand;
import com.bubbletea.auth.application.auth.command.SignUpCommand;
import com.bubbletea.auth.application.auth.result.SignUpResult;
import com.bubbletea.auth.application.auth.result.TokenResult;
import com.bubbletea.auth.domain.auth.AuthAccount;
import com.bubbletea.auth.domain.auth.AuthAccountRepository;
import com.bubbletea.auth.domain.auth.RefreshTokenStore;
import com.bubbletea.auth.infrastructure.client.UserServiceClient;
import com.bubbletea.auth.infrastructure.client.dto.CreateMemberInternalRequest;
import com.bubbletea.auth.infrastructure.client.dto.CreateMemberInternalResponse;
import com.bubbletea.auth.infrastructure.client.dto.MemberAuthInfoResponse;
import com.bubbletea.auth.infrastructure.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class AuthCommandService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final AuthAccountRepository authAccountRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final UserServiceClient userServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 회원가입
     *
     * 1. Auth DB에서 이메일 중복 확인
     * 2. 비밀번호 BCrypt 암호화
     * 3. User Service에 회원 기본 정보 생성 요청
     * 4. Auth DB에 로그인 계정 저장
     */
    @Transactional
    public SignUpResult signUp(SignUpCommand command) {
        validateDuplicateEmail(command.email());

        String encodedPassword =
                passwordEncoder.encode(command.password());

        CreateMemberInternalResponse memberResponse =
                userServiceClient.createMember(
                        new CreateMemberInternalRequest(
                                command.email(),
                                command.nickname()
                        )
                );

        AuthAccount authAccount = AuthAccount.create(
                memberResponse.memberId(),
                command.email(),
                encodedPassword
        );

        AuthAccount savedAccount =
                authAccountRepository.save(authAccount);

        return new SignUpResult(
                savedAccount.getMemberId(),
                memberResponse.email(),
                memberResponse.nickname()
        );
    }

    /**
     * 로그인
     *
     * 1. 이메일로 Auth 계정 조회
     * 2. BCrypt 비밀번호 검증
     * 3. User Service에서 회원 상태 및 역할 검증
     * 4. Access/Refresh Token 발급
     * 5. Refresh Token Redis 저장
     */
    @Transactional(readOnly = true)
    public TokenResult login(LoginCommand command) {
        AuthAccount authAccount =
                authAccountRepository.findByEmail(command.email())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "이메일 또는 비밀번호가 올바르지 않습니다."
                                )
                        );

        validatePassword(
                command.password(),
                authAccount.getPasswordHash()
        );

        MemberAuthInfoResponse memberInfo =
                userServiceClient.getMemberAuthInfo(
                        authAccount.getMemberId()
                );

        validateLoginAvailable(memberInfo);

        return issueTokens(
                memberInfo.memberId(),
                memberInfo.role()
        );
    }

    /**
     * 토큰 재발급
     *
     * Refresh Token Rotation 방식으로
     * 기존 Refresh Token을 검증한 뒤 새 토큰으로 교체한다.
     */
    public TokenResult refresh(
            RefreshTokenCommand command
    ) {
        String refreshToken = command.refreshToken();

        jwtProvider.validateRefreshToken(refreshToken);

        Long memberId =
                jwtProvider.getMemberId(refreshToken);

        String storedRefreshToken =
                refreshTokenStore.findByMemberId(memberId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "저장된 Refresh Token이 없습니다."
                                )
                        );

        if (!matchesToken(
                refreshToken,
                storedRefreshToken
        )) {
            throw new IllegalArgumentException(
                    "Refresh Token이 일치하지 않습니다."
            );
        }

        MemberAuthInfoResponse memberInfo =
                userServiceClient.getMemberAuthInfo(memberId);

        validateLoginAvailable(memberInfo);

        return issueTokens(
                memberInfo.memberId(),
                memberInfo.role()
        );
    }

    /**
     * 로그아웃
     *
     * 전달받은 Refresh Token과 Redis에 저장된 토큰이
     * 일치할 때만 Redis에서 삭제한다.
     */
    public void logout(LogoutCommand command) {
        String refreshToken = command.refreshToken();

        jwtProvider.validateRefreshToken(refreshToken);

        Long memberId =
                jwtProvider.getMemberId(refreshToken);

        String storedRefreshToken =
                refreshTokenStore.findByMemberId(memberId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "이미 로그아웃되었거나 저장된 토큰이 없습니다."
                                )
                        );

        if (!matchesToken(
                refreshToken,
                storedRefreshToken
        )) {
            throw new IllegalArgumentException(
                    "Refresh Token이 일치하지 않습니다."
            );
        }

        refreshTokenStore.deleteByMemberId(memberId);
    }

    private TokenResult issueTokens(
            Long memberId,
            String role
    ) {
        String accessToken =
                jwtProvider.createAccessToken(
                        memberId,
                        role
                );

        String refreshToken =
                jwtProvider.createRefreshToken(
                        memberId,
                        role
                );

        refreshTokenStore.save(
                memberId,
                refreshToken,
                jwtProvider.getRefreshTokenExpiration()
        );

        return new TokenResult(
                accessToken,
                refreshToken,
                jwtProvider.getAccessTokenExpirationSeconds(),
                jwtProvider.getRefreshTokenExpirationSeconds()
        );
    }

    private void validateDuplicateEmail(String email) {
        if (authAccountRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 이메일입니다."
            );
        }
    }

    private void validatePassword(
            String rawPassword,
            String encodedPassword
    ) {
        if (!passwordEncoder.matches(
                rawPassword,
                encodedPassword
        )) {
            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }
    }

    private void validateLoginAvailable(
            MemberAuthInfoResponse memberInfo
    ) {
        if (!memberInfo.loginAvailable()) {
            throw new IllegalArgumentException(
                    "로그인할 수 없는 회원입니다."
            );
        }

        if (!ACTIVE_STATUS.equals(memberInfo.status())) {
            throw new IllegalArgumentException(
                    "활성 상태의 회원이 아닙니다."
            );
        }
    }

    private boolean matchesToken(
            String providedToken,
            String storedToken
    ) {
        return MessageDigest.isEqual(
                providedToken.getBytes(StandardCharsets.UTF_8),
                storedToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}