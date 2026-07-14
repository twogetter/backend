package com.bubbletea.product.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bubbletea.product.application.product.dto.ProductListResponseDto;
import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductFixture;
import com.bubbletea.product.domain.product.ProductRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProductQueryServiceTest {

    @InjectMocks
    private ProductQueryService productQueryService;

    @Mock
    private ProductRepository productRepository;

    @Nested
    @DisplayName("상품 목록 조회 테스트")
    class GetProductListTest {

        @Test
        @DisplayName("커서 없이 첫 페이지를 조회하면 상품 목록과 다음 커서를 반환한다.")
        void getProductList_success_firstPage() {
            // given
            int size = 1;
            List<Product> mockProducts = List.of(
                ProductFixture.create("id1", "아티1", "아이돌"),
                ProductFixture.create("id2", "아티2", "아이돌")
            );
            given(productRepository.findActiveProducts(any()))
                .willReturn(mockProducts);

            // when
            ProductListResponseDto result =
                productQueryService.getProductList("아이돌", null, size);

            // then
            assertThat(result.items()).hasSize(size);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isNotNull();

            then(productRepository).should().findActiveProducts(any());
        }

        @Test
        @DisplayName("마지막 페이지 조회 시 hasNext는 false이고 nextCursor는 null이다.")
        void getProductList_success_lastPage() {
            // given
            int size = 20;
            List<Product> mockProducts = List.of(
                ProductFixture.create("id1", "아티1", "아이돌"),
                ProductFixture.create("id2", "아티2", "아이돌")
            );
            given(productRepository.findActiveProducts(any()))
                .willReturn(mockProducts);

            // when
            ProductListResponseDto result =
                productQueryService.getProductList("아이돌", null, size);

            // then
            assertThat(result.items()).hasSize(2);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        @DisplayName("유효한 커서로 다음 페이지를 조회하면 정상적으로 반환한다.")
        void getProductList_success_withValidCursor() {
            // given
            int size = 1;
            String cursor = CursorCodec.encode("아티1", "id1");
            List<Product> mockProducts = List.of(
                ProductFixture.create("id2", "아티2", "아이돌"),
                ProductFixture.create("id3", "아티3", "아이돌")
            );
            given(productRepository.findActiveProducts(any()))
                .willReturn(mockProducts);

            // when
            ProductListResponseDto result =
                productQueryService.getProductList("아이돌", cursor, size);

            // then
            assertThat(result.items()).hasSize(size);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isNotNull();
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 목록을 반환한다.")
        void getProductList_success_emptyResult() {
            // given
            given(productRepository.findActiveProducts(any()))
                .willReturn(Collections.emptyList());

            // when
            ProductListResponseDto result =
                productQueryService.getProductList("아이돌", null, 20);

            // then
            assertThat(result.items()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        @DisplayName("요청된 groupName 조건이 없어도 전체 조회가 가능하다.")
        void getProductList_success_withoutGroupName() {
            // given
            List<Product> mockProducts = List.of(
                ProductFixture.create("id1", "아티1", "아이돌"),
                ProductFixture.create("id2", "아티2", "아이돌")
            );
            given(productRepository.findActiveProducts(any()))
                .willReturn(mockProducts);

            // when
            ProductListResponseDto result =
                productQueryService.getProductList(null, null, 20);

            // then
            assertThat(result.items()).hasSize(2);
            assertThat(result.hasNext()).isFalse();
        }
    }
}
