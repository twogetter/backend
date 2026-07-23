package com.bubbletea.product.application.reservation.executor;

import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.DELETION_DATE;
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
class NotifyDeletionScheduleExecutorTest {

    @InjectMocks
    private NotifyDeletionScheduleExecutor notifyDeletionScheduleExecutor;

    @Mock
    private ProductScheduleNotificationService productScheduleNotificationService;

    @Mock
    private ProductChangeHistoryRepository productChangeHistoryRepository;

    @Test
    @DisplayName("supports()는 NOTIFY_DELETION_SCHEDULE 타입을 반환한다")
    void supports_returnsNotifyDeletionScheduleType() {
        // when
        ReservationCommandType result = notifyDeletionScheduleExecutor.supports();

        // then
        assertThat(result).isEqualTo(ReservationCommandType.NOTIFY_DELETION_SCHEDULE);
    }

    @Test
    @DisplayName("성공적으로 삭제 일정을 알림으로 발송하고 이력을 저장한다.")
    void execute_success() {
        // given
        String reservationId = "001";
        String productName = "[김연옌] 구독권";
        LocalDateTime deletionDate = LocalDateTime.of(2026, 7, 23, 12, 0);

        Map<String, Object> payload = new HashMap<>();
        payload.put(PRODUCT_NAME, productName);
        payload.put(DELETION_DATE, deletionDate.toString());

        ProductChangeReservation reservation = mock(ProductChangeReservation.class);
        given(reservation.getId()).willReturn(reservationId);
        given(reservation.getPayload()).willReturn(payload);

        given(productChangeHistoryRepository.save(any(ProductChangeHistory.class)))
            .willReturn(mock(ProductChangeHistory.class));

        // when
        notifyDeletionScheduleExecutor.execute(reservation);

        // then
        then(productScheduleNotificationService).should()
            .notifyDeletionSchedule(eq(reservationId), eq(productName), eq(deletionDate));
        then(productChangeHistoryRepository).should().save(any(ProductChangeHistory.class));
    }
}