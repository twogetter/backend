package com.bubbletea.payment.entity;

import com.bubbletea.payment.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "user_brandpay_auth")
@Getter
@Builder
@SQLDelete(sql = "UPDATE user_brandpay_auth SET deleted_at = NOW() WHERE user_id = ?")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserBrandpayAuth extends BaseEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;
    private String customerKey;
    private String accessToken;
    private String refreshToken;

    private LocalDateTime deletedAt;
    @OneToMany(mappedBy = "userBrandpayAuth", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

    public void updateTossTokens(String newAccessToken, String newRefreshToken) {
        if (newAccessToken == null || newAccessToken.isEmpty()) {
            throw new IllegalArgumentException("갱신할 AccessToken이 올바르지 않습니다.");
        }
        this.accessToken = newAccessToken;
        this.refreshToken = newRefreshToken;
        this.deletedAt = null;
    }
}
