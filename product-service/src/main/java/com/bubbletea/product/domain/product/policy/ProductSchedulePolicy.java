package com.bubbletea.product.domain.product.policy;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.domain.exception.ProductErrorCode;
import java.time.LocalDateTime;
import lombok.experimental.UtilityClass;


@UtilityClass
public class ProductSchedulePolicy {

    private final int MINIMUM_LEAD_TIME_DAYS = 20;

    public void validate(LocalDateTime runDate, LocalDateTime now) {
        LocalDateTime minimumOpenDate = now.plusDays(MINIMUM_LEAD_TIME_DAYS);
        if (runDate.isBefore(minimumOpenDate)) {
            throw new AppException(ProductErrorCode.INVALID_RUN_DATE);
        }
    }

}
