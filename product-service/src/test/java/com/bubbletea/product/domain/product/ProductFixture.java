package com.bubbletea.product.domain.product;

import org.springframework.test.util.ReflectionTestUtils;

public class ProductFixture {

    private static final String DEFAULT_ID = "test-id";
    private static final String DEFAULT_ARTIST_NAME = "아티스트";
    private static final String DEFAULT_GROUP_NAME = "아이돌그룹";
    private static final ProductStatus DEFAULT_STATUS = ProductStatus.ACTIVE;
    private static final boolean DEFAULT_DELETED = false;

    public static Product createDefault() {
        return create(DEFAULT_ID, DEFAULT_ARTIST_NAME, DEFAULT_GROUP_NAME);
    }

    public static Product create(String id, String artistName, String groupName) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "artistName", artistName);
        ReflectionTestUtils.setField(product, "groupName", groupName);
        ReflectionTestUtils.setField(product, "status", DEFAULT_STATUS);
        ReflectionTestUtils.setField(product, "deleted", DEFAULT_DELETED);
        return product;
    }
}
