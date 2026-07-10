package com.bubbletea.product.domain.reservation;

import java.util.List;

public interface ProductChangeReservationRepository {

    void saveAll(List<ProductChangeReservation> reservations);
}
