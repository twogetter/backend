package com.bubbletea.product.application.reservation.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.domain.history.ProductChangeHistory;
import com.bubbletea.product.domain.history.ProductChangeHistoryRepository;
import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductRepository;
import com.bubbletea.product.domain.product.ProductStatus;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivateExecutorTest {

    @InjectMocks
    private ActivateExecutor activateExecutor;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductChangeHistoryRepository productChangeHistoryRepository;

    @Test
    @DisplayName("supports()는 ACTIVATE 타입을 반환한다.")
    void supports_returnsActivateType() {
        // when
        ReservationCommandType result = activateExecutor.supports();

        // then
        assertThat(result).isEqualTo(ReservationCommandType.ACTIVATE);
    }

    @Test
    @DisplayName("정상적으로 상품을 활성화하고 이력을 저장한다.")
    void execute_success() {
        // given
        String productId = "001";

        Product product = mock(Product.class);
        given(product.getStatus()).willReturn(ProductStatus.ACTIVE);

        ProductChangeReservation reservation = mock(ProductChangeReservation.class);
        given(reservation.getProductId()).willReturn(productId);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(productChangeHistoryRepository.save(any(ProductChangeHistory.class)))
            .willReturn(mock(ProductChangeHistory.class));

        // when
        activateExecutor.execute(reservation);

        // then
        then(product).should().activate();
        then(productRepository).should().save(product);
        then(productChangeHistoryRepository).should().save(any(ProductChangeHistory.class));
    }

    @Test
    @DisplayName("상품이 존재하지 않으면 AppException을 던진다.")
    void execute_productNotFound_throwsAppException() {
        // given
        String productId = "001";

        ProductChangeReservation reservation = mock(ProductChangeReservation.class);
        given(reservation.getProductId()).willReturn(productId);

        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> activateExecutor.execute(reservation))
            .isInstanceOf(AppException.class);

        then(productRepository).should(never()).save(any());
        then(productChangeHistoryRepository).should(never()).save(any());
    }
}