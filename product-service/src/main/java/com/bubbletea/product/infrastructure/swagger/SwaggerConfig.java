package com.bubbletea.product.infrastructure.swagger;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI productServiceOpenAPI() {
        return new OpenAPI()
            .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
            .title("Product Service API")
            .description("상품 서비스 API 명세서")
            .version("v1");
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .displayName("외부 공개 API")
            .pathsToMatch("/api/products/**")
            .packagesToScan("com.bubbletea.product.presentation.controller")
            .build();
    }

    @Bean
    public GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
            .group("internal")
            .displayName("내부 통신 API")
            .pathsToMatch("/api/v1/**")
            .packagesToScan("com.bubbletea.product.presentation.internal")
            .build();
    }

}
