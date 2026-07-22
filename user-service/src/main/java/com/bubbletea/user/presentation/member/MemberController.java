package com.bubbletea.user.presentation.member;

import com.bubbletea.user.application.member.MemberCommandService;
import com.bubbletea.user.application.member.MemberQueryService;
import com.bubbletea.user.application.member.result.MemberInfoResult;
import com.bubbletea.user.presentation.member.dto.MemberInfoResponseDto;
import com.bubbletea.user.presentation.member.dto.MemberProfileUpdateRequestDto;
import com.bubbletea.user.presentation.member.support.MemberAccessValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private static final String USER_ID_HEADER =
            "X-User-Id";

    private static final String USER_ROLE_HEADER =
            "X-User-Role";

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;
    private final MemberAccessValidator memberAccessValidator;

    @GetMapping("/{memberId}")
    public MemberInfoResponseDto detail(
            @PathVariable Long memberId,
            @RequestHeader(USER_ID_HEADER)
            Long requesterMemberId,
            @RequestHeader(USER_ROLE_HEADER)
            String requesterRole
    ) {
        memberAccessValidator.validateOwnerOrAdmin(
                requesterMemberId,
                requesterRole,
                memberId
        );

        MemberInfoResult result =
                memberQueryService.getById(memberId);

        return MemberInfoResponseDto.from(result);
    }

    @PatchMapping("/{memberId}/profile")
    public void edit(
            @PathVariable Long memberId,
            @RequestHeader(USER_ID_HEADER)
            Long requesterMemberId,
            @RequestHeader(USER_ROLE_HEADER)
            String requesterRole,
            @Valid @RequestBody
            MemberProfileUpdateRequestDto request
    ) {
        memberAccessValidator.validateOwnerOrAdmin(
                requesterMemberId,
                requesterRole,
                memberId
        );

        memberCommandService.update(
                memberId,
                request.toCommand()
        );
    }

    @DeleteMapping("/{memberId}")
    public void delete(
            @PathVariable Long memberId,
            @RequestHeader(USER_ID_HEADER)
            Long requesterMemberId,
            @RequestHeader(USER_ROLE_HEADER)
            String requesterRole
    ) {
        memberAccessValidator.validateOwnerOrAdmin(
                requesterMemberId,
                requesterRole,
                memberId
        );

        memberCommandService.delete(memberId);
    }
}