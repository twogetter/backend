package com.bubbletea.product.infrastructure.mongo.reservation;

import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ReservationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

interface ProductChangeReservationMongoRepository extends MongoRepository<ProductChangeReservation, String> {

    boolean existsByProductIdAndStatus(
        String productId, ReservationStatus status
    );
}
