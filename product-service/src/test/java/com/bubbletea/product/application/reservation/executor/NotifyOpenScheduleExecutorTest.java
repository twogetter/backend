package com.bubbletea.product.application.reservation.executor;

import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.OPEN_DATE;
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
class NotifyOpenScheduleExecutorTest {

    @InjectMocks
    private NotifyOpenScheduleExecutor notifyOpenScheduleExecutor;

    @Mock
    private ProductScheduleNotificationService productScheduleNotificationService;

    @Mock
    private ProductChangeHistoryRepository productChangeHistoryRepository;

    @Test
    @DisplayName("supports()는 NOTIFY_OPEN_SCHEDULE 타입을 반환한다.")
    void supports_returnsNotifyOpenScheduleType() {
        // when
        ReservationCommandType result = notifyOpenScheduleExecutor.supports();

        // then
        assertThat(result).isEqualTo(ReservationCommandType.NOTIFY_OPEN_SCHEDULE);
    }

    @Test
    @DisplayName("성공적으로 오픈 일정을 알림으로 발송하고 이력을 저장한다.")
    void execute_success() {
        // given
        String reservationId = "001";
        String productName = "[김연옌] 구독권";
        LocalDateTime openDate = LocalDateTime.of(2026, 7, 23, 12, 0);

        Map<String, Object> payload = new HashMap<>();
        payload.put(PRODUCT_NAME, productName);
        payload.put(OPEN_DATE, openDate.toString());

        ProductChangeReservation reservation = mock(ProductChangeReservation.class);
        given(reservation.getId()).willReturn(reservationId);
        given(reservation.getPayload()).willReturn(payload);

        given(productChangeHistoryRepository.save(any(ProductChangeHistory.class)))
            .willReturn(mock(ProductChangeHistory.class));

        // when
        notifyOpenScheduleExecutor.execute(reservation);

        // then
        then(productScheduleNotificationService).should()
            .notifyOpenSchedule(eq(reservationId), eq(productName), eq(openDate));
        then(productChangeHistoryRepository).should().save(any(ProductChangeHistory.class));
    }
}