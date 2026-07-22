package com.bubbletea.product.presentation.internal;


import com.bubbletea.product.presentation.internal.dto.ProductResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "[Internal] Product", description = "내부 서비스 통신용 상품 API")
public interface ProductInternalControllerDocs {

    @Operation(
        summary = "상품 유효성 검증 정보 조회",
        description = "[Internal API] 내부 서비스에서 상품 유효성 검증 시 사용합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ProductResponseDto.class))
        )
    })
    ProductResponseDto getProduct(
        @Parameter(description = "상품 PID", required = true, example = "1")
        @PathVariable Long id
    );

}
