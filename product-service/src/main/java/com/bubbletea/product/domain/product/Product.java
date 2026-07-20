package com.bubbletea.product.domain.product;


import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.domain.common.BaseTimeEntity;
import com.bubbletea.product.domain.exception.ProductErrorCode;
import com.bubbletea.product.domain.product.policy.ProductSchedulePolicy;
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
    @CompoundIndex(
        name = "idx_status_openDate",
        def = "{'status': 1, 'openDate': 1}"
    ),
    @CompoundIndex(
        name = "idx_product_list",
        def = "{'status': 1, 'deleted': 1, 'artistName': 1, '_id': 1}"
    ),
    @CompoundIndex(
        name = "idx_product_list_by_group",
        def = "{'status': 1, 'deleted': 1, 'groupName': 1, 'artistName': 1, '_id': 1}"
    )
})
public class Product extends BaseTimeEntity {

    @Id
    private String id;

    @Indexed(unique = true, partialFilter = "{ 'deleted': false }")
    private Long artistId;

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
        Long artistId, String artistName, String groupName,
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
        Long artistId, String artistName, String groupName,
        String description, String imageUrl, long price, LocalDateTime openDate
    ) {
        ProductSchedulePolicy.validate(openDate, LocalDateTime.now());
        return new Product(
            artistId, artistName, groupName, description, imageUrl, price, openDate);
    }

    public void verifySchedulability(LocalDateTime runDate) {
        assertNotDeleted();
        ProductSchedulePolicy.validate(runDate, LocalDateTime.now());
    }

    public void activate() {
        assertNotDeleted();
        this.status = ProductStatus.ACTIVE;
        this.statusChangedAt = LocalDateTime.now();
    }

    public void deactivate() {
        assertNotDeleted();
        this.status = ProductStatus.INACTIVE;
        this.statusChangedAt = LocalDateTime.now();
    }

    public void changePrice(long newPrice) {
        assertNotDeleted();
        this.price = newPrice;
    }

    public void markDeleted() {
        assertNotDeleted();
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.scheduledDeletionDate = null;
    }

    private void assertNotDeleted() {
        if (this.deleted) {
            throw new AppException(ProductErrorCode.ALREADY_DELETED);
        }
    }

}
