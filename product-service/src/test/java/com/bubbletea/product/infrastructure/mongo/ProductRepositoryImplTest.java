package com.bubbletea.product.infrastructure.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductFixture;
import com.bubbletea.product.domain.product.ProductListCondition;
import com.bubbletea.product.domain.product.ProductRepository;
import com.bubbletea.product.domain.product.ProductSearchCondition;
import com.bubbletea.product.infrastructure.mongo.product.ProductRepositoryImpl;
import com.bubbletea.product.support.MongoAuditingTestConfig;
import com.bubbletea.product.support.ProductMongoOnlySupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;


@DataMongoTest
@Import({
    ProductRepositoryImpl.class,
    MongoAuditingTestConfig.class
})
public class ProductRepositoryImplTest extends ProductMongoOnlySupport {

    @Autowired
    private ProductRepository productRepository;

    private static final LocalDateTime OPEN_DATE = LocalDateTime.now().plusDays(30);

    private Product saveDefaultProduct(Long artistId, String artistName, String groupName) {
        return productRepository.save(
            ProductFixture.createDefaultByArtistInfo(artistId, artistName, groupName, OPEN_DATE)
        );
    }

    private Product saveActiveProduct(Long artistId, String artistName, String groupName) {
        Product product = ProductFixture
            .createActiveByArtistInfo(artistId, artistName, groupName, OPEN_DATE);
        return productRepository.save(product);
    }

    private Product saveDeletedProduct(Long artistId, String artistName, String groupName) {
        Product product = ProductFixture
            .createDeletedByArtistInfo(artistId, artistName, groupName, OPEN_DATE);
        return productRepository.save(product);
    }

    @Nested
    @DisplayName("save(), findById() 테스트")
    class SaveAndFindById {

        @Test
        @DisplayName("저장된 상품을 id로 조회할 수 있다.")
        void saveAndFindById_success() {
            // given
            Product product = ProductFixture.createDefault();

            // when
            Product saved = productRepository.save(product);
            Optional<Product> found = productRepository.findById(saved.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
            assertThat(found.get().getArtistName()).isEqualTo(saved.getArtistName());
        }

        @Test
        @DisplayName("존재하지 않는 id로 조회하면 빈 Optional을 반환한다.")
        void findById_notFound_returnsEmpty() {
            // when
            Optional<Product> found = productRepository.findById("not-existing-id");

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("저장 시 createdAt이 자동으로 설정된다.")
        void save_createdAtIsSetAutomatically() {
            // given
            Product product = ProductFixture.createDefault();

            // when
            Product saved = productRepository.save(product);

            // then
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("findByPid() 테스트")
    class FindByPid {

        @Test
        @DisplayName("존재하는 pid로 조회하면 상품을 반환한다.")
        void existingPid_returnsProduct() {
            // given
            Product saved = saveDefaultProduct(1L, "아티스트1", "아이돌");

            // when
            Optional<Product> found = productRepository.findByPid(saved.getPid());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getPid()).isEqualTo(saved.getPid());
        }

        @Test
        @DisplayName("존재하지 않는 pid로 조회하면 빈 Optional을 반환한다.")
        void nonExistingPid_returnsEmpty() {
            // when
            Optional<Product> found = productRepository.findByPid(999L);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByArtistIdAndDeletedFalse() 테스트")
    class ExistsByArtistIdAndDeletedFalse {

        @Test
        @DisplayName("삭제되지 않은 상품이 있으면 true를 반환한다.")
        void activeProductExists_returnsTrue() {
            // given
            saveDefaultProduct(1L, "아티스트1", "아이돌");

            // when
            boolean result = productRepository.existsByArtistIdAndDeletedFalse(1L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("삭제된 상품만 있으면 false를 반환한다.")
        void onlyDeletedProductExists_returnsFalse() {
            // given
            saveDeletedProduct(1L, "아티스트1", "아이돌");

            // when
            boolean result = productRepository.existsByArtistIdAndDeletedFalse(1L);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("상품이 없으면 false를 반환한다.")
        void noProduct_returnsFalse() {
            // when
            boolean result = productRepository.existsByArtistIdAndDeletedFalse(999L);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findActiveProductById() 테스트")
    class FindActiveProductById {

        @Test
        @DisplayName("ACTIVE 상태의 상품을 id로 조회할 수 있다.")
        void activeProduct_returnsProduct() {
            // given
            Product saved = saveActiveProduct(1L, "아티스트1", "아이돌");

            // when
            Optional<Product> found = productRepository.findActiveProductById(saved.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("PENDING_OPEN 상태의 상품은 조회되지 않는다.")
        void pendingOpenProduct_returnsEmpty() {
            Product saved = saveDefaultProduct(1L, "아티스트1", "아이돌");

            // when
            Optional<Product> found = productRepository.findActiveProductById(saved.getId());

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("삭제된 상품은 조회되지 않는다.")
        void deletedProduct_returnsEmpty() {
            // given
            Product saved = saveDeletedProduct(1L, "아티스트1", "아이돌");

            // when
            Optional<Product> found = productRepository.findActiveProductById(saved.getId());

            // then
            assertThat(found).isEmpty();
        }

    }

    @Nested
    @DisplayName("findActiveProducts() 테스트")
    class FindActiveProducts {

        @Test
        @DisplayName("ACTIVE 상태의 상품만 조회된다.")
        void returnsOnlyActiveProducts() {
            // given
            saveActiveProduct(1L, "아티스트1", "아이돌");
            saveDefaultProduct(2L, "아티스트2", "아이돌");
            saveDeletedProduct(3L, "아티스트3", "아이돌");

            ProductListCondition condition = new ProductListCondition(
                null, null, null, 10);

            // when
            List<Product> result = productRepository.findActiveProducts(condition);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getArtistName()).isEqualTo("아티스트1");
        }

        @Test
        @DisplayName("groupName 필터가 있으면 해당 그룹의 상품만 조회된다.")
        void groupNameFilter_returnsFilteredProducts() {
            // given
            saveActiveProduct(1L, "아티스트1", "아이돌");
            saveActiveProduct(2L, "아티스트2", "나가수");
            saveActiveProduct(3L, "아티스트3", "아이돌");

            ProductListCondition condition =
                new ProductListCondition("아이돌", null, null, 10);

            // when
            List<Product> result = productRepository.findActiveProducts(condition);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(p -> "아이돌".equals(p.getGroupName()));
        }

        @Test
        @DisplayName("groupName이 null인 솔로 아티스트는 artistName으로 필터된다.")
        void soloArtist_filteredByArtistName() {
            // given
            saveActiveProduct(1L, "솔로아티스트", null);
            saveActiveProduct(2L, "아티스트2", "아이돌");

            ProductListCondition condition =
                new ProductListCondition("솔로아티스트", null, null, 10);

            // when
            List<Product> result = productRepository.findActiveProducts(condition);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getArtistName()).isEqualTo("솔로아티스트");
        }

        @Test
        @DisplayName("커서 없이 조회하면 artistName 오름차순으로 정렬된다.")
        void noCursor_returnsSortedByArtistName() {
            // given
            saveActiveProduct(1L, "다_아티스트", "아이돌");
            saveActiveProduct(2L, "가_아티스트", "아이돌");
            saveActiveProduct(3L, "나_아티스트", "아이돌");

            ProductListCondition condition = new ProductListCondition(
                null, null, null, 10);

            // when
            List<Product> result = productRepository.findActiveProducts(condition);

            // then
            assertThat(result).extracting(Product::getArtistName)
                .containsExactly("가_아티스트", "나_아티스트", "다_아티스트");
        }

        @Test
        @DisplayName("size + 1개를 조회해서 hasNext 판단이 가능하다.")
        void limitIsSizePlusOne() {
            // given
            saveActiveProduct(1L, "아티스트1", "아이돌");
            saveActiveProduct(2L, "아티스트2", "아이돌");
            saveActiveProduct(3L, "아티스트3", "아이돌");

            int size = 2;
            ProductListCondition condition = new ProductListCondition(
                null, null, null, size);

            // when
            List<Product> result = productRepository.findActiveProducts(condition);

            assertThat(result).hasSize(size + 1);
        }

        @Test
        @DisplayName("커서 기반으로 다음 페이지를 조회한다.")
        void withCursor_returnsNextPage() {
            // given
            saveActiveProduct(1L, "가아티스트", "아이돌");
            Product second = saveActiveProduct(2L, "나아티스트", "아이돌");
            saveActiveProduct(3L, "다아티스트", "아이돌");

            ProductListCondition condition = new ProductListCondition(
                null,
                second.getArtistName(),
                second.getId(),
                10
            );

            // when
            List<Product> result = productRepository.findActiveProducts(condition);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getArtistName()).isEqualTo("다아티스트");
        }

        @Test
        @DisplayName("같은 artistName에서 커서 id 이후 상품을 조회한다")
        void withCursor_sameArtistName_returnsAfterCursorId() {
            // given
            Product first = saveActiveProduct(1L, "동명아티스트", "아이돌");
            Product second = saveActiveProduct(2L, "동명아티스트", "아이돌");
            Product third = saveActiveProduct(3L, "동명아티스트", "아이돌");

            ProductListCondition condition = new ProductListCondition(
                null,
                second.getArtistName(),
                second.getId(),
                10
            );

            // when
            List<Product> result = productRepository.findActiveProducts(condition);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getId()).isEqualTo(third.getId());
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 목록을 반환한다.")
        void noActiveProducts_returnsEmptyList() {
            // given
            ProductListCondition condition = new ProductListCondition(
                null, null, null, 10);

            // when
            List<Product> result = productRepository.findActiveProducts(condition);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findDistinctActiveGroupNames() 테스트")
    class FindDistinctActiveGroupNames {

        @Test
        @DisplayName("ACTIVE 상품의 중복 없는 groupName 목록을 반환한다")
        void returnsDistinctGroupNames() {
            // given
            saveActiveProduct(1L, "아티스트1", "아이돌");
            saveActiveProduct(2L, "아티스트2", "아이돌");
            saveActiveProduct(3L, "아티스트3", "나가수");

            // when
            List<String> result = productRepository.findDistinctActiveGroupNames();

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder("아이돌", "나가수");
        }

        @Test
        @DisplayName("groupName이 null인 상품은 목록에 포함되지 않는다")
        void nullGroupName_isExcluded() {
            // given
            saveActiveProduct(1L, "아티스트1", "아이돌");
            saveActiveProduct(2L, "솔로아티스트", null);

            // when
            List<String> result = productRepository.findDistinctActiveGroupNames();

            // then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly("아이돌");
            assertThat(result).doesNotContainNull();
        }

        @Test
        @DisplayName("ACTIVE 상태가 아닌 상품의 groupName은 포함되지 않는다.")
        void nonActiveProducts_groupNamesExcluded() {
            // given
            saveActiveProduct(1L, "아티스트1", "아이돌");
            saveDefaultProduct(2L, "아티스트2", "나가수");
            saveDeletedProduct(3L, "아티스트3", "아이브");

            // when
            List<String> result = productRepository.findDistinctActiveGroupNames();

            // then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly("아이돌");
        }

        @Test
        @DisplayName("ACTIVE 상품이 없으면 빈 목록을 반환한다.")
        void noActiveProducts_returnsEmptyList() {
            // when
            List<String> result = productRepository.findDistinctActiveGroupNames();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchActiveProducts() 테스트")
    class SearchActiveProducts {

        @Test
        @DisplayName("artistName으로 검색하면 해당 상품이 조회된다.")
        void searchByArtistName_returnsMatchingProducts() {
            // given
            saveActiveProduct(1L, "버블티아티스트", "아이돌");
            saveActiveProduct(2L, "다른아티스트", "아이돌");

            ProductSearchCondition condition =
                new ProductSearchCondition("버블티", null, null, 10);

            // when
            List<Product> result = productRepository.searchActiveProducts(condition);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getArtistName()).isEqualTo("버블티아티스트");
        }

        @Test
        @DisplayName("groupName으로 검색하면 해당 상품이 조회된다")
        void searchByGroupName_returnsMatchingProducts() {
            // given
            saveActiveProduct(1L, "아티스트1", "버블팝");
            saveActiveProduct(2L, "아티스트2", "다른그룹");

            ProductSearchCondition condition =
                new ProductSearchCondition("버블", null, null, 10);

            // when
            List<Product> result = productRepository.searchActiveProducts(condition);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getGroupName()).isEqualTo("버블팝");
        }

        @Test
        @DisplayName("검색은 대소문자를 구분하지 않는다.")
        void search_caseInsensitive() {
            // given
            saveActiveProduct(1L, "BubbleTea", "아이돌");

            ProductSearchCondition condition =
                new ProductSearchCondition("bubbletea", null, null, 10);

            // when
            List<Product> result = productRepository.searchActiveProducts(condition);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("정규식 특수문자가 포함된 키워드도 안전하게 검색된다.")
        void searchWithRegexSpecialCharacters_searchesSafely() {
            // given
            saveActiveProduct(1L, "아티스트.특수", "아이돌");
            saveActiveProduct(2L, "아티스트일반", "아이돌");

            ProductSearchCondition condition =
                new ProductSearchCondition("아티스트.특수", null, null, 10);

            // when
            List<Product> result = productRepository.searchActiveProducts(condition);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getArtistName()).isEqualTo("아티스트.특수");
        }

        @Test
        @DisplayName("ACTIVE 상태가 아닌 상품은 검색되지 않는다.")
        void nonActiveProducts_notIncludedInSearch() {
            // given
            saveActiveProduct(1L, "버블티아티스트", "아이돌");
            saveDefaultProduct(2L, "버블티아티스트2", "아이돌");
            saveDeletedProduct(3L, "버블티아티스트3", "아이돌");

            ProductSearchCondition condition =
                new ProductSearchCondition("버블티", null, null, 10);

            // when
            List<Product> result = productRepository.searchActiveProducts(condition);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("커서 기반으로 다음 페이지를 검색한다.")
        void withCursor_returnsNextPage() {
            // given
            saveActiveProduct(1L, "가_버블아티스트", "아이돌");
            Product second = saveActiveProduct(2L, "나_버블아티스트", "아이돌");
            saveActiveProduct(3L, "다_버블아티스트", "아이돌");

            ProductSearchCondition condition = new ProductSearchCondition(
                "버블",
                second.getArtistName(),
                second.getId(),
                10
            );

            // when
            List<Product> result = productRepository.searchActiveProducts(condition);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getArtistName()).isEqualTo("다_버블아티스트");
        }

        @Test
        @DisplayName("size + 1개를 조회해서 hasNext 판단이 가능하다.")
        void limitIsSizePlusOne() {
            // given
            saveActiveProduct(1L, "버블아티스트1", "아이돌");
            saveActiveProduct(2L, "버블아티스트2", "아이돌");
            saveActiveProduct(3L, "버블아티스트3", "아이돌");

            int size = 2;
            ProductSearchCondition condition =
                new ProductSearchCondition("버블", null, null, size);

            // when
            List<Product> result = productRepository.searchActiveProducts(condition);

            // then
            assertThat(result).hasSize(size + 1);
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 목록을 반환한다.")
        void noMatchingProducts_returnsEmptyList() {
            // given
            saveActiveProduct(1L, "아티스트1", "아이돌");

            ProductSearchCondition condition =
                new ProductSearchCondition("없는키워드", null, null, 10);

            // when
            List<Product> result = productRepository.searchActiveProducts(condition);

            // then
            assertThat(result).isEmpty();
        }
    }
}
