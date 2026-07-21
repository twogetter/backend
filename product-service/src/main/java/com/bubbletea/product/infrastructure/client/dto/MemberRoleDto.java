package com.bubbletea.product.infrastructure.client.dto;


public record MemberRoleDto(
    Long memberId,
    String role,
    String status
) {

}
