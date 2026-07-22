package com.bubbletea.product.presentation.swagger;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.product.application.product.dto.ProductDetailResponseDto;
import com.bubbletea.product.application.product.dto.ProductListResponseDto;
import com.bubbletea.product.application.product.dto.ProductResponseDto;
import com.bubbletea.product.presentation.dto.ProductRegisterRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Product", description = "상품 관리 API")
public interface ProductControllerDocs {

    @Operation(
        summary = "상품 등록",
        description = "새로운 상품을 등록합니다.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "상품 등록 성공",
                content = @Content(schema = @Schema(implementation = ProductResponseDto.class))
            )
        }
    )
    ApiResponse<ProductResponseDto> registerProduct(
        @Valid @org.springframework.web.bind.annotation.RequestBody
        ProductRegisterRequestDto request
    );

    @Operation(
        summary = "상품 목록 조회",
        description = "커서 기반 페이지네이션으로 상품 목록을 조회합니다. groupName으로 필터링 가능합니다.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = @Content(schema = @Schema(implementation = ProductListResponseDto.class))
            )
        }
    )
    ApiResponse<ProductListResponseDto> getProducts(
        @Parameter(description = "상품 그룹명 필터 (선택)")
        @RequestParam(required = false) String groupName,

        @Parameter(description = "커서 값 (이전 응답의 nextCursor, 첫 페이지는 생략)")
        @RequestParam(required = false) String cursor,

        @Parameter(description = "페이지 크기 (1~30, 기본값: 20)")
        @RequestParam(defaultValue = "20") @Min(1) @Max(30) int size
    );

    @Operation(
        summary = "필터 가능한 그룹 목록 조회",
        description = "상품 목록 조회 시 필터로 사용 가능한 그룹명 목록을 반환합니다.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = @Content(schema = @Schema(implementation = List.class))
            )
        }
    )
    ApiResponse<List<String>> getFilterableGroups();

    @Operation(
        summary = "상품 상세 조회",
        description = "상품 ID로 특정 상품의 상세 정보를 조회합니다.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = @Content(schema = @Schema(implementation = ProductDetailResponseDto.class))
            )
        }
    )
    ApiResponse<ProductDetailResponseDto> getProduct(
        @Parameter(description = "상품 ID", required = true, example = "prod-001")
        @PathVariable String productId
    );

    @Operation(
        summary = "상품 검색",
        description = "키워드로 상품을 검색합니다. 커서 기반 페이지네이션을 지원합니다.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "검색 성공",
                content = @Content(schema = @Schema(implementation = ProductListResponseDto.class))
            )
        }
    )
    ApiResponse<ProductListResponseDto> searchProducts(
        @Parameter(description = "검색 키워드", required = true, example = "이름")
        @RequestParam String keyword,

        @Parameter(description = "커서 값 (선택)")
        @RequestParam(required = false) String cursor,

        @Parameter(description = "페이지 크기 (1~30, 기본값: 20)")
        @RequestParam(defaultValue = "20") @Min(1) @Max(30) int size
    );
}
