package com.bubbletea.product.domain.product;

public record ProductSearchCondition(
    String keyword,
    String cursorArtistName,
    String cursorId,
    int size
) {

    public boolean hasCursor() {
        return cursorArtistName != null && cursorId != null;
    }
}
