package com.bubbletea.product.domain.product.policy;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.domain.exception.ProductErrorCode;
import java.time.LocalDateTime;
import lombok.experimental.UtilityClass;


@UtilityClass
public class ProductOpenSchedulePolicy {

    private final int MINIMUM_LEAD_TIME_DAYS = 20;

    public void validate(LocalDateTime openDate, LocalDateTime now) {
        LocalDateTime minimumOpenDate = now.plusDays(MINIMUM_LEAD_TIME_DAYS);
        if (openDate.isBefore(minimumOpenDate)) {
            throw new AppException(ProductErrorCode.INVALID_OPEN_DATE);
        }
    }
}
