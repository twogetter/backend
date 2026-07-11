package com.bubbletea.product.domain.product.exception;

import com.bubbletea.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {

    INVALID_OPEN_DATE(
        HttpStatus.BAD_REQUEST,
        "PRODUCT-INVALID-OPEN_DATE",
        "상품 오픈 예약은 최소 20일 전에 신청해야 합니다."
    ),
    DUPLICATE_ARTIST_PRODUCT(
        HttpStatus.CONFLICT,
        "PRODUCT-CONFLICT-DUPLICATE_ARTIST",
        "해당 아티스트는 이미 상품으로 등록되어 있습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
