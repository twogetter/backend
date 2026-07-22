package com.bubbletea.order.domain.entity;

import com.bubbletea.order.domain.enums.AttemptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Table(name = "payment_attempts")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttempt {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private SubscriptionOrder subscriptionOrder;

  private int sequence;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private AttemptStatus status;

  @Column(name="fail_reason", columnDefinition = "TEXT")
  private String failReason;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Builder
  public PaymentAttempt(SubscriptionOrder order, int sequence, AttemptStatus status, String failReason) {
    this.subscriptionOrder = order;
    this.sequence = sequence;
    this.status = status;
    this.failReason = failReason;
    this.createdAt = LocalDateTime.now();
  }

  public static PaymentAttempt success(SubscriptionOrder order, int sequence) {
    return new PaymentAttempt(order, sequence, AttemptStatus.SUCCESS, null);
  }

  public static PaymentAttempt fail(SubscriptionOrder order, int sequence, String failReason) {
    return new PaymentAttempt(order, sequence, AttemptStatus.FAIL, failReason);
  }
}
