package com.bubbletea.product.application.reservation.executor;


import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.BATCH_ID;
import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.CHANGED_PRICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.application.reservation.executor.PriceChangeExecutor;
import com.bubbletea.product.domain.history.ProductChangeHistory;
import com.bubbletea.product.domain.history.ProductChangeHistoryRepository;
import com.bubbletea.product.domain.history.ProductPriceChangeHistory;
import com.bubbletea.product.domain.history.ProductPriceChangeHistoryRepository;
import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductRepository;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PriceChangeExecutorTest {

    @InjectMocks
    private PriceChangeExecutor priceChangeExecutor;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductChangeHistoryRepository productChangeHistoryRepository;

    @Mock
    private ProductPriceChangeHistoryRepository productPriceChangeHistoryRepository;

    @Test
    @DisplayName("supports()는 PRICE_CHANGE 타입을 반환한다.")
    void supports_returnsPriceChangeType() {
        // when
        ReservationCommandType result = priceChangeExecutor.supports();

        // then
        assertThat(result).isEqualTo(ReservationCommandType.PRICE_CHANGE);
    }

    @Test
    @DisplayName("정상적으로 가격을 변경하고 이력을 저장한다.")
    void execute_success() {
        // given
        String productId = "PRD001";
        long originalPrice = 10000L;
        long changedPrice = 15000L;
        String batchId = "batch-001";

        Product product = mock(Product.class);
        given(product.getId()).willReturn(productId);
        given(product.getPrice()).willReturn(originalPrice);

        Map<String, Object> payload = new HashMap<>();
        payload.put(CHANGED_PRICE, changedPrice);
        payload.put(BATCH_ID, batchId);

        ProductChangeReservation reservation = mock(ProductChangeReservation.class);
        given(reservation.getProductId()).willReturn(productId);
        given(reservation.getPayload()).willReturn(payload);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(productPriceChangeHistoryRepository.save(any(ProductPriceChangeHistory.class)))
            .willReturn(mock(ProductPriceChangeHistory.class));
        given(productChangeHistoryRepository.save(any(ProductChangeHistory.class)))
            .willReturn(mock(ProductChangeHistory.class));

        // when
        priceChangeExecutor.execute(reservation);

        // then
        then(product).should().changePrice(changedPrice);
        then(productRepository).should().save(product);
        then(productPriceChangeHistoryRepository).should()
            .save(any(ProductPriceChangeHistory.class));
        then(productChangeHistoryRepository).should().save(any(ProductChangeHistory.class));
    }

    @Test
    @DisplayName("상품이 존재하지 않으면 AppException을 던진다.")
    void execute_productNotFound_throwsAppException() {
        // given
        String productId = "PRD00";

        Map<String, Object> payload = new HashMap<>();
        payload.put(CHANGED_PRICE, 15000L);
        payload.put(BATCH_ID, "batch-001");

        ProductChangeReservation reservation = mock(ProductChangeReservation.class);
        given(reservation.getProductId()).willReturn(productId);

        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> priceChangeExecutor.execute(reservation))
            .isInstanceOf(AppException.class);

        then(productRepository).should(never()).save(any());
        then(productPriceChangeHistoryRepository).should(never()).save(any());
        then(productChangeHistoryRepository).should(never()).save(any());
    }
}
