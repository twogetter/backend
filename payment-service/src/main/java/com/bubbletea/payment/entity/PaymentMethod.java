package com.bubbletea.payment.entity;

import com.bubbletea.payment.entity.enums.PaymentMethodType;
import com.bubbletea.payment.entity.enums.PaymentMethodStatus;
import com.bubbletea.payment.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "payment_methods")
@Getter
@Builder
@SQLDelete(sql = "UPDATE payment_methods SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentMethod extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserBrandpayAuth userBrandpayAuth;


//    private String billingKey;
    private String provider;

    @Enumerated(EnumType.STRING)
    private PaymentMethodType type;
    private String tossMethodId;
    private String displayName;
    private String maskedNumber;

    @Enumerated(EnumType.STRING)
    private PaymentMethodStatus status;
    private Boolean isDefault;

    private LocalDateTime deletedAt;
    @OneToMany(mappedBy = "paymentMethod", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentRetry> paymentRetries = new ArrayList<>();

    public void updateFrom(PaymentMethod incoming) {
        this.displayName = incoming.displayName;
        this.status = incoming.status;
        this.isDefault = incoming.isDefault;
    }
}
