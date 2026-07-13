package com.bubbletea.auth.presentation.auth;

import com.bubbletea.auth.application.auth.AuthCommandService;
import com.bubbletea.auth.application.auth.command.LogoutCommand;
import com.bubbletea.auth.application.auth.command.RefreshTokenCommand;
import com.bubbletea.auth.application.auth.result.SignUpResult;
import com.bubbletea.auth.application.auth.result.TokenResult;
import com.bubbletea.auth.presentation.auth.dto.LoginRequest;
import com.bubbletea.auth.presentation.auth.dto.SignUpRequest;
import com.bubbletea.auth.presentation.auth.dto.SignUpResponse;
import com.bubbletea.auth.presentation.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthCommandService authCommandService;
    private final RefreshTokenCookieManager cookieManager;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(
            @Valid @RequestBody SignUpRequest request
    ) {
        SignUpResult result =
                authCommandService.signUp(
                        request.toCommand()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SignUpResponse.from(result));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResult result =
                authCommandService.login(
                        request.toCommand()
                );

        ResponseCookie refreshTokenCookie =
                cookieManager.create(
                        result.refreshToken(),
                        result.refreshTokenExpiresIn()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookie.toString()
                )
                .body(TokenResponse.from(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(
                    name = RefreshTokenCookieManager.COOKIE_NAME
            )
            String refreshToken
    ) {
        TokenResult result =
                authCommandService.refresh(
                        new RefreshTokenCommand(refreshToken)
                );

        ResponseCookie rotatedRefreshTokenCookie =
                cookieManager.create(
                        result.refreshToken(),
                        result.refreshTokenExpiresIn()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        rotatedRefreshTokenCookie.toString()
                )
                .body(TokenResponse.from(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(
                    name = RefreshTokenCookieManager.COOKIE_NAME
            )
            String refreshToken
    ) {
        authCommandService.logout(
                new LogoutCommand(refreshToken)
        );

        ResponseCookie deletedCookie =
                cookieManager.delete();

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deletedCookie.toString()
                )
                .build();
    }
}