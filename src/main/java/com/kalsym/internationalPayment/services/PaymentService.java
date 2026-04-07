package com.kalsym.internationalPayment.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.Transaction;
import com.kalsym.internationalPayment.model.dao.MMResponse;
import com.kalsym.internationalPayment.model.dao.Order.OrderConfirm;
import com.kalsym.internationalPayment.model.dao.Order.OrderConfirmData;
import com.kalsym.internationalPayment.model.dao.Order.OrderUpdate;
import com.kalsym.internationalPayment.repositories.TransactionRepository;
import com.kalsym.internationalPayment.utility.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Optional;

@Service
public class PaymentService {

    @Value("${orderUrl}")
    String orderUrl;

    @Value("${order-service.token:Bearer accessToken}")
    private String orderServiceToken;

    @Value("${BWALLET_PAYMENT_PUBLIC_KEY}")
    private String bwalletPubKey;

    @Value("${BWALLET_PAYMENT_PRIVATE_KEY}")
    private String bwalletPriKey;

    @Value("${BWALLET_PAYMENT_PUBLIC_KEY_DOMES}")
    private String bwalletPubKeyDomes;

    @Value("${BWALLET_PAYMENT_PRIVATE_KEY_DOMES}")
    private String bwalletPriKeyDomes;

    @Value("${BWALLET_PAYMENT_PUBLIC_KEY_UTILITY}")
    private String bwalletPubKeyUtility;

    @Value("${BWALLET_PAYMENT_PRIVATE_KEY_UTILITY}")
    private String bwalletPriKeyUtility;

    @Value("${BWALLET_PAYMENT_PUBLIC_KEY_VOUCHER}")
    private String bwalletPubKeyVoucher;

    @Value("${BWALLET_PAYMENT_PRIVATE_KEY_VOUCHER}")
    private String bwalletPriKeyVoucher;

    @Value("${BWALLET_PAYMENT_PUBLIC_KEY_ENTERTAINMENT}")
    private String bwalletPubKeyEntertainment;

    @Value("${BWALLET_PAYMENT_PRIVATE_KEY_ENTERTAINMENT}")
    private String bwalletPriKeyEntertainment;

    @Value ("${BWALLET_PAYMENT_URL}")
    private String bwalletBaseUrl;

    @Autowired
    TransactionRepository transactionRepository;

    @Value("${channel.name:e-kedai}")
    private String channelName;

    public String getToken(String transactionId) {

        // MMResponse mmResponse = new MMResponse();
        String logprefix = "getToken()" + transactionId;

        Optional<Transaction> transaction = transactionRepository.findByTransactionId(transactionId);

        if (!transaction.isPresent()) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Error", "Transaction not found");
            return "";
        }

        Transaction transactionProdType = transaction.get();

        String prodType = transactionProdType.getProduct().getProductType().toString();
        String countryCode = transactionProdType.getProduct().getCountryCode();

        String pubKeyWallet = bwalletPubKey;
        String privKeyWallet = bwalletPriKey;
        String url = InternationalPaymentApplication.MMGETTOKENURL;

        if("bizwallet".equalsIgnoreCase(channelName)) {
            if ("PREPAID".equalsIgnoreCase(prodType) && "MY".equalsIgnoreCase(countryCode)) {
                //domestic eload
                pubKeyWallet = bwalletPubKeyDomes;
                privKeyWallet = bwalletPriKeyDomes;
            } else if("PREPAID".equalsIgnoreCase(prodType)) {
                // international
                pubKeyWallet = bwalletPubKey;
                privKeyWallet = bwalletPriKey;
            } else if("BILLPAYMENT".equalsIgnoreCase(prodType)) {
                // utility int & domestic
                pubKeyWallet = bwalletPubKeyUtility;
                privKeyWallet = bwalletPriKeyUtility;
            } else if("ENTERTAINMENT".equalsIgnoreCase(prodType)) {
                // entertainment int & domestic
                pubKeyWallet = bwalletPubKeyEntertainment;
                privKeyWallet = bwalletPriKeyEntertainment;
            } else if("VOUCHER".equalsIgnoreCase(prodType)) {
                // voucher int & domestic
                pubKeyWallet = bwalletPubKeyVoucher;
                privKeyWallet = bwalletPriKeyVoucher;
            }
            url = bwalletBaseUrl + "/api/mmpay/token";
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set("key", InternationalPaymentApplication.KEY);
        headers.set("Host", InternationalPaymentApplication.HOST);
        headers.set("User-Agent", "PostmanRuntime/7.33.0");

        JsonObject request = new JsonObject();

        if("bizwallet".equalsIgnoreCase(channelName)) {
            headers.set("key", pubKeyWallet);
            request.addProperty("pass", privKeyWallet);
        } else {
             headers.set("key", InternationalPaymentApplication.KEY);
             request.addProperty("pass", InternationalPaymentApplication.PASS);
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Request Url: ", url);
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Request Body: ",
                request.toString());
        // Create an HttpEntity with the form data and headers
        HttpEntity<String> requestEntity = new HttpEntity<>(request.toString(), headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
                String.class);
        try {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Response: ",
                    responseEntity.getBody());

            if (responseEntity.getStatusCodeValue() == 200) {
                JsonObject jsonResp = new Gson().fromJson(responseEntity.getBody(), JsonObject.class);
                return jsonResp.get("Token").getAsString();
            } else {
                return "";
            }
        } catch (Exception ex) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Exception", ex.getMessage());
            return "";

        }
    }

    public MMResponse requestPayment(Transaction transaction) {

        MMResponse mmResponse = new MMResponse();
        String logprefix = "requestPayment() [" + transaction.getTransactionId() + "]";
        String countryCode = transaction.getProduct().getCountryCode();
        String pubKeyBwallet = null;

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set("key", InternationalPaymentApplication.KEY);

        if("bizwallet".equalsIgnoreCase(countryCode)) {
            headers.set("key", pubKeyBwallet);
        } else {
            headers.set("key", InternationalPaymentApplication.KEY);
        }

        headers.set("Host", InternationalPaymentApplication.HOST);
        headers.set("User-Agent", "PostmanRuntime/7.33.0");
        String token = getToken(transaction.getTransactionId());
        if (!token.isEmpty()) {
           
            String prodType = transaction.getProduct().getProductType().toString();
            String datetime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String uniqueId = Long.toHexString(System.nanoTime()).toUpperCase();
            String url = InternationalPaymentApplication.MMPAYMENTURL;

            if ("bizwallet".equalsIgnoreCase(channelName)) {
                if ("BILLPAYMENT".equalsIgnoreCase(prodType)) {
                    pubKeyBwallet = bwalletPubKeyUtility;
                } else if("ENTERTAINMENT".equalsIgnoreCase(prodType)) {
                    pubKeyBwallet = bwalletPubKeyEntertainment;
                } else if("VOUCHER".equalsIgnoreCase(prodType)) {
                    pubKeyBwallet = bwalletPubKeyVoucher;
                } else {
                    if ("MY".equalsIgnoreCase(countryCode)) {
                        pubKeyBwallet = bwalletPubKeyDomes;
                    } else {
                        pubKeyBwallet = bwalletPubKey;
                    }
                }
                url = bwalletBaseUrl + "/api/mmpay/pay";
            }

            String trxId = "BW_" + prodType + countryCode + "_" + uniqueId + "_" + datetime;

            String trxType;
            if ("PREPAID".equalsIgnoreCase(prodType)) {
                if ("MY".equalsIgnoreCase(countryCode)) {
                    trxType = "Y05"; // eload local
                } else {
                    trxType = "Y06"; // eload international
                }
            } else if ("BILLPAYMENT".equalsIgnoreCase(prodType)) {
                trxType = "Y13"; // utility int & local
            } else if ("VOUCHER".equalsIgnoreCase(prodType)) {
                trxType = "Y14"; // voucher int & local
            } else if ("ENTERTAINMENT".equalsIgnoreCase(prodType) || "GAMES".equalsIgnoreCase(prodType)) {
                trxType = "Y15"; // entertainment int & local
            } else {
                trxType = "Y05"; // default will follow eload local
            }

            JsonObject request = new JsonObject();
            request.addProperty("Token", token);
            request.addProperty("MobileNumber", transaction.getPhoneNo());
            request.addProperty("Amount", transaction.getTransactionAmount());
            request.addProperty("TransactionNumber", transaction.getTransactionId());
            
            if ("bizwallet".equalsIgnoreCase(channelName)) {
                request.addProperty("TransactionType", trxType);
            }

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Using pubKeywallet: " + pubKeyBwallet);

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "TransactionId [" + trxId + "] Request Body: ", request.toString());

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "TransactionId [" + transaction.getTransactionId() + "]Request Body: ", request.toString());

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Request Url: ", url);

            // Create an HttpEntity with the form data and headers
            HttpEntity<String> requestEntity = new HttpEntity<>(request.toString(), headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
                    String.class);
            JsonObject jsonResp = new Gson().fromJson(responseEntity.getBody(), JsonObject.class);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "TransactionId [" + transaction.getTransactionId() + "]Response", jsonResp.toString());

            String responseCode = jsonResp.get("Code").getAsString();
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "TransactionId [" + transaction.getTransactionId() + "]ResponseCode:" + responseCode);
            if (responseCode.equals("000")) {
                // OTPReferenceNumber
                // Response{"OTPReferenceNumber":"edc69373-37a1-498b-8ea7-7ce50fb4f43d","URN":"1011111250093727","Code":"000","Message":"Success"}

                // if ("bizwallet".equalsIgnoreCase(channelName)) {
                mmResponse.setIsOtpRequired(true);
                // } else {
                //     mmResponse.setIsOtpRequired(jsonResp.get("IsOtpRequired").getAsBoolean());
                // }
                mmResponse.setStatus(String.valueOf(responseEntity.getStatusCodeValue()));
                mmResponse.setMessage(jsonResp.get("Message").getAsString());
                mmResponse.setCode(jsonResp.get("Code").getAsString());
                mmResponse.setOtpReferenceNo(jsonResp.get("OTPReferenceNumber").getAsString());
            } else {
                mmResponse.setIsOtpRequired(true);
                mmResponse.setStatus(String.valueOf(responseEntity.getStatusCodeValue()));
                mmResponse.setCode(jsonResp.get("Code").getAsString());
                mmResponse.setMessage(jsonResp.get("Message").getAsString());
            }
        }
        return mmResponse;
    }

    public MMResponse validateOtp(Transaction transaction) {

        MMResponse mmResponse = new MMResponse();
        String logprefix = "validateOtp() [" + transaction.getTransactionId() + "]";
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Request Body: ", transaction);

        RestTemplate restTemplate = new RestTemplate();

        String prodType = transaction.getProduct().getProductType().toString();
        String countryCode = transaction.getProduct().getCountryCode(); 
        String pubKeyBwallet = null;
        String url = InternationalPaymentApplication.MMVALIDATEOTP;

        if("bizwallet".equalsIgnoreCase(channelName)) {
            if("BILLPAYMENT".equalsIgnoreCase(prodType)) {
                pubKeyBwallet = bwalletPubKeyUtility;
            } else if("ENTERTAINMENT".equalsIgnoreCase(prodType)) {
                pubKeyBwallet = bwalletPubKeyEntertainment;
            } else if("VOUCHER".equalsIgnoreCase(prodType)) {
                pubKeyBwallet = bwalletPubKeyVoucher;
            } else {
                pubKeyBwallet = bwalletPubKeyDomes;
                if ("MY".equalsIgnoreCase(countryCode)) {
                    pubKeyBwallet = bwalletPubKeyDomes;
                } else {
                    pubKeyBwallet = bwalletPubKey;
                }
            }
            url = bwalletBaseUrl + "/api/mmpay/otpresponse";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set("key", InternationalPaymentApplication.KEY);

        if("bizwallet".equalsIgnoreCase(channelName)) {
            headers.set("key", pubKeyBwallet);
        } else {
            headers.set("key", InternationalPaymentApplication.KEY);
        }

        headers.set("Host", InternationalPaymentApplication.HOST);
        headers.set("User-Agent", "PostmanRuntime/7.33.0");
        String token = getToken(transaction.getTransactionId());

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, 
            "Using key: " + ("bizwallet".equalsIgnoreCase(channelName) ? pubKeyBwallet : InternationalPaymentApplication.KEY));

        if (!token.isEmpty()) {

            JsonObject request = new JsonObject();
            request.addProperty("Token", token);
            request.addProperty("OTPReferenceNumber", transaction.getOtpReferenceNo());
            request.addProperty("OTP", transaction.getOtpNo());

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Request Url: ", url);

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Request Body: ",
                    request.toString());
            // Create an HttpEntity with the form data and headers
            HttpEntity<String> requestEntity = new HttpEntity<>(request.toString(), headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
                    String.class);
            JsonObject jsonResp = new Gson().fromJson(responseEntity.getBody(), JsonObject.class);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "TransactionId [" + transaction.getTransactionId() + "]Response", jsonResp.toString());

            String responseCode = jsonResp.get("Code").getAsString();

            if (responseCode.equals("000")) {
                mmResponse.setStatus(String.valueOf(responseEntity.getStatusCodeValue()));
                mmResponse.setMessage(jsonResp.get("Message").getAsString());
                mmResponse.setCode(jsonResp.get("Code").getAsString());
                mmResponse.setSpTransactionId(jsonResp.get("TransactionId").getAsString());

            } else {
                mmResponse.setStatus(String.valueOf(responseEntity.getStatusCodeValue()));
                mmResponse.setCode(jsonResp.get("Code").getAsString());
                mmResponse.setMessage(jsonResp.get("Message").getAsString());
            }
        }
        return mmResponse;
    }

    public MMResponse resendOtp(Transaction transaction) {

        MMResponse mmResponse = new MMResponse();
        String logprefix = "resendOtp() [" + transaction.getTransactionId() + "]";

        RestTemplate restTemplate = new RestTemplate();

        String countryCode = transaction.getProduct().getCountryCode();
        String prodType = transaction.getProduct().getProductType().toString();
        String pubKeyBwallet = null;
        String url = InternationalPaymentApplication.MMRESENDOTPURL;

        if("bizwallet".equalsIgnoreCase(channelName)) {
            if("PREPAID".equalsIgnoreCase(prodType) && "MY".equalsIgnoreCase(countryCode)) {
                // domestic eload
                pubKeyBwallet = bwalletPubKeyDomes;
            } else if("PREPAID".equalsIgnoreCase(prodType)) {
                // international
                pubKeyBwallet = bwalletPubKey;
            } else if("BILLPAYMENT".equalsIgnoreCase(prodType)) {
                // utility int & domestic
                pubKeyBwallet = bwalletPubKeyUtility;
            } else if("ENTERTAINMENT".equalsIgnoreCase(prodType)) {
                // entertainment int & domestic
                pubKeyBwallet = bwalletPubKeyEntertainment;
            } else if("VOUCHER".equalsIgnoreCase(prodType)) {
                // voucher int & domestic
                pubKeyBwallet = bwalletPubKeyVoucher;
            }
            url = bwalletBaseUrl + "/api/mmpay/otpresend";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set("key", InternationalPaymentApplication.KEY);

        if("bizwallet".equalsIgnoreCase(channelName)) {
            headers.set("key", pubKeyBwallet);
        } else {
            headers.set("key", InternationalPaymentApplication.KEY);
        }

        headers.set("Host", InternationalPaymentApplication.HOST);
        headers.set("User-Agent", "PostmanRuntime/7.33.0");
        String token = getToken(transaction.getTransactionId());
        if (!token.isEmpty()) {

            JsonObject request = new JsonObject();
            request.addProperty("Token", token);
            request.addProperty("OTPReferenceNumber", transaction.getOtpReferenceNo());

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Request Body: ",
                    request.toString());

            // Create an HttpEntity with the form data and headers
            HttpEntity<String> requestEntity = new HttpEntity<>(request.toString(), headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
                    String.class);
            JsonObject jsonResp = new Gson().fromJson(responseEntity.getBody(), JsonObject.class);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "RequestUrl: ",
                    url.toString());
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "TransactionId [" + transaction.getTransactionId() + "] Response: ", jsonResp.toString());

            String responseCode = jsonResp.get("Code").getAsString();
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "TransactionId [" + transaction.getTransactionId() + "] ResponseCode: " + responseCode);

            if (responseCode.equals("000")) {
                mmResponse.setStatus(String.valueOf(responseEntity.getStatusCodeValue()));
                mmResponse.setMessage(jsonResp.get("Message").getAsString());
                mmResponse.setCode(jsonResp.get("Code").getAsString());
            } else {
                mmResponse.setStatus(String.valueOf(responseEntity.getStatusCodeValue()));
                mmResponse.setCode(jsonResp.get("Code").getAsString());
                mmResponse.setMessage(jsonResp.get("Message").getAsString());

            }
        }
        return mmResponse;
    }

    public MMResponse getPaymentStatus(Transaction transaction) {

        MMResponse mmResponse = new MMResponse();
        String logprefix = "getPaymentStatus()  [" + transaction.getTransactionId() + "]";

        RestTemplate restTemplate = new RestTemplate();

        String countryCode = transaction.getProduct().getCountryCode();
        String prodType = transaction.getProduct().getProductType().toString();
        String pubKeyBwallet = null;
        String url = InternationalPaymentApplication.MMPAYMENTSTATUSURL;

        if("bizwallet".equalsIgnoreCase(channelName)) {
            if("PREPAID".equalsIgnoreCase(prodType) && "MY".equalsIgnoreCase(countryCode)) {
                // domistic eload
                pubKeyBwallet = bwalletPubKeyDomes;
            } else if("PREPAID".equalsIgnoreCase(prodType)) {
                // international
                pubKeyBwallet = bwalletPubKey;
            } else if("BILLPAYMENT".equalsIgnoreCase(prodType)) {
                // utility int & domestic
                pubKeyBwallet = bwalletPubKeyUtility;
            } else if("ENTERTAINMENT".equalsIgnoreCase(prodType)) {
                // entertainment int & domestic
                pubKeyBwallet = bwalletPubKeyEntertainment;
            } else if("VOUCHER".equalsIgnoreCase(prodType)) {
                // voucher int & domestic
                pubKeyBwallet = bwalletPubKeyVoucher;
            }
            url = bwalletBaseUrl + "/api/mmpay/paystatus";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set("key", InternationalPaymentApplication.KEY);

        if ("bizwallet".equalsIgnoreCase(channelName)) {
            headers.set("key", pubKeyBwallet);
        } else {
            headers.set("key", InternationalPaymentApplication.KEY);
        }

        headers.set("Host", InternationalPaymentApplication.HOST);
        headers.set("User-Agent", "PostmanRuntime/7.33.0");

        String token = getToken(transaction.getTransactionId());
        if (!token.isEmpty()) {

            JsonObject request = new JsonObject();
            request.addProperty("Token", token);
            request.addProperty("TransactionNumber", transaction.getTransactionId());

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Request Body: ",
                    request.toString());

            // Create an HttpEntity with the form data and headers
            HttpEntity<String> requestEntity = new HttpEntity<>(request.toString(), headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
                    String.class);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Response: ",
                    mmResponse.toString());
            JsonObject jsonResp = new Gson().fromJson(responseEntity.getBody(), JsonObject.class);

            if (responseEntity.getStatusCodeValue() == 200) {
                mmResponse.setStatus(String.valueOf(responseEntity.getStatusCodeValue()));
                mmResponse.setMessage(jsonResp.get("Message").getAsString());
                mmResponse.setCode(jsonResp.get("Code").getAsString());
            } else {
                mmResponse.setStatus(String.valueOf(responseEntity.getStatusCodeValue()));
                mmResponse.setMessage(jsonResp.get("Message").getAsString());
            }
        }
        return mmResponse;
    }

    public MMResponse refundTransaction(Transaction transaction) {

        MMResponse mmResponse = new MMResponse();
        String logprefix = "refundTransaction() [" + transaction.getTransactionId() + "]";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("key", InternationalPaymentApplication.KEY);
        headers.set("Host", InternationalPaymentApplication.HOST);
        headers.set("User-Agent", "PostmanRuntime/7.33.0");
        String url = InternationalPaymentApplication.MMREFUNDURL;
        String token = getToken(transaction.getTransactionId());
        if (!token.isEmpty()) {
            SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyyMMdd");

            String resultDateStr = sourceFormat.format(transaction.getCreatedDate());

            JsonObject request = new JsonObject();
            request.addProperty("Token", token);
            request.addProperty("OriginalTransactionNumber", transaction.getTransactionId());
            request.addProperty("OriginalTransactionDate", resultDateStr);
            request.addProperty("OriginalAmount", transaction.getTransactionAmount());
            request.addProperty("RefundAmount", transaction.getTransactionAmount());

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Request Body: ",
                    request.toString());

            if("bizwallet".equalsIgnoreCase(channelName)) {
                url = bwalletBaseUrl + "/api/mmpay/refund";
            }

            // Create an HttpEntity with the form data and headers
            HttpEntity<String> requestEntity = new HttpEntity<>(request.toString(), headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
                    String.class);
            JsonObject jsonResp = new Gson().fromJson(responseEntity.getBody(), JsonObject.class);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Request Url :", url);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Response: ",
                    jsonResp.toString());

            if (responseEntity.getStatusCodeValue() == 200) {
                mmResponse.setStatus(String.valueOf(responseEntity.getStatusCodeValue()));
                mmResponse.setMessage(jsonResp.get("Message").getAsString());
                mmResponse.setCode(jsonResp.get("Code").getAsString());
            } else {
                mmResponse.setStatus(String.valueOf(responseEntity.getStatusCodeValue()));
                mmResponse.setMessage(jsonResp.get("Message").getAsString());
            }
        }
        return mmResponse;
    }

    public OrderConfirm groupOrderUpdateStatus(String transactionId, String orderId, String paymentStatus,
            String modifyBy, String message, String paymentChannel) {
        String logprefix = "groupOrderUpdateStatus() [" + transactionId + "]";

        String url = orderUrl + "ordergroup/" + orderId + "/completion-status-updates";
        try {
            RestTemplate restTemplate = new RestTemplate();
            Instant instant = Instant.now();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", orderServiceToken);
            OrderUpdate orders = new OrderUpdate(message, instant.toString(), modifyBy, orderId, paymentStatus,
                    paymentChannel);
            // orders.setComments(message);
            // orders.setCreated(instant.toString());
            // orders.setModifiedBy(modifyBy);
            // orders.setOrderId(orderId);
            // orders.setStatus(paymentStatus);
            // orders.setPaymentChannel(paymentChannel);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "payment : " + orders);

            HttpEntity<OrderUpdate> httpEntity = new HttpEntity<>(orders, headers);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "orderDeliveryConfirmationURL : " + url);
            ResponseEntity<OrderConfirmData> res = restTemplate.exchange(url, HttpMethod.PUT, httpEntity,
                    OrderConfirmData.class);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Response : " + res);

            Logger.application.debug(
                    "Sending request to order-service: {} to update order group status (liveChatCsrGroupName) against orderId: {} , httpEntity: {}",
                    url, orderId, httpEntity);
            if (res != null) {
                OrderConfirmData orderConfirm = (OrderConfirmData) res.getBody();
                Logger.application.debug("Request sent to live service, responseCode: {}, responseBody: {}",
                        res.getStatusCode(), res.getBody());
                return orderConfirm.getData();
            } else {
                Logger.application.warn("Cannot get Order against orderId: {}", orderId);
            }

        } catch (RestClientException e) {
            Logger.application.error("Error getting Order id:{}, url: {}", orderId, url, e);
            return null;
        }
        return null;
    }
}
