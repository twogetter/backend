package com.bubbletea.commontest.container;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    default void cleanUpPostgres() throws Exception {
        try (Connection conn = DriverManager.getConnection(
            POSTGRES_CONTAINER.getJdbcUrl(),
            POSTGRES_CONTAINER.getUsername(),
            POSTGRES_CONTAINER.getPassword());
            Statement stmt = conn.createStatement()) {

            StringBuilder tables = new StringBuilder();
            try (ResultSet rs = stmt.executeQuery(
                "SELECT format('%I.%I', schemaname, tablename) " +
                    "FROM pg_tables " +
                    "WHERE schemaname = 'public' " +
                    "AND tablename NOT IN ('flyway_schema_history')")) {

                while (rs.next()) {
                    if (!tables.isEmpty()) {
                        tables.append(", ");
                    }
                    tables.append(rs.getString(1)); // 이미 quoting된 식별자 그대로 사용
                }
            }

            if (!tables.isEmpty()) {
                stmt.execute("TRUNCATE TABLE " + tables + " RESTART IDENTITY CASCADE");
            }
        }
    }
}
