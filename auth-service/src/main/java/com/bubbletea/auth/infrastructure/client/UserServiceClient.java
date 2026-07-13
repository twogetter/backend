package com.bubbletea.auth.infrastructure.client;

import com.bubbletea.auth.infrastructure.client.dto.CreateMemberInternalRequest;
import com.bubbletea.auth.infrastructure.client.dto.CreateMemberInternalResponse;
import com.bubbletea.auth.infrastructure.client.dto.MemberAuthInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "user-service",
        path = "/internal/members"
)
public interface UserServiceClient {

    @PostMapping
    CreateMemberInternalResponse createMember(
            @RequestBody CreateMemberInternalRequest request
    );

    @GetMapping("/{memberId}/auth-info")
    MemberAuthInfoResponse getMemberAuthInfo(
            @PathVariable("memberId") Long memberId
    );

    /**
     * 회원가입 과정에서 Auth 계정 저장에 실패했을 때
     * user-service에 먼저 생성된 회원을 되돌린다.
     */
    @DeleteMapping("/{memberId}/signup-rollback")
    void rollbackSignUp(
            @PathVariable("memberId") Long memberId
    );
}