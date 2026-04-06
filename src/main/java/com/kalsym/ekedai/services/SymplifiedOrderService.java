package com.kalsym.ekedai.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kalsym.ekedai.utility.CustomTimestampDeserializer;
import com.kalsym.ekedai.utility.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class SymplifiedOrderService {

        private final RestTemplate restTemplate;
        private final String orderServiceUrl;
        private final String bearerToken;

        @Autowired
        public SymplifiedOrderService(RestTemplateBuilder restTemplateBuilder,
                        @Value("${orderUrl}") String orderServiceUrl,
                        @Value("${order-service.token:accessToken}") String bearerToken) {
                this.restTemplate = restTemplateBuilder.build();
                this.orderServiceUrl = orderServiceUrl;
                this.bearerToken = bearerToken;
        }

        public ResponseEntity<String> getOrdersByGroupIds(List<String> orderGroupIds, boolean onlyVoucher, int page,
                        int pageSize, String search) {
                String url = UriComponentsBuilder.fromHttpUrl(orderServiceUrl + "/orders/batch")
                                .queryParam("onlyVoucher", onlyVoucher)
                                .queryParam("page", page)
                                .queryParam("pageSize", pageSize)
                                .queryParam("search", search)
                                .toUriString();

                // Set headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + bearerToken);

                // Create the request body as a raw array
                HttpEntity<List<String>> requestEntity = new HttpEntity<>(orderGroupIds, headers);

                return restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                requestEntity,
                                String.class);
        }

        public HttpResponse claimFreeCoupon(String phone, String voucherCode) {

                String url = UriComponentsBuilder.fromHttpUrl(orderServiceUrl + "/orders/placeFreeCouponGroupOrder")
                                .queryParam("voucherCode", voucherCode)
                                .queryParam("phoneNumber", phone)
                                .toUriString();

                // Set headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + bearerToken);

                HttpEntity<String> requestEntity = new HttpEntity<>(headers);
                RestTemplate restTemplate = new RestTemplate();

                // Make the GET request and return the response body as a String
                ResponseEntity<String> responseEntity = restTemplate.exchange(
                                url, HttpMethod.POST, requestEntity,
                                String.class);

                // Register the custom deserializer for Date
                Gson gson = new GsonBuilder()
                                .registerTypeAdapter(Date.class, new CustomTimestampDeserializer())
                                .create();
                return gson.fromJson(responseEntity.getBody(), HttpResponse.class);
        }
}
