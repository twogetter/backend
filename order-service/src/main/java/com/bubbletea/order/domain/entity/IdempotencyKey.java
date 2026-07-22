package com.bubbletea.order.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Table(name = "idempotency_keys",
    uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_member_key",
        columnNames = {"member_id", "idempotency_key"}))
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "idempotency_key", nullable = false)
  private String idempotencyKey;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Column(name = "subscription_id", nullable = false)
  private Long subscriptionId;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public IdempotencyKey(Long memberId, String idempotencyKey, Long orderId, Long subscriptionId) {
    this.memberId = memberId;
    this.idempotencyKey = idempotencyKey;
    this.orderId = orderId;
    this.subscriptionId = subscriptionId;
  }
}
