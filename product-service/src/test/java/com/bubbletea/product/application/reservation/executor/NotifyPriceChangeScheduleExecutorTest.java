package com.bubbletea.product.application.reservation.executor;

import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.CHANGED_PRICE;
import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.ORIGINAL_PRICE;
import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.PRICE_CHANGE_DATE;
import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.PRODUCT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.bubbletea.product.application.event.ProductScheduleNotificationService;
import com.bubbletea.product.domain.history.ProductChangeHistory;
import com.bubbletea.product.domain.history.ProductChangeHistoryRepository;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotifyPriceChangeScheduleExecutorTest {

    @InjectMocks
    private NotifyPriceChangeScheduleExecutor notifyPriceChangeScheduleExecutor;

    @Mock
    private ProductScheduleNotificationService productScheduleNotificationService;

    @Mock
    private ProductChangeHistoryRepository productChangeHistoryRepository;

    @Test
    @DisplayName("supports()는 NOTIFY_PRICE_CHANGE_SCHEDULE 타입을 반환한다.")
    void supports_returnsNotifyPriceChangeScheduleType() {
        // when
        ReservationCommandType result = notifyPriceChangeScheduleExecutor.supports();

        // then
        assertThat(result).isEqualTo(ReservationCommandType.NOTIFY_PRICE_CHANGE_SCHEDULE);
    }

    @Test
    @DisplayName("성공적으로 가격 변경 일정을 알림으로 발송하고 이력을 저장한다.")
    void execute_success() {
        // given
        String reservationId = "001";
        String productName = "[김연옌] 구독권";
        LocalDateTime priceChangeDate = LocalDateTime.of(2026, 7, 23, 12, 0);
        long originalPrice = 5000L;
        long changedPrice = 6000L;

        Map<String, Object> payload = new HashMap<>();
        payload.put(PRODUCT_NAME, productName);
        payload.put(PRICE_CHANGE_DATE, priceChangeDate.toString());
        payload.put(ORIGINAL_PRICE, originalPrice);
        payload.put(CHANGED_PRICE, changedPrice);

        ProductChangeReservation reservation = mock(ProductChangeReservation.class);
        given(reservation.getId()).willReturn(reservationId);
        given(reservation.getPayload()).willReturn(payload);

        given(productChangeHistoryRepository.save(any(ProductChangeHistory.class)))
            .willReturn(mock(ProductChangeHistory.class));

        // when
        notifyPriceChangeScheduleExecutor.execute(reservation);

        // then
        then(productScheduleNotificationService).should()
            .notifyPriceChangeSchedule(
                eq(reservationId),
                eq(productName),
                eq(priceChangeDate),
                eq(originalPrice),
                eq(changedPrice)
            );
        then(productChangeHistoryRepository).should().save(any(ProductChangeHistory.class));
    }
}