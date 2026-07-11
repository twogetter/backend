package com.bubbletea.payment.global.exception;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class PaymentSystemException extends AppException {
    public PaymentSystemException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PaymentSystemException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
