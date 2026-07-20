package com.bubbletea.commontest.container;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public interface PostgresTestContainer {

    PostgreSQLContainer<?> POSTGRES_CONTAINER = createAndStartContainer();

    private static PostgreSQLContainer<?> createAndStartContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("bubbletea_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
    }
}
