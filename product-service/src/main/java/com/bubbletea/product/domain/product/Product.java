package com.bubbletea.product.domain.product;


import com.bubbletea.product.domain.common.BaseTimeEntity;
import com.bubbletea.product.domain.product.policy.ProductOpenSchedulePolicy;
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
@Document(collection = "products")
@CompoundIndexes({
    @CompoundIndex(name = "idx_status_openDate", def = "{'status': 1, 'openDate': 1}")
})
public class Product extends BaseTimeEntity {

    @Id
    private String id;

    @Indexed(unique = true, partialFilter = "{ 'deleted': false }")
    private String artistId;

    private String artistName;

    private String groupName;

    private String name;

    private String description;

    private String imageUrl;

    private ProductStatus status;

    private long price;

    private LocalDateTime openDate;

    private LocalDateTime scheduledDeletionDate;

    private LocalDateTime statusChangedAt;

    private boolean deleted;

    private LocalDateTime deletedAt;

    private Product(
        String artistId, String artistName, String groupName,
        String description, String imageUrl, long price, LocalDateTime openDate
    ) {
        this.artistId = artistId;
        this.artistName = artistName;
        this.groupName = groupName;
        this.name = ProductNameGenerator.generate(artistName, groupName);
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = ProductStatus.PENDING_OPEN;
        this.price = price;
        this.openDate = openDate;
        this.deleted = false;
        this.statusChangedAt = LocalDateTime.now();
    }

    public static Product schedule(
        String artistId, String artistName, String groupName,
        String description, String imageUrl, long price,
        LocalDateTime openDate
    ) {
        ProductOpenSchedulePolicy.validate(openDate, LocalDateTime.now());
        return new Product(
            artistId, artistName, groupName, description, imageUrl, price, openDate
        );
    }

}
