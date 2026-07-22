package com.bubbletea.product.presentation.swagger;


import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.product.application.reservation.dto.ReservationDetailResponseDto;
import com.bubbletea.product.application.reservation.dto.ReservationListResponseDto;
import com.bubbletea.product.domain.reservation.ReservationCategory;
import com.bubbletea.product.domain.reservation.ReservationStatus;
import com.bubbletea.product.presentation.dto.ProductDeletionReservationRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "ProductReservation", description = "상품 예약 관리 API")
public interface ProductReservationControllerDocs {

    @Operation(
        summary = "예약 목록 조회",
        description = "카테고리와 상태로 필터링하여 예약 목록을 조회합니다. scheduledAt 기준 오름차순 정렬이 기본값입니다.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = @Content(schema = @Schema(implementation = ReservationListResponseDto.class))
            )
        }
    )
    ApiResponse<ReservationListResponseDto> getReservations(
        @Parameter(
            description = "예약 카테고리 필터 (선택)",
            schema = @Schema(implementation = ReservationCategory.class)
        )
        @RequestParam(required = false) ReservationCategory category,

        @Parameter(
            description = "예약 상태 필터 (선택)",
            schema = @Schema(implementation = ReservationStatus.class)
        )
        @RequestParam(required = false) ReservationStatus status,

        @Parameter(hidden = true) Pageable pageable
    );

    @Operation(
        summary = "예약 상세 조회",
        description = "예약 ID로 특정 예약의 상세 정보를 조회합니다.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = @Content(schema = @Schema(implementation = ReservationDetailResponseDto.class))
            )
        }
    )
    ApiResponse<ReservationDetailResponseDto> getReservation(
        @Parameter(description = "예약 ID", required = true, example = "rsv-001")
        @PathVariable String reservationId
    );

    @Operation(
        summary = "상품 삭제 예약 등록",
        description = "지정된 날짜에 상품이 삭제되도록 예약합니다.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "예약 등록 성공")
        }
    )
    ApiResponse<Void> reserveDeletion(
        @Parameter(description = "상품 ID", required = true, example = "prod-001")
        @PathVariable String productId,

        @Valid @org.springframework.web.bind.annotation.RequestBody
        ProductDeletionReservationRequestDto request
    );
}
