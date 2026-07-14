package com.bubbletea.product.application.reservation;


import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.domain.exception.ProductErrorCode;
import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductRepository;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import com.bubbletea.product.presentation.dto.ProductDeletionReservationRequestDto;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductReservationService {

    private static final long NOTIFICATION_LEAD_DAYS = 3L;

    private final ProductRepository productRepository;
    private final ProductChangeReservationRepository productChangeReservationRepository;

    public void reserveDeletion(String productId, ProductDeletionReservationRequestDto request) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (productChangeReservationRepository.existsByProductIdAndCommandType(
            productId, ReservationCommandType.DELETION)
        ) {
            throw new AppException(ProductErrorCode.ALREADY_RESERVED);
        }

        LocalDateTime deleteDate = request.deleteDate();
        product.verifySchedulability(deleteDate);
        LocalDateTime notifyAt = deleteDate.minusDays(NOTIFICATION_LEAD_DAYS);

        productChangeReservationRepository.saveAll(List.of(
                ProductChangeReservation.ofNotifyDeletionSchedule(
                    productId, notifyAt, product.getName(), deleteDate),
                ProductChangeReservation.ofDeletion(productId, deleteDate)
            )
        );
    }

}
