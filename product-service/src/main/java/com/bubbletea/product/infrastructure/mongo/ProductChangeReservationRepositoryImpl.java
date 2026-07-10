package com.bubbletea.product.infrastructure.mongo;

import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProductChangeReservationRepositoryImpl implements ProductChangeReservationRepository {

    private final ProductChangeReservationMongoRepository productChangeReservationMongoRepository;

    @Override
    public void saveAll(List<ProductChangeReservation> reservations) {
        productChangeReservationMongoRepository.saveAll(reservations);
    }
}
