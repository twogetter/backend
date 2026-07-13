package com.bubbletea.product.infrastructure.mongo.reservation;

import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import org.springframework.data.mongodb.repository.MongoRepository;

interface ProductChangeReservationMongoRepository extends MongoRepository<ProductChangeReservation, String> {
}
