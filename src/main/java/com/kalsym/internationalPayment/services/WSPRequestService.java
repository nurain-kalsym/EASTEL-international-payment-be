package com.kalsym.internationalPayment.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.Transaction;
import com.kalsym.internationalPayment.model.dao.MtradePaymentRequest;
import com.kalsym.internationalPayment.model.dao.MtradePaymentResponse;
import com.kalsym.internationalPayment.model.dao.RetailRate;
import com.kalsym.internationalPayment.model.dao.ValidateBill;
import com.kalsym.internationalPayment.model.enums.VariantType;
import com.kalsym.internationalPayment.repositories.ProductRepository;
import com.kalsym.internationalPayment.repositories.ProductVariantRepository;
import com.kalsym.internationalPayment.utility.Logger;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.Objects;
import java.util.function.Consumer;

@Service
public class WSPRequestService {

    @Value("${wsp.username}")
    private String wspUsername;
    @Value("${wsp.password}")
    private String wspPassword;

    @Value("${wsp.billpaymenturl}")
    private String wspBillPaymentUrl;

    @Value("${wsp.topupurl}")
    private String wspTopUpUrl;

    @Value("${wsp.getvoucherurl}")
    private String wspGetVoucherUrl;

    @Value("${wsp.getretailrate}")
    private String wspGetRetailRateUrl;

    @Value("${wsp.getoperatorurl}")
    private String wspGetQueryOperatoryUrll;

    @Value("${wsp.getQueryTransactionUrl}")
    private String wspGetQueryTransactionUrl;

    @Value("${wsp.getValidateBillUrl}")
    private String wspGetValidateBillUrl;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductVariantRepository productVariantRepository;

    public MtradePaymentResponse requestPaymentType(MtradePaymentRequest mtradePaymentRequest) {
        MtradePaymentResponse mtradePaymentResponse;

        if (mtradePaymentRequest.getVariantType().equals(VariantType.BILLPAYMENT)) {
            mtradePaymentResponse = requestBillPayment(mtradePaymentRequest);
        } else if (mtradePaymentRequest.getVariantType().equals(VariantType.TOPUP) ||
                mtradePaymentRequest.getVariantType().equals(VariantType.BILLPREPAID) ||
                mtradePaymentRequest.getVariantType().equals(VariantType.BUNDLE)) {
            mtradePaymentResponse = requestTopUpPayment(mtradePaymentRequest);
        } else {
            mtradePaymentResponse = requestVoucherPayment(mtradePaymentRequest);
        }
        return mtradePaymentResponse;
    }

    public MtradePaymentResponse requestBillPayment(MtradePaymentRequest mtradePaymentRequest) {
        MtradePaymentResponse paymentResponse = new MtradePaymentResponse();
        String token = InternationalPaymentApplication.WSPTOKEN;

        JsonObject billPayment = new JsonObject();
        billPayment.addProperty("productCode", mtradePaymentRequest.getProductCode());
        billPayment.addProperty("senderMsisdn", mtradePaymentRequest.getSenderMsisdn());
        billPayment.addProperty("recipientMsisdn", mtradePaymentRequest.getRecipientMsisdn());
        billPayment.addProperty("customerTransactionId", mtradePaymentRequest.getCustomerTransactionId());
        billPayment.addProperty("payAmount", mtradePaymentRequest.getPayAmount());
        billPayment.addProperty("routingId", mtradePaymentRequest.getRoutingId());
        billPayment.addProperty("accountNo", mtradePaymentRequest.getAccountNo());
        if (mtradePaymentRequest.getBillPhoneNumber() != null && !mtradePaymentRequest.getBillPhoneNumber().isEmpty())
            billPayment.addProperty("billPhoneNumber", mtradePaymentRequest.getBillPhoneNumber());
        if (mtradePaymentRequest.getExtra1() != null && !mtradePaymentRequest.getExtra1().isEmpty())
            billPayment.addProperty("extra1", mtradePaymentRequest.getExtra1());
        if (mtradePaymentRequest.getExtra2() != null && !mtradePaymentRequest.getExtra2().isEmpty())
            billPayment.addProperty("extra2", mtradePaymentRequest.getExtra2());
        if (mtradePaymentRequest.getExtra3() != null && !mtradePaymentRequest.getExtra3().isEmpty())
            billPayment.addProperty("extra3", mtradePaymentRequest.getExtra3());
        if (mtradePaymentRequest.getExtra4() != null && !mtradePaymentRequest.getExtra4().isEmpty())
            billPayment.addProperty("extra4", mtradePaymentRequest.getExtra4());

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestBillPayment", "Response :: ",
                billPayment);

        RestTemplate restTemplate = new RestTemplate();

        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer Bearer" + token);

        // Create the request body
        String requestBody = billPayment.toString();
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // Send a POST request and retrieve the response
        String url = wspBillPaymentUrl;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "RequestBillPayment", "Response :: ",
                response.getBody());

        if (response.getStatusCodeValue() == 200) {
            JsonObject getResponseBody = new Gson().fromJson(response.getBody(), JsonObject.class);

            String responseCode = getResponseBody.get("responseCode").getAsString();

            if ("200".equals(responseCode)) {
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "RequestBillPayment",
                        "Object succes:: ",
                        getResponseBody);
                paymentResponse.setResponseCode(getResponseBody.get("responseCode").getAsString());
                paymentResponse.setResponseDescription(getResponseBody.get("responseDescription").getAsString());
                paymentResponse
                        .setCustomerTransactionId(getResponseBody.get("customerTransactionId").getAsString());
                paymentResponse.setSystemTransactionId(getResponseBody.get("sysTransactionId").getAsString());
            } else {
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestBillPayment",
                        "Object failed/pending:: ", getResponseBody);

                paymentResponse.setResponseCode(getResponseBody.get("responseCode").getAsString());
                paymentResponse.setResponseDescription(getResponseBody.get("responseDescription").getAsString());
                paymentResponse
                        .setCustomerTransactionId(getResponseBody.get("customerTransactionId").getAsString());
                paymentResponse.setSystemTransactionId(getResponseBody.get("sysTransactionId").getAsString());
            }
        } else {
            JsonObject getResponseBody = new Gson().fromJson(response.getBody(), JsonObject.class);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "RequestBillPayment", "Object failed:: ",
                    getResponseBody);

            paymentResponse.setResponseCode(getResponseBody.get("responseCode").getAsString());
            paymentResponse.setResponseDescription(getResponseBody.get("responseDescription").getAsString());
            paymentResponse.setCustomerTransactionId(getResponseBody.get("customerTransactionId").getAsString());
            paymentResponse.setSystemTransactionId(getResponseBody.get("sysTransactionId").getAsString());
        }
        return paymentResponse;

    }

    public MtradePaymentResponse requestTopUpPayment(MtradePaymentRequest mtradePaymentRequest) {
        MtradePaymentResponse paymentResponse = new MtradePaymentResponse();
        String token = InternationalPaymentApplication.WSPTOKEN;

        JsonObject billPayment = new JsonObject();
        billPayment.addProperty("productCode", mtradePaymentRequest.getProductCode());
        billPayment.addProperty("senderMsisdn", mtradePaymentRequest.getSenderMsisdn());
        billPayment.addProperty("recipientMsisdn", mtradePaymentRequest.getRecipientMsisdn());
        billPayment.addProperty("customerTransactionId", mtradePaymentRequest.getCustomerTransactionId());
        billPayment.addProperty("payAmount", mtradePaymentRequest.getPayAmount());
        billPayment.addProperty("routingId", mtradePaymentRequest.getRoutingId());
        billPayment.addProperty("accountNo", mtradePaymentRequest.getAccountNo());
        if (mtradePaymentRequest.getBillPhoneNumber() != null && !mtradePaymentRequest.getBillPhoneNumber().isEmpty())
            billPayment.addProperty("billPhoneNumber", mtradePaymentRequest.getBillPhoneNumber());
        if (mtradePaymentRequest.getExtra1() != null && !mtradePaymentRequest.getExtra1().isEmpty())
            billPayment.addProperty("extra1", mtradePaymentRequest.getExtra1());
        if (mtradePaymentRequest.getExtra2() != null && !mtradePaymentRequest.getExtra2().isEmpty())
            billPayment.addProperty("extra2", mtradePaymentRequest.getExtra2());
        if (mtradePaymentRequest.getExtra3() != null && !mtradePaymentRequest.getExtra3().isEmpty())
            billPayment.addProperty("extra3", mtradePaymentRequest.getExtra3());
        if (mtradePaymentRequest.getExtra4() != null && !mtradePaymentRequest.getExtra4().isEmpty())
            billPayment.addProperty("extra4", mtradePaymentRequest.getExtra4());
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestTopUpPayment", "Request:: ",
                billPayment);

        RestTemplate restTemplate = new RestTemplate();

        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer Bearer" + token);

        // Create the request body
        String requestBody = billPayment.toString();
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // Send a POST request and retrieve the response
        String url = wspTopUpUrl;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestTopUpPayment", "Response :: ",
                    response.getBody());

            if (response.getStatusCodeValue() == 200) {
                JsonObject getResponseBody = new Gson().fromJson(response.getBody(), JsonObject.class);

                String responseCode = getResponseBody.get("responseCode").getAsString();

                if ("200".equals(responseCode)) {
                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestTopUpPayment",
                            "Object succes:: ", getResponseBody);
                    paymentResponse.setResponseCode(getResponseBody.get("responseCode").getAsString());
                    paymentResponse.setResponseDescription(getResponseBody.get("responseDescription").getAsString());
                    paymentResponse
                            .setCustomerTransactionId(getResponseBody.get("customerTransactionId").getAsString());
                    paymentResponse.setSystemTransactionId(getResponseBody.get("sysTransactionId").getAsString());
                } else {
                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestTopUpPayment",
                            "Object failed/pending:: ", getResponseBody);

                    paymentResponse.setResponseCode(getResponseBody.get("responseCode").getAsString());
                    paymentResponse.setResponseDescription(getResponseBody.get("responseDescription").getAsString());
                    paymentResponse
                            .setCustomerTransactionId(getResponseBody.get("customerTransactionId").getAsString());
                    paymentResponse.setSystemTransactionId(getResponseBody.get("sysTransactionId").getAsString());
                }

            } else {
                JsonObject getResponseBody = new Gson().fromJson(response.getBody(), JsonObject.class);
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestTopUpPayment",
                        "Object failed:: ", getResponseBody);

                paymentResponse.setResponseCode(getResponseBody.get("responseCode").getAsString());
                paymentResponse.setResponseDescription(getResponseBody.get("responseDescription").getAsString());
                paymentResponse.setCustomerTransactionId(getResponseBody.get("customerTransactionId").getAsString());
                paymentResponse.setSystemTransactionId(getResponseBody.get("sysTransactionId").getAsString());
            }
        } catch (Exception exception) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestTopUpPayment", "Exception :: ",
                    exception.getMessage());
            return null;
        }
        return paymentResponse;

    }

    public MtradePaymentResponse requestVoucherPayment(MtradePaymentRequest mtradePaymentRequest) {
        MtradePaymentResponse paymentResponse = new MtradePaymentResponse();
        String token = InternationalPaymentApplication.WSPTOKEN;

        JsonObject billPayment = new JsonObject();
        billPayment.addProperty("productCode", mtradePaymentRequest.getProductCode());
        billPayment.addProperty("senderMsisdn", mtradePaymentRequest.getSenderMsisdn());
        billPayment.addProperty("recipientMsisdn", mtradePaymentRequest.getRecipientMsisdn());
        billPayment.addProperty("customerTransactionId", mtradePaymentRequest.getCustomerTransactionId());
        billPayment.addProperty("payAmount", mtradePaymentRequest.getPayAmount());
        billPayment.addProperty("routingId", mtradePaymentRequest.getRoutingId());
        billPayment.addProperty("accountNo", mtradePaymentRequest.getAccountNo());
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestVoucherPayment", "Request:: ",
                billPayment);

        RestTemplate restTemplate = new RestTemplate();

        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer Bearer" + token);

        // Create the request body
        String requestBody = billPayment.toString();
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // Send a POST request and retrieve the response
        String url = wspGetVoucherUrl;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestVoucherPayment", "Response:: ",
                response.getBody());

        if (response.getStatusCodeValue() == 200) {
            JsonObject getResponseBody = new Gson().fromJson(response.getBody(), JsonObject.class);

            String responseCode = getResponseBody.get("responseCode").getAsString();
            if ("200".equals(responseCode)) {
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestVoucherPayment",
                        "Object succes:: ", getResponseBody);
                paymentResponse.setResponseCode(responseCode);
                paymentResponse.setResponseDescription(getResponseBody.get("responseDescription").getAsString());
                paymentResponse.setCustomerTransactionId(getResponseBody.get("customerTransactionId").getAsString());
                paymentResponse.setSystemTransactionId(getResponseBody.get("sysTransactionId").getAsString());
                paymentResponse.setVoucherNo(getResponseBody.get("voucherNo").getAsString());
                paymentResponse.setVoucherSerial(getResponseBody.get("voucherSerial").getAsString());
            } else {
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestVouchePayment",
                        "Object failed/pending:: ", getResponseBody);

                paymentResponse.setResponseCode(getResponseBody.get("responseCode").getAsString());
                paymentResponse.setResponseDescription(getResponseBody.get("responseDescription").getAsString());
                paymentResponse.setCustomerTransactionId(getResponseBody.get("customerTransactionId").getAsString());
                paymentResponse.setSystemTransactionId(getResponseBody.get("sysTransactionId").getAsString());
            }
        } else {
            JsonObject getResponseBody = new Gson().fromJson(response.getBody(), JsonObject.class);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestVoucherPayment",
                    "Object failed:: ", getResponseBody);

            paymentResponse.setResponseCode(getResponseBody.get("responseCode").getAsString());
            paymentResponse.setResponseDescription(getResponseBody.get("responseDescription").getAsString());
            paymentResponse.setCustomerTransactionId(getResponseBody.get("customerTransactionId").getAsString());
            paymentResponse.setSystemTransactionId(getResponseBody.get("sysTransactionId").getAsString());
        }
        return paymentResponse;

    }

    public RetailRate requestRetailRate(String countryCode) {
        String logprefix = "RequetRetailRate";
        RetailRate rate = new RetailRate();
        String token = InternationalPaymentApplication.WSPTOKEN;

        RestTemplate restTemplate = new RestTemplate();

        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer Bearer" + token);

        // Create the request body
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

        // Send a POST request and retrieve the response
        String url = wspGetRetailRateUrl + countryCode;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Response ", response.getBody());

        if (response.getStatusCodeValue() == 200) {
            JsonObject getResponseBody = new Gson().fromJson(response.getBody(), JsonObject.class);
            if (Objects.equals(getResponseBody.get("responseCode").getAsString(), "000")) {
                JsonObject jsonObject = getResponseBody.get("attributes").getAsJsonObject().get("customerRetailPrice")
                        .getAsJsonObject();

                rate.setRate(jsonObject.get("rate").getAsDouble());
                rate.setId(jsonObject.get("id").getAsInt());
                rate.setStatus(jsonObject.get("status").getAsString());
                rate.setCurrencyCode(jsonObject.get("currencyCode").getAsString());
            } else {
                rate = null;
            }
        } else
            rate = null;
        return rate;

    }

    public ValidateBill requestValidateBill(String productCode, String accountNo, String method) {
        String logprefix = "RequetValidateBill";
        ValidateBill validateBill = new ValidateBill();
        String token = InternationalPaymentApplication.WSPTOKEN;

        RestTemplate restTemplate = new RestTemplate();

        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer Bearer" + token);

        // Create the request body
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

        // Send a POST request and retrieve the response
        String url = wspGetValidateBillUrl + "?productCode=" + productCode;
        if (accountNo != null)
            url += "&accountNo=" + accountNo;

        if (method != null)
            url += "&method=" + method;
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "url : ", url);

        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Response ",
                    response.getBody());
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "response.getStatusCodeValue() : ", response.getStatusCodeValue());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // Handle the 404 response as needed, e.g., set appropriate fields in
                // validateBill
                validateBill.setResponseDescription("Resource not found");
                return validateBill;
            } else {
                // Handle other exceptions as needed
                validateBill = null;
                return validateBill;
            }
        }

        if (response.getStatusCodeValue() == 200) {
            JsonObject getResponseBody = new Gson().fromJson(response.getBody(), JsonObject.class);
            JsonObject jsonData = getResponseBody.get("data").getAsJsonObject();

            if (jsonData != null && jsonData.has("resultCode") && !jsonData.get("resultCode").isJsonNull()) {
                String resultCode = jsonData.get("resultCode").getAsString();

                if (Objects.equals(resultCode, "000")) {
                    // Populate validateBill for resultCode 000
                    setFieldIfPresent(jsonData, "billStatus", validateBill::setBillStatus);
                    setFieldIfPresent(jsonData, "description", validateBill::setResponseDescription);

                    if (jsonData.has("showBill") && !jsonData.get("showBill").isJsonNull()) {
                        try {
                            String showBillString = jsonData.get("showBill").getAsString();
                            ObjectMapper mapper = new ObjectMapper();

                            // Convert the raw JSON string to a Jackson JsonNode
                            JsonNode showBillNode = null;
                            try {
                                showBillNode = mapper.readTree(showBillString);
                            } catch (JsonProcessingException e) {
                                // Log the error or handle it as needed
                                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                        "Error processing JSON: " + e.getMessage());
                            }

                            // Assuming ValidateBill can accept a JsonNode for showBilltest
                            if (showBillNode != null) {
                                validateBill.setShowBill(showBillNode);
                            }
                        } catch (Exception e) {
                            // This catches any other exceptions that might occur outside of JSON processing
                            // Log the error or handle it as needed
                            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                    "n error occurred: " + e.getMessage());
                        }
                    }

                } else if (Objects.equals(resultCode, "211") || Objects.equals(resultCode, "203")
                        || (Objects.equals(resultCode, "214"))) {
                    // Populate validateBill for resultCode 211 or 203
                    setFieldIfPresent(jsonData, "billStatus", validateBill::setBillStatus);
                    setFieldIfPresent(jsonData, "description", validateBill::setResponseDescription);
                } else if (Objects.equals(resultCode, "200")) {
                    // Populate validateBill for resultCode 214
                    setFieldIfPresent(jsonData, "billStatus", validateBill::setBillStatus);
                    setFieldIfPresent(jsonData, "description", validateBill::setResponseDescription);
                    if (jsonData.has("planList") && !jsonData.get("planList").isJsonNull()) {
                        try {
                            String planListString = jsonData.get("planList").getAsString();
                            ObjectMapper mapper = new ObjectMapper();

                            // Convert the raw JSON string to a Jackson JsonNode
                            JsonNode planListNode = null;
                            try {
                                planListNode = mapper.readTree(planListString);
                            } catch (JsonProcessingException e) {
                                // Log the error or handle it as needed
                                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                        "Error processing JSON: " + e.getMessage());
                            }

                            // Assuming ValidateBill can accept a JsonNode for planList
                            if (planListNode != null) {
                                validateBill.setPlanList(planListNode);
                            }
                        } catch (Exception e) {
                            // This catches any other exceptions that might occur outside of JSON processing
                            // Log the error or handle it as needed
                            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                    "n error occurred: " + e.getMessage());
                        }
                    }
                } else {
                    validateBill = null;
                }
            }
        } else {
            validateBill = null;
        }

        return validateBill;
    }

    private void setFieldIfPresent(JsonObject jsonData, String fieldKey, Consumer<String> setter) {
        if (jsonData.has(fieldKey) && !jsonData.get(fieldKey).isJsonNull()) {
            setter.accept(jsonData.get(fieldKey).getAsString());
        }
    }

    public MtradePaymentResponse requestGetOperator(String phoneNo, String countryCode) {
        MtradePaymentResponse queryOperator = new MtradePaymentResponse();
        String token = InternationalPaymentApplication.WSPTOKEN;

        RestTemplate restTemplate = new RestTemplate();
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestGetOperator()", " Token ", token);

        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer Bearer" + token);

        // Create the request body
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

        // Send a POST request and retrieve the response
        String url = wspGetQueryOperatoryUrll + "phoneNo=" + phoneNo + "&countryCode=" + countryCode;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "requestGetOperator()", " Response ",
                response.getBody());

        if (response.getStatusCodeValue() == 200) {
            JsonObject getResponseBody = new Gson().fromJson(response.getBody(), JsonObject.class);
            queryOperator.setResponseCode(getResponseBody.get("responseCode").getAsString());
            queryOperator.setResponseDescription(getResponseBody.get("responseDescription").getAsString());
            queryOperator.setProductOwner(getResponseBody.get("attributes").getAsJsonObject().get("result")
                    .getAsJsonObject().get("productOwner").getAsString());
            queryOperator.setProductOwnerName(getResponseBody.get("attributes").getAsJsonObject().get("result")
                    .getAsJsonObject().get("productOwnerName").getAsString());

        } else {
            JsonObject getResponseBody = new Gson().fromJson(response.getBody(), JsonObject.class);
            queryOperator.setResponseCode(getResponseBody.get("responseCode").getAsString());
            queryOperator.setResponseDescription(getResponseBody.get("responseDescription").getAsString());

        }
        return queryOperator;

    }

    public MtradePaymentResponse queryTransaction(Transaction transaction) {
        MtradePaymentResponse queryTransaction = new MtradePaymentResponse();

        RestTemplate restTemplate = new RestTemplate();
        String wspToken = InternationalPaymentApplication.WSPTOKEN;

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION,
                "RequestQueryTransaction() " + transaction.getTransactionId(), " Token ", wspToken);
        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer Bearer" + wspToken);

        // Create the request body
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

        // Send a POST request and retrieve the response
        String url = wspGetQueryTransactionUrl + "?sysTransactionId=" + transaction.getWspTransactionId();
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION,
                "RequestQueryTransaction() " + transaction.getTransactionId(), " FullUrl ", url);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION,
                "RequestQueryTransaction() " + transaction.getTransactionId(), " Response ", response);
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION,
                "RequestQueryTransaction() " + transaction.getTransactionId(), " Response Body", response.getBody());
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION,
                "RequestQueryTransaction() " + transaction.getTransactionId(), " Response Status Code",
                response.getStatusCodeValue());

        final JsonObject getResponseBody = new Gson().fromJson(response.getBody(), JsonObject.class);

        final String statusCode = getResponseBody.get("statusCode").getAsString();

        if (statusCode == null || statusCode.isEmpty()) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION,
                    "RequestQueryTransaction() Exception, Status Code [null or empty] :" + statusCode, " Response ",
                    response.getBody());

            return null;
        } else {
            if ("500".equals(statusCode)) {
                return null;
            } else {
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION,
                        "statusCode {}, responseDescription {} "
                                + statusCode + getResponseBody.get("status").getAsString());
                queryTransaction.setResponseCode(statusCode);
                queryTransaction.setResponseDescription(getResponseBody.get("status").getAsString());
                if ("000".equals(statusCode)) {
                    if (getResponseBody.has("voucherNo")) {
                        queryTransaction.setVoucherNo(getResponseBody.get("voucherNo").getAsString());
                        queryTransaction.setVoucherSerial(getResponseBody.get("voucherSerial").getAsString());
                        queryTransaction.setVoucherExpiryDate(getResponseBody.get("voucherExpiry").getAsString());
                        queryTransaction.setVoucherUrl(getResponseBody.get("voucherUrl").getAsString());
                    }
                }
            }
        }

        return queryTransaction;
    }

    @Getter
    @Setter
    public static class PaymentResponse {
        String customerTransactionId;
        String responseCode;
        String responseDescription;
        String systemTransactionId;
        String voucherNo;
        String voucherSerial;

    }

}
