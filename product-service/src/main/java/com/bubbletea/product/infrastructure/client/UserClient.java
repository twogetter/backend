package com.bubbletea.product.infrastructure.client;


import com.bubbletea.product.infrastructure.client.dto.MemberRoleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/members/{memberId}/role")
    MemberRoleDto getMemberRoleDto(@PathVariable Long memberId);
}
