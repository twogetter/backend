package com.bubbletea.product.domain.product;

import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public class ProductFixture {

    private static final String DEFAULT_ID = "test-id";
    public static final Long DEFAULT_ARTIST_ID = 1L;
    private static final String DEFAULT_ARTIST_NAME = "아티스트";
    private static final String DEFAULT_GROUP_NAME = "아이돌그룹";
    private static final String DEFAULT_DESCRIPTION = "아티스트 구독권입니다.";
    private static final ProductStatus DEFAULT_STATUS = ProductStatus.ACTIVE;
    private static final boolean DEFAULT_DELETED = false;
    public static final String DEFAULT_IMAGE_URL = "https://image.example.com/product.jpg";
    public static final long DEFAULT_PRICE = 10000L;
    public static final LocalDateTime DEFAULT_OPEN_DATE = LocalDateTime.now().plusDays(30);
    public static final LocalDateTime DEFAULT_CREATED_AT = LocalDateTime.now();


    public static Product createDefault() {
        Product product =  create(DEFAULT_ID, DEFAULT_ARTIST_ID, DEFAULT_ARTIST_NAME, DEFAULT_GROUP_NAME);
        injectBaseFields(product, DEFAULT_ID);
        return product;
    }

    public static Product create(String id, String artistName, String groupName) {
        Product product = Product.schedule(
            DEFAULT_ARTIST_ID,
            artistName,
            groupName,
            DEFAULT_DESCRIPTION,
            DEFAULT_IMAGE_URL,
            DEFAULT_PRICE,
            DEFAULT_OPEN_DATE
        );
        injectBaseFields(product, id);
        return product;
    }

    /**
     * id, artistId, artistName, groupName 지정
     */
    public static Product create(
        String id, Long artistId, String artistName, String groupName
    ) {
        Product product = Product.schedule(
            artistId,
            artistName,
            groupName,
            DEFAULT_DESCRIPTION,
            DEFAULT_IMAGE_URL,
            DEFAULT_PRICE,
            DEFAULT_OPEN_DATE
        );
        injectBaseFields(product, id);
        return product;
    }

    /**
     * artistId, openDate 지정 (예약 스케줄 테스트용)
     */
    public static Product create(Long artistId, LocalDateTime openDate) {
        Product product = Product.schedule(
            artistId,
            DEFAULT_ARTIST_NAME,
            DEFAULT_GROUP_NAME,
            DEFAULT_DESCRIPTION,
            DEFAULT_IMAGE_URL,
            DEFAULT_PRICE,
            openDate
        );
        injectBaseFields(product, DEFAULT_ID);
        return product;
    }

    /**
     * 모든 필드 직접 지정
     */
    public static Product create(
        String id, Long artistId, String artistName, String groupName,
        String description, String imageUrl, long price, LocalDateTime openDate
    ) {
        Product product = Product.schedule(
            artistId, artistName, groupName, description, imageUrl, price, openDate
        );
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    /**
     * ACTIVE 상태의 Product 생성 (activate() 도메인 메서드를 직접 호출해서 실제 상태 전이 반영)
     */
    public static Product createActive() {
        Product product = create(
            DEFAULT_ID, DEFAULT_ARTIST_ID, DEFAULT_ARTIST_NAME, DEFAULT_GROUP_NAME
        );
        product.activate();
        return product;
    }

    public static Product createActive(String id, String artistName, String groupName) {
        Product product = create(id, artistName, groupName);
        product.activate();
        return product;
    }

    /**
     * INACTIVE 상태의 Product 생성
     */
    public static Product createInactive() {
        Product product = create(
            DEFAULT_ID, DEFAULT_ARTIST_ID, DEFAULT_ARTIST_NAME, DEFAULT_GROUP_NAME
        );
        product.deactivate();
        return product;
    }

    /**
     * 삭제된 Product 생성
     */
    public static Product createDeleted() {
        Product product = create(
            DEFAULT_ID, DEFAULT_ARTIST_ID, DEFAULT_ARTIST_NAME, DEFAULT_GROUP_NAME
        );
        product.markDeleted();
        return product;
    }

    private static void injectBaseFields(Product product, String id) {
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "createdAt", DEFAULT_CREATED_AT);
    }


    private ProductFixture() {
    }
}
