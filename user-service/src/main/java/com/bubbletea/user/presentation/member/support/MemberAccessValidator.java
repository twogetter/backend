package com.bubbletea.user.presentation.member.support;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Component
public class MemberAccessValidator {

    private static final String ADMIN_ROLE = "ADMIN";

    /**
     * 본인 또는 관리자만 회원 리소스에 접근할 수 있도록 검증합니다.
     *
     * @param requesterMemberId Gateway가 전달한 요청자 회원 ID
     * @param requesterRole     Gateway가 전달한 요청자 역할
     * @param targetMemberId    접근하려는 대상 회원 ID
     */
    public void validateOwnerOrAdmin(
            Long requesterMemberId,
            String requesterRole,
            Long targetMemberId
    ) {
        boolean owner =
                Objects.equals(
                        requesterMemberId,
                        targetMemberId
                );

        boolean admin =
                ADMIN_ROLE.equals(requesterRole);

        if (owner || admin) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 또는 관리자만 해당 회원 정보에 접근할 수 있습니다."
        );
    }
}