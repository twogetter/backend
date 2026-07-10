package com.bubbletea.product.domain.common;

import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;


@Getter
public abstract class BaseCreatedAtEntity {

    @CreatedDate
    private LocalDateTime createdAt;
}
