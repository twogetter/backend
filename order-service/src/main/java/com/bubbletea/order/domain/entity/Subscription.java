package com.bubbletea.order.domain.entity;

import com.bubbletea.order.domain.enums.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Table(name = "subscriptions")
@SQLRestriction("deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SubscriptionStatus status;

  @CreatedDate
  @Column(name = "started_at", nullable = false, updatable = false)
  private LocalDateTime startedAt;
  private LocalDateTime endedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public Subscription(Long memberId, Long productId) {
    this.memberId = memberId;
    this.productId = productId;
    this.status = SubscriptionStatus.PENDING;
  }

  public void activate() {
    this.status = SubscriptionStatus.ACTIVE;
  }

  public void softDelete() {
    this.status = SubscriptionStatus.CANCELED;
    this.endedAt = LocalDateTime.now();
    this.deletedAt = LocalDateTime.now();
  }
}
