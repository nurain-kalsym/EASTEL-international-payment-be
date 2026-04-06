package com.kalsym.internationalPayment.services;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.utility.HttpResponse;
import com.kalsym.internationalPayment.utility.Logger;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;

@Service
public class FraudCheckClient {

    @Value("${fraudCheckUrl:http://localhost:2345}")
    private String fraudCheckUrl;

    @Value("${fraudToken:MSPteOMTlVWnJiZMZaDHLa1Eq-YSDOLSPvSnE1P8nxc}")
    private String fraudToken;

    @Value("${fraudConnectTimeout:10000}")
    private int fraudConnectTimeout;

    @Value("${fraudWaitTimeout:30000}")
    private int fraudWaitTimeout;

    public HttpResponse firstCheckFraud(HttpServletRequest request, String transactionId) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "[firstCheckFraud] Calling fraud API....";

        String url = fraudCheckUrl + "/first-check/process/" + transactionId;
        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Authorization", fraudToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "URL::::: " + url);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> newResponse = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        JsonObject getResponseBody = new Gson().fromJson(newResponse.getBody(), JsonObject.class);
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Response Object succes:: ",
                getResponseBody);

        Integer status = newResponse.getStatusCodeValue();
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "status: ", status);
        if (status.equals(200)) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "api response: ",
                    newResponse.toString());

            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode root = mapper.readTree(newResponse.getBody().toString());
                String res = root.get("res").asText();

                response.setStatus(HttpStatus.OK);
                response.setData(res);
                response.setMessage(res);

                return response;

            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpStatus.EXPECTATION_FAILED);
                response.setError("Error extracting data from fraud API response");
                response.setMessage("Error extracting data from fraud API response");

                return response;
            }
        } else {
            response.setStatus(HttpStatus.EXPECTATION_FAILED);
            response.setError("Error getting response from fraud API");
            response.setMessage("Error getting response from fraud API");

            return response;
        }
    }

    public HttpResponse secondCheckFraud(HttpServletRequest request, String transactionId) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "[secondCheckFraud] Calling fraud API....";

        String url = fraudCheckUrl + "/second-check/process/" + transactionId;
        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Authorization", fraudToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "URL::::: " + url);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> newResponse = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        JsonObject getResponseBody = new Gson().fromJson(newResponse.getBody(), JsonObject.class);
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Response Object succes:: ",
                getResponseBody);

        Integer status = newResponse.getStatusCodeValue();
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "status: ", status);
        if (status.equals(200)) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "api response: ",
                    newResponse.toString());

            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode root = mapper.readTree(newResponse.getBody().toString());
                String res = root.get("res").asText();

                response.setStatus(HttpStatus.OK);
                response.setData(res);
                response.setMessage(res);

                return response;

            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpStatus.EXPECTATION_FAILED);
                response.setError("Error extracting data from fraud API response");
                response.setMessage("Error extracting data from fraud API response");

                return response;
            }
        } else {
            response.setStatus(HttpStatus.EXPECTATION_FAILED);
            response.setError("Error getting response from fraud API");
            response.setMessage("Error getting response from fraud API");

            return response;
        }
    }
}
