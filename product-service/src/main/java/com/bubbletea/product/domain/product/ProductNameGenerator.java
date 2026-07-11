package com.bubbletea.product.domain.product;


import lombok.experimental.UtilityClass;

@UtilityClass
public class ProductNameGenerator {

    private final String SUFFIX = " 구독권";

    public String generate(String artistName, String groupName) {
        String displayName = (groupName != null && !groupName.isBlank())
            ? groupName + " " + artistName
            : artistName;
        return "[" + displayName + "]" + SUFFIX;
    }
}
