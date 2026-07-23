package com.bubbletea.product.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.application.exception.ProductApplicationErrorCode;
import com.bubbletea.product.application.product.dto.ProductDetailResponseDto;
import com.bubbletea.product.application.product.dto.ProductListResponseDto;
import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductFixture;
import com.bubbletea.product.domain.product.ProductRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

    @Nested
    @DisplayName("상품 검색 테스트")
    class SearchProductsTest {

        @Test
        @DisplayName("keyword가 null이면 INVALID_SEARCH_KEYWORD 오류를 던진다.")
        void searchProducts_fail_nullKeyword() {
            // given
            String keyword = null;

            // when & then
            AppException exception = assertThrows(AppException.class, () ->
                productQueryService.searchProducts(keyword, null, 10));
            assertThat(exception.getErrorCode())
                .isEqualTo(ProductApplicationErrorCode.INVALID_SEARCH_KEYWORD);

            then(productRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("keyword가 빈 문자열이면 INVALID_SEARCH_KEYWORD 오류를 던진다.")
        void searchProducts_fail_blankKeyword() {
            // given
            String keyword = "   ";

            // when & then
            AppException exception = assertThrows(AppException.class, () ->
                productQueryService.searchProducts(keyword, null, 10));
            assertThat(exception.getErrorCode())
                .isEqualTo(ProductApplicationErrorCode.INVALID_SEARCH_KEYWORD);

            then(productRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("cursor 없이 검색하면 첫 페이지 결과를 반환한다.")
        void searchProducts_success_firstPage() {
            // given
            int size = 1;
            List<Product> mockProducts = List.of(
                ProductFixture.create("id1", "아티1", "아이돌"),
                ProductFixture.create("id2", "아티2", "아이돌")
            );
            given(productRepository.searchActiveProducts(any()))
                .willReturn(mockProducts);

            // when
            ProductListResponseDto result =
                productQueryService.searchProducts("아티", null, size);

            // then
            assertThat(result.items()).hasSize(size);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isNotNull();

            then(productRepository).should().searchActiveProducts(any());
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 목록을 반환한다.")
        void searchProducts_success_emptyResult() {
            // given
            given(productRepository.searchActiveProducts(any()))
                .willReturn(Collections.emptyList());

            // when
            ProductListResponseDto result =
                productQueryService.searchProducts("없음", null, 10);

            // then
            assertThat(result.items()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();

            then(productRepository).should().searchActiveProducts(any());
        }

        @Test
        @DisplayName("유효한 cursor로 검색하면 다음 페이지 결과를 반환한다.")
        void searchProducts_success_withCursor() {
            // given
            int size = 1;
            String cursor = CursorCodec.encode("아티1", "id1");

            List<Product> mockProducts = List.of(
                ProductFixture.create("id2", "아티2", "아이돌"),
                ProductFixture.create("id3", "아티3", "아이돌")
            );
            given(productRepository.searchActiveProducts(any()))
                .willReturn(mockProducts);

            // when
            ProductListResponseDto result =
                productQueryService.searchProducts("아티", cursor, size);

            // then
            assertThat(result.items()).hasSize(size);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isNotNull();

            then(productRepository).should().searchActiveProducts(any());
        }

    }

    @Nested
    @DisplayName("상품 상세 조회 테스트")
    class GetProductDetailTest {

        @Test
        @DisplayName("존재하는 productId로 조회하면 상세 응답 DTO를 반환한다.")
        void existingProductId_returnsDetailDto() {
            // given
            String productId = "product-1";
            Product product = ProductFixture.createDefault();

            given(productRepository.findActiveProductById(productId))
                .willReturn(Optional.of(product));

            // when
            ProductDetailResponseDto result = productQueryService.getProductDetail(productId);

            // then
            assertThat(result).isNotNull();
            then(productRepository).should().findActiveProductById(productId);
        }

        @Test
        @DisplayName("존재하지 않는 productId로 조회하면 AppException을 던진다.")
        void nonExistingProductId_throwsAppException() {
            // given
            String productId = "not-existing";

            given(productRepository.findActiveProductById(productId))
                .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productQueryService.getProductDetail(productId))
                .isInstanceOf(AppException.class);

            then(productRepository).should().findActiveProductById(productId);
        }
    }

    @Nested
    @DisplayName("pid로 상품 상세 조회 테스트")
    class GetProductDetailByPidTest {

        @Test
        @DisplayName("존재하는 pid로 조회하면 상세 응답 DTO를 반환한다.")
        void existingPid_returnsDetailDto() {
            // given
            Long pid = 1L;
            Product product = ProductFixture.createDefault();

            given(productRepository.findByPid(pid))
                .willReturn(Optional.of(product));

            // when
            ProductDetailResponseDto result = productQueryService.getProductDetailByPid(pid);

            // then
            assertThat(result).isNotNull();
            then(productRepository).should().findByPid(pid);
        }

        @Test
        @DisplayName("존재하지 않는 pid로 조회하면 AppException을 던진다.")
        void nonExistingPid_throwsAppException() {
            // given
            Long pid = 999L;

            given(productRepository.findByPid(pid))
                .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productQueryService.getProductDetailByPid(pid))
                .isInstanceOf(AppException.class);

            then(productRepository).should().findByPid(pid);
        }
    }

    @Nested
    @DisplayName("필터용 그룹명 목록 조회 테스트")
    class GetFilterableGroupNamesTest {

        @Test
        @DisplayName("그룹명 목록을 반환한다.")
        void returnsGroupNameList() {
            // given
            List<String> groupNames = List.of("아이돌", "아이브", "아일릿");

            given(productRepository.findDistinctActiveGroupNames())
                .willReturn(groupNames);

            // when
            List<String> result = productQueryService.getFilterableGroupNames();

            // then
            assertThat(result).containsExactlyElementsOf(groupNames);
            then(productRepository).should().findDistinctActiveGroupNames();
        }

        @Test
        @DisplayName("활성화된 상품이 없으면 빈 목록을 반환한다.")
        void noActiveProducts_returnsEmptyList() {
            // given
            given(productRepository.findDistinctActiveGroupNames())
                .willReturn(Collections.emptyList());

            // when
            List<String> result = productQueryService.getFilterableGroupNames();

            // then
            assertThat(result).isEmpty();
            then(productRepository).should().findDistinctActiveGroupNames();
        }
    }

}
