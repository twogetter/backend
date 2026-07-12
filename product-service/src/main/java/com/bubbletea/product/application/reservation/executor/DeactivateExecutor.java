package com.bubbletea.product.application.reservation.executor;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.domain.history.ProductChangeHistory;
import com.bubbletea.product.domain.history.ProductChangeHistoryRepository;
import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductRepository;
import com.bubbletea.product.domain.exception.ProductErrorCode;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class DeactivateExecutor implements ReservationCommandExecutor {

    private final ProductRepository productRepository;
    private final ProductChangeHistoryRepository productChangeHistoryRepository;

    @Override
    public ReservationCommandType supports() {
        return ReservationCommandType.DEACTIVATE;
    }

    @Override
    public void execute(ProductChangeReservation reservation) {
        Product product = productRepository.findById(reservation.getProductId())
            .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));

        product.deactivate();
        productRepository.save(product);

        productChangeHistoryRepository.save(
            ProductChangeHistory.recordSuccess(
                reservation, Map.of("status", product.getStatus().name()))
        );
    }
}
