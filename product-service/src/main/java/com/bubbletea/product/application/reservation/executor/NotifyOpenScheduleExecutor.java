package com.bubbletea.product.application.reservation.executor;

import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.OPEN_DATE;
import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.PRODUCT_NAME;

import com.bubbletea.product.application.event.ProductScheduleNotificationService;
import com.bubbletea.product.domain.history.ProductChangeHistory;
import com.bubbletea.product.domain.history.ProductChangeHistoryRepository;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotifyOpenScheduleExecutor implements ReservationCommandExecutor {

    private final ProductScheduleNotificationService productScheduleNotificationService;
    private final ProductChangeHistoryRepository productChangeHistoryRepository;

    @Override
    public ReservationCommandType supports() {
        return ReservationCommandType.NOTIFY_OPEN_SCHEDULE;
    }

    @Override
    public void execute(ProductChangeReservation reservation) {
        String productName = (String) reservation.getPayload().get(PRODUCT_NAME);
        LocalDateTime openDate = LocalDateTime.parse(
            (String) reservation.getPayload().get(OPEN_DATE));

        productScheduleNotificationService.notifyOpenSchedule(
            reservation.getId(), productName, openDate);

        productChangeHistoryRepository.save(
            ProductChangeHistory.recordSuccess(reservation, reservation.getPayload()));
    }
}
