package com.bubbletea.product.domain.product;


public record ProductListCondition(
    String groupNameFilter,
    String cursorArtistName,
    String cursorId,
    int size
) {

    public boolean hasCursor() {
        return cursorArtistName != null && cursorId != null;
    }

    public boolean hasGroupNameFilter() {
        return groupNameFilter != null && !groupNameFilter.isBlank();
    }

}
