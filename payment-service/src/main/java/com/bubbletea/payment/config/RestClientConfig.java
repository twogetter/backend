package com.bubbletea.payment.config;

import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Configuration
public class RestClientConfig {

    @Value("${toss.payments.api-secret-key}")
    private String apiSecretKey;

    @Bean
    public RestClient tossRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        String encodedSecret = Base64.getEncoder()
                .encodeToString((apiSecretKey + ":").getBytes(StandardCharsets.UTF_8));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("https://tosspayments.com") // 공통 Base URL 지정
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedSecret) // 자동 헤더 세팅
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // 자동 헤더 세팅
                .build();
    }
}