package com.bubbletea.payment.global.exception;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class PaymentException extends AppException {
    public PaymentException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PaymentException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
