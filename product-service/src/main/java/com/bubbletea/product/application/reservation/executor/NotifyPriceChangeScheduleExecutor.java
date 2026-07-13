package com.bubbletea.product.application.reservation.executor;

import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.CHANGED_PRICE;
import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.ORIGINAL_PRICE;
import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.PRICE_CHANGE_DATE;
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
public class NotifyPriceChangeScheduleExecutor implements ReservationCommandExecutor {

    private final ProductScheduleNotificationService productScheduleNotificationService;
    private final ProductChangeHistoryRepository productChangeHistoryRepository;

    @Override
    public ReservationCommandType supports() {
        return ReservationCommandType.NOTIFY_PRICE_CHANGE_SCHEDULE;
    }

    @Override
    public void execute(ProductChangeReservation reservation) {
        String productName = (String) reservation.getPayload().get(PRODUCT_NAME);
        LocalDateTime priceChangeDate = LocalDateTime.parse(
            (String) reservation.getPayload().get(PRICE_CHANGE_DATE));
        long originalPrice = ((Number) reservation.getPayload().get(ORIGINAL_PRICE)).longValue();
        long changedPrice = ((Number) reservation.getPayload().get(CHANGED_PRICE)).longValue();

        productScheduleNotificationService.notifyPriceChangeSchedule(
            productName, priceChangeDate, originalPrice, changedPrice
        );

        productChangeHistoryRepository.save(
            ProductChangeHistory.recordSuccess(reservation, reservation.getPayload()));
    }
}
