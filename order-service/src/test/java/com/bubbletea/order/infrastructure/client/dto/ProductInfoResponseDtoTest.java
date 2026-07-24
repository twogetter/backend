package com.bubbletea.order.infrastructure.client.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProductInfoResponseDto (product-service 검증 응답 어댑터)")
class ProductInfoResponseDtoTest {

  @Test
  @DisplayName("편의 접근자는 pid·name 을 주문 도메인 용어로 노출한다")
  void convenienceAccessors() {
    ProductInfoResponseDto dto =
        new ProductInfoResponseDto(10L, "[솔로 아이유] 구독권", 9900L, "ACTIVE", 10L);

    assertThat(dto.productId()).isEqualTo(10L);
    assertThat(dto.productName()).isEqualTo("[솔로 아이유] 구독권");
    assertThat(dto.priceAmount()).isEqualByComparingTo(BigDecimal.valueOf(9900));
  }

  @Test
  @DisplayName("price 가 null 이면 priceAmount() 는 null 을 반환한다")
  void priceAmountNullWhenPriceNull() {
    ProductInfoResponseDto dto =
        new ProductInfoResponseDto(10L, "구독권", null, "ACTIVE", 10L);

    assertThat(dto.priceAmount()).isNull();
  }
}
