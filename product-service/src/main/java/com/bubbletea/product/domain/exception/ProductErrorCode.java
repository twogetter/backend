package com.bubbletea.product.domain.exception;

import com.bubbletea.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {

    INVALID_RUN_DATE(
        HttpStatus.BAD_REQUEST,
        "PRODUCT-INVALID-RUN-DATE",
        "상품 실행 예약은 최소 20일 전에 신청되어야 합니다."
    ),
    DUPLICATE_ARTIST_PRODUCT(
        HttpStatus.CONFLICT,
        "PRODUCT-CONFLICT-DUPLICATE_ARTIST",
        "해당 아티스트는 이미 상품으로 등록되어 있습니다."
    ),
    ALREADY_RESERVED(
        HttpStatus.CONFLICT,
        "PRODUCT-CONFLICT-ALREADY_RESERVED",
        "예약이 이미 등록되어 있습니다."
    ),
    ALREADY_DELETED(
        HttpStatus.CONFLICT,
        "PRODUCT-CONFLICT-ALREADY_DELETED",
        "이미 삭제된 상품은 상태를 변경할 수 없습니다."
    ),
    PRODUCT_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "PRODUCT-NOT_FOUND-ID",
        "해당 ID의 상품을 찾을 수 없습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
