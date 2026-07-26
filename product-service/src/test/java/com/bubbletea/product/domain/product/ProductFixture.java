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


    public static Product createDefaultMock() {
        Product product = Product.schedule(
            DEFAULT_ARTIST_ID,
            DEFAULT_ARTIST_NAME,
            DEFAULT_GROUP_NAME,
            DEFAULT_DESCRIPTION,
            DEFAULT_IMAGE_URL,
            DEFAULT_PRICE,
            DEFAULT_OPEN_DATE
        );
        injectBaseFields(product, DEFAULT_ID);
        return product;
    }

    public static Product createDefault() {
        return Product.schedule(
            DEFAULT_ARTIST_ID, DEFAULT_ARTIST_NAME, DEFAULT_GROUP_NAME,
            DEFAULT_DESCRIPTION, DEFAULT_IMAGE_URL, DEFAULT_PRICE, DEFAULT_OPEN_DATE);
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

    public static Product createMock(Long artistId, LocalDateTime openDate) {
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

    public static Product createDefaultByArtistInfo(
        Long artistId, String artistName, String groupName, LocalDateTime openDate
    ) {
        return Product.schedule(
            artistId,
            artistName,
            groupName,
            DEFAULT_DESCRIPTION,
            DEFAULT_IMAGE_URL,
            DEFAULT_PRICE,
            openDate
        );
    }

    public static Product createActiveByArtistInfo(
        Long artistId, String artistName, String groupName, LocalDateTime openDate
    ) {
        Product product = Product.schedule(
            artistId,
            artistName,
            groupName,
            DEFAULT_DESCRIPTION,
            DEFAULT_IMAGE_URL,
            DEFAULT_PRICE,
            openDate
        );
        product.activate();
        return product;
    }

    public static Product createDeletedByArtistInfo(
        Long artistId, String artistName, String groupName, LocalDateTime openDate) {
        Product product = Product.schedule(
            artistId,
            artistName,
            groupName,
            DEFAULT_DESCRIPTION,
            DEFAULT_IMAGE_URL,
            DEFAULT_PRICE,
            openDate
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
