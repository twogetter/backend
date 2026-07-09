package com.bubbletea.user.presentation.member;

import com.bubbletea.user.application.member.result.MemberInfoResult;
import com.bubbletea.user.application.member.MemberCommandService;
import com.bubbletea.user.application.member.MemberQueryService;
import com.bubbletea.user.presentation.member.dto.MemberInfoResponseDto;
import com.bubbletea.user.presentation.member.dto.MemberProfileUpdateRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;

    @GetMapping("/{memberId}")
    public MemberInfoResponseDto detail(@PathVariable Long memberId) {
        MemberInfoResult result = memberQueryService.getById(memberId);
        return MemberInfoResponseDto.from(result);
    }

    @PatchMapping("/{memberId}/profile")
    public void edit(
            @PathVariable Long memberId,
            @Valid @RequestBody MemberProfileUpdateRequestDto request
    ) {
        memberCommandService.update(memberId, request.toCommand());
    }

    @DeleteMapping("/{memberId}")
    public void delete(@PathVariable Long memberId) {
        memberCommandService.delete(memberId);
    }
}