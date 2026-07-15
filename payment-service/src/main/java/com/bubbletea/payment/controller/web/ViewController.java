package com.bubbletea.payment.controller.web;

import com.bubbletea.payment.service.BrandpayService;
import com.bubbletea.payment.service.dto.BrandpayAuthDetailResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ViewController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${toss.payments.client-key}")
    private String CLIENT_KEY;
    @Value("${toss.payments.widget-secret-key}")
    private String WIDGET_SECRET_KEY;
    @Value("${toss.payments.api-secret-key}")
    private String API_SECRET_KEY;
    @Value("${toss.payments.redirect-url}")
    private String REDIRECT_URL;

    private final Map<String, String> billingKeyMap = new HashMap<>();

    private final BrandpayService brandPayService;

    @GetMapping(path = {"/brandpay/checkout", "/brandpay/checkout.html"})
    public String brandPayPage(
            @RequestParam Long userId,
            @RequestParam Long orderId,
            Model model) {
        BrandpayAuthDetailResponseDto info = brandPayService.getCustomerKey(userId);

//        model.addAttribute("action", action);

        model.addAttribute("redirectUrl", REDIRECT_URL);
        model.addAttribute("clientKey", CLIENT_KEY);
        model.addAttribute("userId", info.userId());
        model.addAttribute("orderId", orderId);
        model.addAttribute("cards", info.cards());
        model.addAttribute("customerKey", info.customerKey());
//        model.addAttribute("orderId", orderId);
//        model.addAttribute("amount", 50000);
//        model.addAttribute("orderName", "토스 티셔츠 외 2건");
//        model.addAttribute("customerName", "김토스");
//        model.addAttribute("customerEmail", "customer123@gmail.com");

        return "brandpay/checkout";
    }

    @GetMapping(path = {"/", "/widget/index", "/widget/index.html"})
    public String indexPage(
            @RequestParam Long userId,
            @RequestParam Long orderId,
            Model model) {
        BrandpayAuthDetailResponseDto info = brandPayService.getCustomerKey(userId);

//        model.addAttribute("action", action);

        model.addAttribute("userId", info.userId());
        model.addAttribute("orderId", orderId);
        model.addAttribute("customerKey", info.customerKey());
//        model.addAttribute("orderId", orderId);
//        model.addAttribute("amount", 50000);
//        model.addAttribute("orderName", "토스 티셔츠 외 2건");
//        model.addAttribute("customerName", "김토스");
//        model.addAttribute("customerEmail", "customer123@gmail.com");

        return "widget/index";
    }

    @PostMapping(value = {"/confirm/widget", "/confirm/payment"})
    public ResponseEntity<JSONObject> confirmPayment(HttpServletRequest request, @RequestBody String jsonBody) throws Exception {
        String secretKey = request.getRequestURI().contains("/confirm/payment") ? API_SECRET_KEY : WIDGET_SECRET_KEY;
        JSONObject response = sendRequest(parseRequestData(jsonBody), secretKey, "https://api.tosspayments.com/v1/payments/confirm");
        int statusCode = response.containsKey("error") ? 400 : 200;
        return ResponseEntity.status(statusCode).body(response);
    }

//    @RequestMapping(value = "/confirm/brandpay", method = RequestMethod.POST, consumes = "application/json")
//    public ResponseEntity<JSONObject> confirmBrandpay(@RequestBody String jsonBody) throws Exception {
//        JSONObject requestData = parseRequestData(jsonBody);
//        String url = "https://api.tosspayments.com/v1/brandpay/payments/confirm";
//        JSONObject response = sendRequest(requestData, API_SECRET_KEY, url);
//        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
//    }
//
//    @RequestMapping(value = "/confirm-billing")
//    public ResponseEntity<JSONObject> confirmBilling(@RequestBody String jsonBody) throws Exception {
//        JSONObject requestData = parseRequestData(jsonBody);
//        String billingKey = billingKeyMap.get(requestData.get("customerKey"));
//        JSONObject response = sendRequest(requestData, API_SECRET_KEY, "https://api.tosspayments.com/v1/billing/" + billingKey);
//        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
//    }
//
//    @RequestMapping(value = "/issue-billing-key")
//    public ResponseEntity<JSONObject> issueBillingKey(@RequestBody String jsonBody) throws Exception {
//        JSONObject requestData = parseRequestData(jsonBody);
//        JSONObject response = sendRequest(requestData, API_SECRET_KEY, "https://api.tosspayments.com/v1/billing/authorizations/issue");
//
//        if (!response.containsKey("error")) {
//            billingKeyMap.put((String) requestData.get("customerKey"), (String) response.get("billingKey"));
//        }
//
//        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);

//    }

//    @RequestMapping(value = "/confirm/brandpay", method = RequestMethod.POST, consumes = "application/json")
//    public ResponseEntity<JSONObject> confirmBrandpay(@RequestBody String jsonBody) throws Exception {
//        JSONObject requestData = parseRequestData(jsonBody);
//        String url = "https://api.tosspayments.com/v1/brandpay/payments/confirm";
//        JSONObject response = sendRequest(requestData, API_SECRET_KEY, url);
//        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
//    }

    @RequestMapping(value = "/fail", method = RequestMethod.GET)
    public String failPayment(HttpServletRequest request, Model model) {
        model.addAttribute("code", request.getParameter("code"));
        model.addAttribute("message", request.getParameter("message"));
        return "/fail";
    }

    private JSONObject parseRequestData(String jsonBody) {
        try {
            return (JSONObject) new JSONParser().parse(jsonBody);
        } catch (ParseException e) {
            logger.error("JSON Parsing Error", e);
            return new JSONObject();
        }
    }

    private JSONObject sendRequest(JSONObject requestData, String secretKey, String urlString) throws IOException {
        HttpURLConnection connection = createConnection(secretKey, urlString);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestData.toString().getBytes(StandardCharsets.UTF_8));
        }

        try (InputStream responseStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream();
             Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8)) {
            return (JSONObject) new JSONParser().parse(reader);
        } catch (Exception e) {
            logger.error("Error reading response", e);
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", "Error reading response");
            return errorResponse;
        }
    }



    private HttpURLConnection createConnection(String secretKey, String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8)));
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        return connection;
    }

}
