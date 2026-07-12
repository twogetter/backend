package com.bubbletea.product.application.reservation.executor;

import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ReservationCommandType;


public interface ReservationCommandExecutor {

    ReservationCommandType supports();

    void execute(ProductChangeReservation reservation);

}
