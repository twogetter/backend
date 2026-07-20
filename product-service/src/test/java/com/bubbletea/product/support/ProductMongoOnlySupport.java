package com.bubbletea.product.support;

import com.bubbletea.commontest.container.MongoTestContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public abstract class ProductMongoOnlySupport implements MongoTestContainer {

}
