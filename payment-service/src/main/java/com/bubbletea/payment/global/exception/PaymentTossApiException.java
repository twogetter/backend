package com.bubbletea.payment.global.exception;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.common.exception.ErrorCode;

public class PaymentTossApiException extends AppException {
    public PaymentTossApiException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PaymentTossApiException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
