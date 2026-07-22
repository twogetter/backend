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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
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
     * 5. Auth DB 저장 실패 시 User Service 회원 생성 보상 처리
     */
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

        try {
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

        } catch (RuntimeException saveException) {
            rollbackCreatedMember(
                    memberResponse.memberId(),
                    saveException
            );

            throw saveException;
        }
    }

    /**
     * 로그인
     *
     * 1. 이메일로 Auth 계정 조회
     * 2. BCrypt 비밀번호 검증
     * 3. User Service에서 회원 상태, 역할, 닉네임 조회
     * 4. Access Token과 Refresh Token 발급
     * 5. Refresh Token Redis 저장
     */
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
                memberInfo.role(),
                memberInfo.nickname()
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

        /*
         * 재발급 시 user-service에서 회원 정보를 다시 조회한다.
         * 닉네임이 변경되었다면 새로운 Access Token에는
         * 변경된 최신 닉네임이 포함된다.
         */
        MemberAuthInfoResponse memberInfo =
                userServiceClient.getMemberAuthInfo(memberId);

        validateLoginAvailable(memberInfo);

        return issueTokens(
                memberInfo.memberId(),
                memberInfo.role(),
                memberInfo.nickname()
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

    /**
     * Access Token에는 닉네임을 포함한다.
     *
     * Refresh Token에는 변경 가능한 닉네임을 포함하지 않는다.
     */
    private TokenResult issueTokens(
            Long memberId,
            String role,
            String nickname
    ) {
        String accessToken =
                jwtProvider.createAccessToken(
                        memberId,
                        role,
                        nickname
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

    /**
     * Auth 계정 저장에 실패했을 경우
     * user-service에 먼저 생성된 회원을 보상 삭제한다.
     */
    private void rollbackCreatedMember(
            Long memberId,
            RuntimeException originalException
    ) {
        try {
            userServiceClient.rollbackSignUp(memberId);

        } catch (RuntimeException rollbackException) {
            log.error(
                    "회원가입 보상 처리 실패. memberId={}",
                    memberId,
                    rollbackException
            );

            originalException.addSuppressed(
                    rollbackException
            );
        }
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

        if (
                memberInfo.nickname() == null
                        || memberInfo.nickname().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "회원 닉네임 정보가 없습니다."
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