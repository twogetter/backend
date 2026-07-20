package com.bubbletea.order.domain.entity;

import com.bubbletea.order.domain.enums.SubscriptionStatus;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Table(name = "billing_schedules")
@SQLRestriction("deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingSchedule {

  @Id  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subscription_id", nullable = false)
  private Subscription subscription;

  @Column(name = "payment_method_id")
  private Long paymentMethodId;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "next_billing_date")
  private LocalDate nextBillingDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SubscriptionStatus status;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public BillingSchedule(Subscription subscription, Long paymentMethodId, BigDecimal amount) {
    this.subscription = subscription;
    this.paymentMethodId = paymentMethodId;
    this.amount = amount;
    this.status = SubscriptionStatus.PENDING;
  }

  public SubscriptionOrder createOrder() {
    return new SubscriptionOrder(this, amount);
  }

  public void activate() {
    this.status = SubscriptionStatus.ACTIVE;
    this.nextBillingDate = LocalDate.now().plusMonths(1);
  }

  /**
   * 정기결제 성공 시 다음 결제일을 한 달 뒤로 이월(결제 주기 유지).
   * 배치 지연 등으로 이월 후에도 여전히 과거면 미래가 될 때까지 이월해, 다음 배치의 연속 청구를 방지
   * (미납분 소급청구는 하지 않는 정책 — 필요 시 별도 구현)
   */
  public void renew() {
    LocalDate base = (nextBillingDate != null) ? nextBillingDate : LocalDate.now();
    LocalDate today = LocalDate.now();
    LocalDate next = base.plusMonths(1);
    while (!next.isAfter(today)) {
      next = next.plusMonths(1);
    }
    this.nextBillingDate = next;
  }

  public void softDelete() {
    this.status = SubscriptionStatus.CANCELED;
    this.deletedAt = LocalDateTime.now();
  }
}
