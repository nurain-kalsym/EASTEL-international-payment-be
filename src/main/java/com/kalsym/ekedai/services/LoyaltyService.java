package com.kalsym.ekedai.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.model.Product;
import com.kalsym.ekedai.model.ProductVariant;
import com.kalsym.ekedai.model.ServiceId;
import com.kalsym.ekedai.model.Transaction;
import com.kalsym.ekedai.model.dao.loyalty.OverallCoinsData;
import com.kalsym.ekedai.repositories.ProductRepository;
import com.kalsym.ekedai.utility.HttpResponse;
import com.kalsym.ekedai.utility.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Optional;

@Service
public class LoyaltyService {

    @Value("${loyalty.service.url:https://uatloyalty.e-kedai.my}")
    String loyaltyServiceUrl;

    @Value("${loyalty.service.token:eyJhbGciOiJIUzI1NiJ9.e30.SWH-3nREFY6jPlzU8Pc0ClTVtPWKhorbaTDhVHx_j7M}")
    String loyaltyServiceToken;

    @Value("${channel.name}")
    String channelName;

    @Autowired
    ProductRepository productRepository;

    private HttpResponse earnReferralCoins(String phone, Double amount, String serviceId) {

        JsonObject jsonBuilder = new JsonObject();
        jsonBuilder.addProperty("amount", amount);
        jsonBuilder.addProperty("channel", channelName);
        jsonBuilder.addProperty("phone", phone);
        jsonBuilder.addProperty("serviceId", serviceId);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + loyaltyServiceToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBuilder.toString(), headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> responseEntity = restTemplate.exchange(
                loyaltyServiceUrl + "/referral/earn-referral-coins", HttpMethod.POST, requestEntity,
                String.class);

        Gson gson = new Gson();
        return gson.fromJson(responseEntity.getBody(), HttpResponse.class);
    }

    public HttpResponse earnLoyaltyCoins(String phone, Double amount, String serviceId, String productName, String productType) {

        JsonObject jsonBuilder = new JsonObject();
        jsonBuilder.addProperty("amount", amount);
        jsonBuilder.addProperty("channel", channelName);
        jsonBuilder.addProperty("phone", phone);
        jsonBuilder.addProperty("serviceId", serviceId);
        jsonBuilder.addProperty("productName", productName);
        jsonBuilder.addProperty("productType", productType);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + loyaltyServiceToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBuilder.toString(), headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> responseEntity = restTemplate.exchange(
                loyaltyServiceUrl + "/loyalty/earn-loyalty-coins", HttpMethod.POST, requestEntity,
                String.class);

        Gson gson = new Gson();
        return gson.fromJson(responseEntity.getBody(), HttpResponse.class);
    }

    private HttpResponse useOverallCoins(String phone, Integer amountToUse, String product, String productImg, String transactionId) {

        JsonObject jsonBuilder = new JsonObject();
        jsonBuilder.addProperty("amountToUse", amountToUse);
        jsonBuilder.addProperty("channel", channelName);
        jsonBuilder.addProperty("phoneNumber", phone);
        jsonBuilder.addProperty("product", product);
        jsonBuilder.addProperty("productImg", productImg);
        jsonBuilder.addProperty("transactionId", transactionId);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + loyaltyServiceToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBuilder.toString(), headers);
        RestTemplate restTemplate = new RestTemplate();

        // Make the GET request and return the response body as a String
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                loyaltyServiceUrl + "/overall-coins/use-coins", HttpMethod.POST, requestEntity,
                String.class
        );

        Gson gson = new Gson();
        return gson.fromJson(responseEntity.getBody(), HttpResponse.class);
    }

    public HttpResponse getAvailableCoins(String phone) {

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + loyaltyServiceToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        // Make the GET request and return the response body as a String
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                loyaltyServiceUrl + "/overall-coins/get-available-coins/" + phone,
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        Gson gson = new Gson();
        return gson.fromJson(responseEntity.getBody(), HttpResponse.class);
    }


    private HttpResponse postTransaction(String phone, Double amount, String productName, String productImg, String transactionId, String transactionType) {

        JsonObject jsonBuilder = new JsonObject();
        jsonBuilder.addProperty("amount", amount);
        jsonBuilder.addProperty("channel", channelName);
        jsonBuilder.addProperty("phoneNumber", phone);
        jsonBuilder.addProperty("productName", productName);
        jsonBuilder.addProperty("productImg", productImg);
        jsonBuilder.addProperty("transactionId", transactionId);
        jsonBuilder.addProperty("transactionType", transactionType);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + loyaltyServiceToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBuilder.toString(), headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> responseEntity = restTemplate.exchange(
                loyaltyServiceUrl + "/transaction/store", HttpMethod.POST, requestEntity,
                String.class);

        Gson gson = new Gson();
        return gson.fromJson(responseEntity.getBody(), HttpResponse.class);
    }

    public ServiceId getServiceIdValues(String serviceIdString) {

        if (serviceIdString == null) {
            return new ServiceId();
        }

        String ref = null;
        String loyalty = null;

        // Split by ';'
        String[] parts = serviceIdString.split(";");

        // Process each part
        for (String part : parts) {
            String[] keyValue = part.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                if ("ref".equals(key)) {
                    ref = value;
                } else if ("loy".equals(key)) {
                    loyalty = value;
                }
            }
        }

        return new ServiceId(ref, loyalty);
    }

    public OverallCoinsData getDataAsOverallCoinsData(Object data) throws IOException {
        if (data == null || data instanceof Integer) {
            return new OverallCoinsData();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(data, OverallCoinsData.class);
    }

    public void handleLoyaltyAndReferralCoins(Transaction transaction, String logPrefix, ProductVariant productVariant) {
        Optional<Product> optionalProduct = productRepository.findById(productVariant.getProductId());
        Gson gson = new Gson();

        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();

            // Earn referral coins for upstreams and earn loyalty coins
            if (productVariant.getServiceId() != null) {

                // Get values of serviceId
                ServiceId serviceId = getServiceIdValues(productVariant.getServiceId());

                // HTTP request to earn referral coins if not null
                if (serviceId.getReferral() != null) {
                    try {
                        HttpResponse httpResponse = earnReferralCoins(
                                transaction.getPhoneNo(),
                                transaction.getTransactionAmount(), serviceId.getReferral());
                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                "Loyalty service - earn referral coins response: " + httpResponse.getCode()
                                        + " - " + httpResponse.getMessage());
                    } catch (HttpStatusCodeException e) {
                        HttpResponse requestResponse = gson.fromJson(e.getResponseBodyAsString(), HttpResponse.class);

                        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                "Error in earning referral coins: " + requestResponse.getMessage(), e.getResponseBodyAsString());
                    } catch (Exception e) {
                        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                "Error in earning referral coins: " + e.getMessage(), e);
                    }
                }
                // HTTP request to earn loyalty coins if not null
                if (serviceId.getLoyalty() != null) {
                    try {
                        HttpResponse httpResponse = earnLoyaltyCoins(
                                transaction.getPhoneNo(),
                                transaction.getTransactionAmount(), serviceId.getLoyalty(), productVariant.getVariantName(),
                                productVariant.getVariantType().name());
                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                "Loyalty service - earn loyalty coins response: " + httpResponse.getCode()
                                        + " - " + httpResponse.getMessage());
                    } catch (HttpStatusCodeException e) {
                        HttpResponse requestResponse = gson.fromJson(e.getResponseBodyAsString(), HttpResponse.class);

                        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                "Error in earning loyalty coins: " + requestResponse.getMessage(), e.getResponseBodyAsString());
                    } catch (Exception e) {
                        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                "Error in earning loyalty coins: " + e.getMessage(), e);
                    }
                }
            }
            // Use coins
            if (transaction.getCoinsRedeemed() != null && transaction.getCoinsRedeemed() != 0) {
                String imageUrl = (product.getImageDetails() != null) ? product.getImageDetails().getImageUrl()
                        : null;

                try {
                    HttpResponse httpResponse = useOverallCoins(
                            transaction.getPhoneNo(),
                            (int) (transaction.getCoinsRedeemed() * 100), // Convert currency value to coins
                            product.getProductName(),
                            imageUrl,
                            transaction.getTransactionId());

                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "Loyalty service - use overall coins for: " + transaction.getPhoneNo());
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "Loyalty service - use overall coins response: " + httpResponse.getCode()
                                    + " - " + httpResponse.getMessage());
                } catch (HttpStatusCodeException e) {
                    HttpResponse requestResponse = gson.fromJson(e.getResponseBodyAsString(), HttpResponse.class);

                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "Error in using overall coins: " + requestResponse.getMessage(), e.getResponseBodyAsString());
                } catch (Exception e) {
                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "Error in using overall coins: " + e.getMessage(), e);
                }
            }
            // HTTP request to post transaction
            try {
                String imageUrl = (product.getImageDetails() != null) ? product.getImageDetails().getImageUrl()
                        : null;
                HttpResponse httpResponse = postTransaction(transaction.getPhoneNo(),
                        transaction.getTransactionAmount(), product.getProductName(), imageUrl, transaction.getId(),
                        String.valueOf(transaction.getTransactionType()));
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Loyalty service - post transaction response: " + httpResponse.getCode()
                                + " - " + httpResponse.getMessage());
            } catch (HttpStatusCodeException e) {
                HttpResponse requestResponse = gson.fromJson(e.getResponseBodyAsString(), HttpResponse.class);

                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Error in post transaction: " + requestResponse.getMessage(), e.getResponseBodyAsString());
            } catch (Exception e) {
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Error in post transaction: " + e.getMessage(), e);
            }
        }
    }

}
