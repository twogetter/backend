package com.bubbletea.user.presentation.member;

import com.bubbletea.user.application.member.MemberCommandService;
import com.bubbletea.user.application.member.MemberQueryService;
import com.bubbletea.user.application.member.result.MemberInfoResult;
import com.bubbletea.user.application.member.result.MemberProfileResult;
import com.bubbletea.user.application.member.result.MemberRoleResult;
import com.bubbletea.user.application.member.result.MemberStatusResult;
import com.bubbletea.user.presentation.member.dto.MemberCreateRequestDto;
import com.bubbletea.user.presentation.member.dto.MemberInfoResponseDto;
import com.bubbletea.user.presentation.member.dto.MemberProfileResponseDto;
import com.bubbletea.user.presentation.member.dto.MemberRoleResponseDto;
import com.bubbletea.user.presentation.member.dto.MemberStatusResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/members")
@RequiredArgsConstructor
public class InternalMemberController {

    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;

    @PostMapping
    public Long create(
            @Valid @RequestBody MemberCreateRequestDto request
    ) {
        return memberCommandService.save(
                request.toCommand()
        );
    }

    @GetMapping("/{memberId}")
    public MemberInfoResponseDto detail(
            @PathVariable Long memberId
    ) {
        MemberInfoResult result =
                memberQueryService.getById(memberId);

        return MemberInfoResponseDto.from(result);
    }

    @GetMapping("/{memberId}/status")
    public MemberStatusResponseDto status(
            @PathVariable Long memberId
    ) {
        MemberStatusResult result =
                memberQueryService.getStatus(memberId);

        return MemberStatusResponseDto.from(result);
    }

    @GetMapping("/{memberId}/profile")
    public MemberProfileResponseDto profile(
            @PathVariable Long memberId
    ) {
        MemberProfileResult result =
                memberQueryService.getProfile(memberId);

        return MemberProfileResponseDto.from(result);
    }

    @GetMapping("/{memberId}/role")
    public MemberRoleResponseDto role(
            @PathVariable Long memberId
    ) {
        MemberRoleResult result =
                memberQueryService.getRole(memberId);

        return MemberRoleResponseDto.from(result);
    }

    /**
     * Auth 계정 저장 실패 시 호출되는 회원가입 보상 API
     */
    @DeleteMapping("/{memberId}/signup-rollback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rollbackSignUp(
            @PathVariable Long memberId
    ) {
        memberCommandService.rollbackSignUp(memberId);
    }
}