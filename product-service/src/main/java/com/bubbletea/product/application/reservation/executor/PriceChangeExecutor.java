package com.bubbletea.product.application.reservation.executor;

import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.BATCH_ID;
import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.CHANGED_PRICE;
import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.ORIGINAL_PRICE;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.domain.history.ProductChangeHistory;
import com.bubbletea.product.domain.history.ProductChangeHistoryRepository;
import com.bubbletea.product.domain.history.ProductPriceChangeHistory;
import com.bubbletea.product.domain.history.ProductPriceChangeHistoryRepository;
import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductRepository;
import com.bubbletea.product.domain.exception.ProductErrorCode;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PriceChangeExecutor implements ReservationCommandExecutor {

    private final ProductRepository productRepository;
    private final ProductChangeHistoryRepository productChangeHistoryRepository;
    private final ProductPriceChangeHistoryRepository productPriceChangeHistoryRepository;

    @Override
    public ReservationCommandType supports() {
        return ReservationCommandType.PRICE_CHANGE;
    }

    @Override
    public void execute(ProductChangeReservation reservation) {
        Product product = productRepository.findById(reservation.getProductId())
            .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));

        long originalPrice = product.getPrice();
        long changedPrice = ((Number) reservation.getPayload().get(CHANGED_PRICE)).longValue();
        String batchId = (String) reservation.getPayload().get(BATCH_ID);

        product.changePrice(changedPrice);
        productRepository.save(product);

        productPriceChangeHistoryRepository.save(
            ProductPriceChangeHistory.of(
                product.getId(), originalPrice, changedPrice, batchId, LocalDateTime.now())
        );

        productChangeHistoryRepository.save(
            ProductChangeHistory.recordSuccess(reservation, Map.of(
                ORIGINAL_PRICE, originalPrice,
                CHANGED_PRICE, changedPrice
            ))
        );
    }
}
