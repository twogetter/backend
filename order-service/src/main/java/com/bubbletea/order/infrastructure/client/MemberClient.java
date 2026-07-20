package com.bubbletea.order.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface MemberClient {
  @GetMapping("/api/v1/members/{memberId}/validate")
  void validateMember(@PathVariable("memberId") Long memberId);
}
