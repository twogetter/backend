package com.bubbletea.product.domain.history;

import com.bubbletea.product.domain.common.BaseCreatedAtEntity;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "product_price_change_histories")
@CompoundIndexes({
    @CompoundIndex(name = "idx_productId_appliedAt", def = "{'productId': 1, 'appliedAt': -1}")
})
public class ProductPriceChangeHistory extends BaseCreatedAtEntity {

    @Id
    private String id;

    private String productId;

    private long originalPrice;

    private long changedPrice;

    @Indexed
    private String batchId;

    private LocalDateTime appliedAt;

    private ProductPriceChangeHistory(
        String productId, long originalPrice, long changedPrice,
        String batchId, LocalDateTime appliedAt
    ) {
        this.productId = productId;
        this.originalPrice = originalPrice;
        this.changedPrice = changedPrice;
        this.batchId = batchId;
        this.appliedAt = appliedAt;
    }
}
