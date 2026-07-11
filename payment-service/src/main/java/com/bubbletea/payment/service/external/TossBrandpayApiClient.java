package com.bubbletea.payment.service.external;

import com.bubbletea.payment.service.dto.TossAccessTokenResponseDto;
import com.bubbletea.payment.service.dto.TossRegisteredPaymentMethodsResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TossBrandpayApiClient {

    private static final Logger logger = LoggerFactory.getLogger(TossBrandpayApiClient.class);
    private static final String BRANDPAY_API_BASE_URL = "https://api.tosspayments.com/v1/brandpay";

    @Value("${toss.payments.api-secret-key}")
    private String apiSecretKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TossAccessTokenResponseDto getAccessToken(String customerKey, String code) throws Exception {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("grantType", "AuthorizationCode");
        requestData.put("customerKey", customerKey);
        requestData.put("code", code);

        String url = BRANDPAY_API_BASE_URL + "/authorizations/access-token";
        logger.info("Toss API Request: {} -> {}", requestData, url);
        try {
            return restClient.post()
                    .uri(url)
                    .body(requestData)
                    .retrieve()
                    .body(TossAccessTokenResponseDto.class);
        } catch (RestClientResponseException e) {
            try {
                return objectMapper.readValue(e.getResponseBodyAsString(), TossAccessTokenResponseDto.class);
            } catch (Exception ex) {
                return new TossAccessTokenResponseDto(null, null, "HTTP_ERROR", e.getMessage());
            }
        }
    }

    public TossRegisteredPaymentMethodsResponseDto getRegisteredPaymentMethods(String accessToken) throws Exception {
        String url = BRANDPAY_API_BASE_URL + "/payments/methods";
        logger.info("Toss API Request: GET {}", url);

        try {
            return restClient.get()
                    .uri(url)
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .retrieve()
                    .body(TossRegisteredPaymentMethodsResponseDto.class);
        } catch (RestClientResponseException e) {
            try {
                return objectMapper.readValue(e.getResponseBodyAsString(), TossRegisteredPaymentMethodsResponseDto.class);
            } catch (Exception ex) {
                return new TossRegisteredPaymentMethodsResponseDto(null, null, null, "HTTP_ERROR", e.getMessage());
            }
        }
    }



}
