package com.bubbletea.order.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface MemberClient {
  // user-service 실제 내부 API 경로에 정렬. 존재하지 않으면 404 → FeignException 으로 검증 실패 처리.
  @GetMapping("/internal/members/{memberId}")
  void validateMember(@PathVariable("memberId") Long memberId);
}
