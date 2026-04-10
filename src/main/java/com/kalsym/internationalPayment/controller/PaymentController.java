package com.kalsym.internationalPayment.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.*;
import com.kalsym.internationalPayment.model.dao.*;
import com.kalsym.internationalPayment.model.dao.Order.OrderConfirm;
import com.kalsym.internationalPayment.model.enums.PaymentStatus;
import com.kalsym.internationalPayment.model.enums.TransactionEnum;
import com.kalsym.internationalPayment.repositories.*;
import com.kalsym.internationalPayment.services.*;
import com.kalsym.internationalPayment.utility.HttpResponse;
import com.kalsym.internationalPayment.utility.Logger;

import io.swagger.v3.oas.annotations.Operation;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    WSPRequestService wspRequestService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    PaymentService paymentService;

    @Autowired
    UserService userService;

    @Autowired
    CountryRepository countryRepository;

    @Autowired
    ProductVariantRepository productVariantDb;

    @Autowired
    SymplifiedOrderService symplifiedOrderService;

    @Autowired
    PaymentChannelRepository paymentChannelRepository;

    @Autowired
    ProductVariantPaymentChannelRepository productVariantPaymentChannelRepository;

    @Autowired
    ProductVariantRepository productVariantRepository;

    
    /**
     * ------------------------------------------------------------------------------------------------------------------------------------------------
     * Payment related endpoints
     * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    @Operation(summary = "Get payment channel info", description = "To retrieve payment channel info by ID")
    @GetMapping(path = { "/channel/{id}" })
    public ResponseEntity<HttpResponse> getPaymentChannel(HttpServletRequest request,
            @PathVariable("id") Long id) throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());

        // Retrieve all active payment channels (where status is 'true' or '1')
        List<PaymentChannel> paymentChannels = paymentChannelRepository.findByStatus(true);

        // Retrieve ProductVariantPaymentChannels for the given product variant ID and
        // enabled = true
        List<ProductVariantPaymentChannel> channels = productVariantPaymentChannelRepository
                .findByEnabledAndProductVariantId(true, id);

        // Step 1: Collect the paymentChannelIds from the ProductVariantPaymentChannels
        Set<Long> enabledChannelIds = channels.stream()
                .map(variantChannel -> variantChannel.getId().getPaymentChannelId())
                .collect(Collectors.toSet());

        // Step 2: Add parent channels of the selected child channels (if parent exists)
        Set<Long> parentIds = paymentChannels.stream()
                .filter(channel -> enabledChannelIds.contains(channel.getId()) && channel.getParentId() != null)
                .map(PaymentChannel::getParentId)
                .collect(Collectors.toSet());

        // Combine both enabledChannelIds and parentIds
        enabledChannelIds.addAll(parentIds);

        // Step 3: Filter payment channels to include both child and parent channels
        List<PaymentChannel> activePaymentChannels = paymentChannels.stream()
                .filter(channel -> enabledChannelIds.contains(channel.getId()))
                .collect(Collectors.toList());

        // Step 4: Separate parents and children
        Map<Long, Map<String, Object>> parents = new HashMap<>();
        Map<Long, List<PaymentChannel>> children = new HashMap<>();

        for (PaymentChannel channel : activePaymentChannels) {
            if (channel.getParentId() == null) {
                // This is a parent channel
                Map<String, Object> parentData = new HashMap<>();
                parentData.put("channelName", channel.getChannelName());
                parents.put(channel.getId(), parentData);
            } else {
                // Convert parentId to Long if needed (in case it's Integer)
                Long parentId = channel.getParentId().longValue();
                // This is a child channel
                children.computeIfAbsent(parentId, k -> new ArrayList<>()).add(channel);
            }
        }

        // Step 5: Attach children to their parents
        children.forEach((parentId, childList) -> {
            if (parents.containsKey(parentId)) {
                parents.get(parentId).put("children", childList);
            }
        });

        // Step 6: Set the response data
        response.setData(parents.values());
        response.setStatus(HttpStatus.OK);

        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @Operation(summary = "[TEST] Mtrade payment method", description = "To trigger Mtrade payment method")
    @PostMapping(path = { "/callback-test/{transactionId}" })
    public ResponseEntity<HttpResponse> callbackTest(HttpServletRequest request,
            @PathVariable("transactionId") String transactionId,
            @RequestParam String paymentChannel) throws Exception {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "callbackTest";

        Optional<Transaction> optionalTransaction = transactionRepository.findByTransactionId(transactionId);

        if (!optionalTransaction.isPresent()) {
            response.setStatus(HttpStatus.NOT_FOUND);
            response.setMessage("Transaction not found");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        Transaction transaction = optionalTransaction.get();

        if (!PaymentStatus.PENDING.equals(transaction.getPaymentStatus())
                || !"PENDING".equals(transaction.getStatus())) {
            response.setData(transaction);
            response.setStatus(HttpStatus.OK, "Transaction already processed");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        transaction.setPaymentStatus(PaymentStatus.PAID);
        transaction.setStatus("PAID");

        ProductVariant productVariant = productVariantDb.findById(transaction.getProductVariantId()).get();

        if (transaction.getTransactionType().equals(TransactionEnum.ORDER)
                || transaction.getTransactionType().equals(TransactionEnum.COUPON)) {
            // Symplified order service
            OrderConfirm res = paymentService.groupOrderUpdateStatus(transaction.getTransactionId(),
                    transaction.getSpOrderId(), "PAYMENT_CONFIRMED", "", "UAT transaction", paymentChannel);

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "callbackTest()",
                    transaction.getTransactionId(), "groupOrderUpdateStatus response ::" + res);
        } else {

            MtradePaymentRequest mtradePaymentRequest = new MtradePaymentRequest();
            mtradePaymentRequest.setSenderMsisdn(transaction.getPhoneNo());
            mtradePaymentRequest.setProductCode(productVariant.getWspProductCode());
            mtradePaymentRequest.setProductId(productVariant.getProductId());
            mtradePaymentRequest.setPayAmount(String.valueOf(transaction.getDenoAmount()));
            mtradePaymentRequest.setVariantType(productVariant.getVariantType());
            mtradePaymentRequest.setExtra1(transaction.getExtra1());
            mtradePaymentRequest.setExtra2(transaction.getExtra2());
            mtradePaymentRequest.setExtra3(transaction.getExtra3());
            mtradePaymentRequest.setExtra4(transaction.getExtra4());
            mtradePaymentRequest.setBillPhoneNumber(transaction.getBillPhoneNumber());
            mtradePaymentRequest.setRecipientMsisdn(transaction.getAccountNo());
            mtradePaymentRequest.setAccountNo(transaction.getAccountNo());
            mtradePaymentRequest.setCustomerTransactionId(transaction.getTransactionId());

            try {
                MtradePaymentResponse responseData = wspRequestService
                        .requestPaymentType(mtradePaymentRequest);

                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "WSP Request : " + responseData);

                transaction.setWspTransactionId(responseData.getSystemTransactionId());
                transaction.setTransactionErrorCode(responseData.getResponseCode());
                transaction.setErrorDescription(responseData.getResponseDescription());

                String responseCode = responseData.getResponseCode();
                if ("000".equals(responseCode)) {
                    transaction.setStatus("PAID");
                } else if ("001".equals(responseCode) || "203".equals(responseCode)
                        || "204".equals(responseCode)) {
                    transaction.setStatus("PROCESSING");
                } else {
                    transaction.setStatus("FAILED");
                }

                Transaction savedTransaction = transactionRepository.save(transaction);
                response.setData(savedTransaction);
                response.setMessage("Success");
                response.setStatus(HttpStatus.OK);

                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        savedTransaction.getTransactionId(), "Request Payment Confirmation Request::",
                        savedTransaction);

                return ResponseEntity.status(HttpStatus.OK).body(response);

            } catch (Exception ex) {
                transaction.setStatus("FAILED");

                if (ex.getMessage() == null || ex.getMessage().isEmpty())
                    transaction.setErrorDescription("eByzarr System Failure");
                else
                    transaction.setErrorDescription(ex.getMessage());

                Transaction saveTransaction = transactionRepository.save(transaction);
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        transaction.getTransactionId(), "Saved with exception : ", saveTransaction);
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        transaction.getTransactionId(), "Update WSP Exception ", ex.getMessage());
                response.setData(saveTransaction);
                response.setMessage("Failed");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
        }

        transactionRepository.save(transaction);

        response.setMessage("Success");
        response.setData("DONE");
        response.setStatus(HttpStatus.OK);

        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @Operation(summary = "Mtrade payment method", description = "To trigger Mtrade paymet method")
    @PostMapping(path = { "/callback" })
    public ResponseEntity<HttpResponse> callback(HttpServletRequest request, @RequestHeader Map<String, String> headers,
            @RequestBody MultiValueMap<String, String> formData) throws Exception {

        Date now = new Date();

        String host = request.getHeader("Host");

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION,
                "Incoming request Host Callback: " + host);

        String logprefix = request.getRequestURI();

        HttpResponse response = new HttpResponse(request.getRequestURI());

        headers.forEach((key, value) -> {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Headers: " + (String.format("Header '%s' = %s", key, value)));

        });

        Set<String> keys = formData.keySet();

        for (String key : keys) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Key = " + key);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Values = " + formData.get(key));
        }

        try {

            String requestURL = request.getRequestURL() != null ? request.getRequestURL().toString() : "Unknown";
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Request URL: " + requestURL);

            String transactionId = extractParam(formData, "OrderDescription");
            String errorCode = extractParam(formData, "ResponseCode");
            String refId = extractParam(formData, "OrderReference");
            String errorMessage = extractParam(formData, "TransactionStatusText");
            String transactionType = extractParam(formData, "TransactionTypeText");
            String transactionDate = extractParam(formData, "TransactionDate");
            String providerError = extractParam(formData, "ProcessorMessage");

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Body: " + formData);
            Optional<Transaction> transaction = transactionRepository
                    .findByTransactionId(transactionId);

            if (transaction.isPresent()) {
                Transaction t = transaction.get();

                if (!PaymentStatus.PENDING.equals(t.getPaymentStatus()) || !"PENDING".equals(t.getStatus())) {
                    response.setData(transaction);
                    response.setStatus(HttpStatus.OK, "Transaction already processed");
                    return ResponseEntity.status(response.getStatus()).body(response);
                }

                Date parsedDate = now;
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("d/M/yyyy h:mm:ss a");
                    parsedDate = dateFormat.parse(transactionDate);
                } catch (ParseException e) {
                    // Handle parsing exception by setting the date to the current date and time
                }

                t.setCallbackDate(now);
                t.setUpdatedDate(now);
                t.setPaidDate(parsedDate);
                t.setPaymentTransactionId(refId); // Get Payment Transaction Id From The Payment Provider
                t.setPaymentErrorCode(errorCode);
                t.setPaymentDescription(errorMessage);
                t.setPaymentProviderError(providerError);

                if (transactionType != null && !transactionType.isEmpty()) {
                    t.setPaymentChannel("OZO-" + transactionType);
                }

                t = transactionRepository.save(t);

                handleCallbackProcessing(request, t, errorCode, transactionType, errorMessage, logprefix);
                response.setData("ACK");
                response.setMessage("ACK");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                response.setStatus(HttpStatus.NOT_FOUND, "Invalid payload");
                return ResponseEntity.status(response.getStatus()).body(response);
            }

        } catch (Exception ex) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Error sending message : ",
                    ex);
            response.setMessage(ex.getMessage());
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);
        }

    }

    @Async
    private void handleCallbackProcessing(HttpServletRequest request, Transaction t, String errorCode,
            String transactionType, String errorMessage, String logprefix) {

        if ("000".equals(errorCode)) {
            t.setPaymentStatus(PaymentStatus.PAID);
        } else if ("003".equals(errorCode)) {
            t.setPaymentStatus(PaymentStatus.PENDING);
        } else {
            t.setPaymentStatus(PaymentStatus.FAILED);
            t.setStatus("FAILED");
            t.setErrorDescription("PAYMENT FAILED");
        }
        transactionRepository.save(t);

        // 000 is PAID
        if ("000".equals(errorCode)) {
            Optional<ProductVariant> variantOpt = productVariantDb.findById(t.getProductVariantId());
            if (!variantOpt.isPresent()) {
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Product variant not found for ID: " + t.getProductVariantId());
                return;
            }
            ProductVariant productVariant = variantOpt.get();

            if (!t.getTransactionType().equals(TransactionEnum.ORDER)
                    && !t.getTransactionType().equals(TransactionEnum.COUPON)) {

                MtradePaymentRequest mtradePaymentRequest = new MtradePaymentRequest();
                mtradePaymentRequest.setSenderMsisdn(t.getPhoneNo());
                mtradePaymentRequest.setProductCode(productVariant.getWspProductCode());
                mtradePaymentRequest.setProductId(productVariant.getProductId());
                mtradePaymentRequest.setPayAmount(String.valueOf(t.getDenoAmount()));
                mtradePaymentRequest.setVariantType(productVariant.getVariantType());
                mtradePaymentRequest.setExtra1(t.getExtra1());
                mtradePaymentRequest.setExtra2(t.getExtra2());
                mtradePaymentRequest.setExtra3(t.getExtra3());
                mtradePaymentRequest.setExtra4(t.getExtra4());
                mtradePaymentRequest.setBillPhoneNumber(t.getBillPhoneNumber());
                mtradePaymentRequest.setAccountNo(t.getAccountNo());
                mtradePaymentRequest.setRecipientMsisdn(t.getAccountNo());
                mtradePaymentRequest.setCustomerTransactionId(t.getTransactionId());

                try {
                    MtradePaymentResponse responseData = wspRequestService
                            .requestPaymentType(mtradePaymentRequest);

                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "WSP Request : " + responseData);

                    t.setWspTransactionId(responseData.getSystemTransactionId());
                    t.setTransactionErrorCode(responseData.getResponseCode());
                    t.setErrorDescription(responseData.getResponseDescription());

                    String responseCode = responseData.getResponseCode();
                    if ("000".equals(responseCode)) {
                        t.setStatus("PAID");

                    } else if ("001".equals(responseCode) || "203".equals(responseCode)
                            || "204".equals(responseCode)) {
                        t.setStatus("PROCESSING");
                    } else {
                        t.setStatus("FAILED");
                    }

                    Transaction savedTransaction = transactionRepository.save(t);

                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            savedTransaction.getTransactionId(), "Request Payment Confirmation Request::",
                            savedTransaction);

                } catch (Exception ex) {
                    t.setStatus("FAILED");

                    if (ex.getMessage() == null || ex.getMessage().isEmpty())
                        t.setErrorDescription("eByzarr System Failure");
                    else
                        t.setErrorDescription(ex.getMessage());

                    Transaction saveTransaction = transactionRepository.save(t);
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            t.getTransactionId(), "Saved with exception : ", saveTransaction);
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            t.getTransactionId(), "Update WSP Exception ", ex.getMessage());

                }
            } else {
                // CALL SYMPLIFIED BACKEND
                try {
                    OrderConfirm res = paymentService.groupOrderUpdateStatus(t.getTransactionId(),
                            t.getSpOrderId(), "PAYMENT_CONFIRMED", "", errorMessage, "OZO-" + transactionType);

                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            t.getTransactionId(), "res::", res);

                    t.setStatus("PAID");

                    Transaction saveTransaction = transactionRepository.save(t);

                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "Request Payment Confirmation Request::",
                            saveTransaction);

                } catch (Exception ex) {
                    t.setStatus("PAID");
                    t.setErrorDescription("Failed to Update Order Services");

                    Transaction saveTransaction = transactionRepository.save(t);

                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "Exception ::" + ex.getMessage());

                }
            }
        } else if ("003".equals(errorCode)) {
            t.setStatus("PENDING");
            t.setErrorDescription("PAYMENT PENDING");
            Transaction saveTransaction = transactionRepository.save(t);

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    saveTransaction.getTransactionId(), "Request Payment Confirmation Request");

            if (t.getTransactionType().equals(TransactionEnum.ORDER)
                    || t.getTransactionType().equals(TransactionEnum.COUPON)) {
                // CALL SYMPLIFIED BACKEND
                try {
                    OrderConfirm res = paymentService.groupOrderUpdateStatus(t.getTransactionId(),
                            t.getSpOrderId(), "PAYMENT_FAILED", "", errorMessage, "OZO-" + transactionType);
                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "res::", res);
                } catch (Exception ex) {
                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "Exception ::" + ex.getMessage());
                }
            }
        } else {
            Transaction saveTransaction = transactionRepository.save(t);

            if (t.getTransactionType().equals(TransactionEnum.ORDER)
                    || t.getTransactionType().equals(TransactionEnum.COUPON)) {
                // CALL SYMPLIFIED BACKEND
                try {
                    OrderConfirm res = paymentService.groupOrderUpdateStatus(t.getTransactionId(),
                            t.getSpOrderId(), "PAYMENT_FAILED", "", errorMessage, "OZO-" + transactionType);
                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "res::", res);
                } catch (Exception ex) {
                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "Exception ::" + ex.getMessage());
                }
            }

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    saveTransaction.getTransactionId(), "Payment Confirmation Callback, Payment Failed: ",
                    saveTransaction);
        }
    }

    private String extractParam(MultiValueMap<String, String> formData, String key) {
        List<String> list = (List<String>) formData.get(key);
        if (list != null && !list.isEmpty()) {
            String value = list.get(0);
            return value;
        }
        return null;
    }

    @Operation(summary = "", description = "To verify tranasaction")
    @PostMapping("/verify")
    public ResponseEntity<HttpResponse> verifyTransaction(HttpServletRequest request,
            @RequestParam String transactionId) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logPrefix = "verifyTransaction";

        try {
            Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);

            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                Optional<ProductVariant> productVariantOptional = productVariantDb
                        .findById(transaction.getProductVariantId());

                if (productVariantOptional.isPresent()) {
                    ProductVariant productVariant = productVariantOptional.get();

                        // Process genuine transaction by making a WSP request
                        MtradePaymentRequest mtradePaymentRequest = new MtradePaymentRequest();
                        mtradePaymentRequest.setSenderMsisdn(transaction.getPhoneNo());
                        mtradePaymentRequest.setProductCode(productVariant.getWspProductCode());
                        mtradePaymentRequest.setProductId(productVariant.getProductId());
                        mtradePaymentRequest.setPayAmount(String.valueOf(transaction.getDenoAmount()));
                        mtradePaymentRequest.setVariantType(productVariant.getVariantType());
                        mtradePaymentRequest.setExtra1(transaction.getExtra1());
                        mtradePaymentRequest.setExtra2(transaction.getExtra2());
                        mtradePaymentRequest.setExtra3(transaction.getExtra3());
                        mtradePaymentRequest.setExtra4(transaction.getExtra4());
                        mtradePaymentRequest.setBillPhoneNumber(transaction.getBillPhoneNumber());
                        mtradePaymentRequest.setAccountNo(transaction.getAccountNo());
                        mtradePaymentRequest.setRecipientMsisdn(transaction.getAccountNo());
                        mtradePaymentRequest.setCustomerTransactionId(transaction.getTransactionId());

                        MtradePaymentResponse responseData;
                        try {
                            responseData = wspRequestService
                                    .requestPaymentType(mtradePaymentRequest);
                            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                    "WSP Request : " + responseData);

                            transaction.setWspTransactionId(responseData.getSystemTransactionId());
                            transaction.setTransactionErrorCode(responseData.getResponseCode());
                            transaction.setErrorDescription(responseData.getResponseDescription());

                            String responseCode = responseData.getResponseCode();
                            if ("000".equals(responseCode)) {
                                transaction.setStatus("PAID");

                            } else if ("001".equals(responseCode) || "203".equals(responseCode)
                                    || "204".equals(responseCode)) {
                                transaction.setStatus("PROCESSING");
                            } else {
                                transaction.setStatus("FAILED");
                            }

                            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                    transaction.getTransactionId(), " Request Payment Confirmation Request::");

                        } catch (Exception e) {
                            transaction.setStatus("FAILED");

                            if (e.getMessage() == null || e.getMessage().isEmpty())
                                transaction.setErrorDescription("eByzarr System Failure");
                            else
                                transaction.setErrorDescription(e.getMessage());

                            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                    transaction.getTransactionId(), "Saved with exception : ", transaction);
                            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                    transaction.getTransactionId(), "Update WSP Exception ", e.getMessage());
                        }
                        Transaction savedTransaction = transactionRepository.save(transaction);
                        response.setStatus(HttpStatus.OK);
                        response.setData(savedTransaction);
                } else {
                    response.setStatus(HttpStatus.NOT_FOUND,
                            "Product Variant not found: " + transaction.getProductVariantId());
                }

            } else {
                response.setStatus(HttpStatus.NOT_FOUND, "Transaction not found: " + transactionId);
            }

        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "Exception: ",
                    e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /* @Operation(summary = "Get symplified history", description = "To retrieve sympflied history from order service")
    @GetMapping(path = { "/symplified-history" })
    public ResponseEntity<?> getSymplifiedTransactionHistory(HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(defaultValue = "true") boolean onlyVoucher,
            @RequestParam(required = false) String search) throws Exception {
        String logprefix = request.getRequestURI() + " get transaction history ";

        HttpResponse response = new HttpResponse(request.getRequestURI());

        User user = userService.getUser(request.getHeader(HEADER_STRING));

        if (user == null) {
            response.setStatus(HttpStatus.NOT_FOUND);
            response.setMessage("User Not Found");
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "User Not Found");

            return ResponseEntity.status(response.getStatus()).body(response);
        }

        Transaction transaction = new Transaction();
        transaction.setUserId(user.getId());

        ExampleMatcher matcher = ExampleMatcher
                .matchingAll()
                .withIgnoreCase()
                .withMatcher("userId", new ExampleMatcher.GenericPropertyMatcher().exact())
                .withIgnoreNullValues()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
        Example<Transaction> example = Example.of(transaction, matcher);
        List<Transaction> transactions = transactionRepository
                .findAll(getSymplifiedTransactionHistoryByUser(from, to, example, status, paymentStatus));

        if (!transactions.isEmpty()) {
            List<String> spOrderIds = transactions.stream()
                    .map(Transaction::getSpOrderId)
                    .collect(Collectors.toList());

            try {
                return symplifiedOrderService.getOrdersByGroupIds(spOrderIds, onlyVoucher, page, pageSize, search);

            } catch (Exception e) {
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Exception");
                response.setStatus(HttpStatus.OK);
                response.setData(Collections.emptyList());
                return ResponseEntity.status(response.getStatus()).body(response);
            }
        }
        response.setData(transactions);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    } */
    
    public static Specification<Transaction> getSymplifiedTransactionHistoryByUser(
            Date from, Date to, Example<Transaction> example, String status,
            PaymentStatus paymentStatus) {
        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();

            if (from != null && to != null) {

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(to);

                // Add one day to the current date
                calendar.add(Calendar.DAY_OF_MONTH, 1);

                // Get the updated date
                Date updatedDate = calendar.getTime();
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdDate"), from));
                predicates.add(builder.lessThanOrEqualTo(root.get("createdDate"), updatedDate));
            }

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            if (paymentStatus != null) {
                predicates.add(builder.equal(root.get("paymentStatus"), paymentStatus));
            }

            // Get records with spOrderId
            predicates.add(builder.isNotNull(root.get("spOrderId")));

            predicates.add(builder.equal(root.get("transactionType"), TransactionEnum.COUPON));

            // Exclude records where both status and paymentStatus are PENDING
            predicates.add(builder.not(
                    builder.and(
                            builder.equal(root.get("status"), "PENDING"),
                            builder.equal(root.get("paymentStatus"), PaymentStatus.PENDING))));

            predicates.add(QueryByExamplePredicateBuilder.getPredicate(root, builder, example));

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
    
    @Operation(summary = "Ozopay manual trigger callback", description = "To manually trigger ozopay callback")
    @PostMapping(path = { "/ozopay/manual-callback/{transactionId}" })
    public ResponseEntity<HttpResponse> manualCallback(HttpServletRequest request,
            @PathVariable("transactionId") String transactionId,
            @RequestParam(required = true) String errorCode,
            @RequestParam(required = true) String refId,
            @RequestParam(required = true) String errorMessage,
            @RequestParam(required = true) String transactionType,
            @RequestParam(required = true) String transactionDate

    ) {

        String logprefix = request.getRequestURI() + " manualCallback() ";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        Transaction t = transactionRepository.findByTransactionId(transactionId).get();

        if (!PaymentStatus.PENDING.equals(t.getPaymentStatus()) || !"PENDING".equals(t.getStatus())) {
            response.setData(t);
            response.setStatus(HttpStatus.OK, "Transaction already processed");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        Date parsedDate = new Date();
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("d/M/yyyy h:mm:ss a");
            parsedDate = dateFormat.parse(transactionDate);

            t.setPaidDate(parsedDate);
        } catch (ParseException e) {
            // Handle parsing exception by setting the date to the current date and time
        }

        t.setPaidDate(parsedDate);
        t.setUpdatedDate(new Date());
        t.setPaymentTransactionId(refId);
        t.setPaymentErrorCode(errorCode);
        t.setPaymentDescription(errorMessage);
        if (!transactionType.isEmpty() && transactionType != null)
            t.setPaymentChannel("OZO-" + transactionType);
        // 000 is PAID
        if (errorCode.equals("000")) {

            t.setPaymentStatus(PaymentStatus.PAID);

            if (!t.getTransactionType().equals(TransactionEnum.ORDER)
                    && !t.getTransactionType().equals(TransactionEnum.COUPON)) {
                ProductVariant productVariant = productVariantDb.findById(t.getProductVariantId()).get();

                MtradePaymentRequest mtradePaymentRequest = new MtradePaymentRequest();
                mtradePaymentRequest.setSenderMsisdn(t.getPhoneNo());
                mtradePaymentRequest.setProductCode(productVariant.getWspProductCode());
                mtradePaymentRequest.setProductId(productVariant.getProductId());
                mtradePaymentRequest.setPayAmount(String.valueOf(t.getDenoAmount()));
                mtradePaymentRequest.setVariantType(productVariant.getVariantType());
                mtradePaymentRequest.setExtra1(t.getExtra1());
                mtradePaymentRequest.setExtra2(t.getExtra2());
                mtradePaymentRequest.setExtra3(t.getExtra3());
                mtradePaymentRequest.setExtra4(t.getExtra4());
                mtradePaymentRequest.setBillPhoneNumber(t.getBillPhoneNumber());
                mtradePaymentRequest.setAccountNo(t.getAccountNo());
                mtradePaymentRequest.setRecipientMsisdn(t.getAccountNo());
                mtradePaymentRequest.setAccountNo(t.getAccountNo());
                mtradePaymentRequest.setCustomerTransactionId(t.getTransactionId());

                try {
                    MtradePaymentResponse responseData = wspRequestService
                            .requestPaymentType(mtradePaymentRequest);

                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "WSP Request : " + responseData);

                    t.setWspTransactionId(responseData.getSystemTransactionId());
                    t.setTransactionErrorCode(responseData.getResponseCode());
                    t.setErrorDescription(responseData.getResponseDescription());

                    String responseCode = responseData.getResponseCode();
                    if ("000".equals(responseCode)) {
                        t.setStatus("PAID");
                    } else if ("001".equals(responseCode) || "203".equals(responseCode)
                            || "204".equals(responseCode)) {
                        t.setStatus("PROCESSING");
                    } else {
                        t.setStatus("FAILED");
                    }

                    Transaction savedTransaction = transactionRepository.save(t);
                    response.setData(savedTransaction);

                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            savedTransaction.getTransactionId(), "Request Payment Confirmation Request::",
                            savedTransaction);

                    return ResponseEntity.status(HttpStatus.OK).body(response);

                } catch (Exception ex) {
                    t.setStatus("FAILED");

                    if (ex.getMessage() == null || ex.getMessage().isEmpty())
                        t.setErrorDescription("eByzarr System Failure");
                    else
                        t.setErrorDescription(ex.getMessage());

                    Transaction saveTransaction = transactionRepository.save(t);
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            t.getTransactionId(), "Saved with exception : ", saveTransaction);
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            t.getTransactionId(), "Update WSP Exception ", ex.getMessage());
                    response.setData(saveTransaction);
                    return ResponseEntity.status(HttpStatus.OK).body(response);
                }
            } else {
                // CALL SYMPLIFIED BACKEND
                try {
                    OrderConfirm res = paymentService.groupOrderUpdateStatus(t.getTransactionId(),
                            t.getSpOrderId(), "PAYMENT_CONFIRMED", "", errorMessage, "OZO-" + transactionType);

                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            t.getTransactionId(), "res::", res);

                    t.setStatus("PAID");

                    Transaction saveTransaction = transactionRepository.save(t);
                    response.setData(saveTransaction);

                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "Request Payment Confirmation Request::",
                            saveTransaction);

                } catch (Exception ex) {
                    t.setStatus("PAID");
                    t.setErrorDescription("Failed to Update Order Services");

                    Transaction saveTransaction = transactionRepository.save(t);

                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "Exception ::" + ex.getMessage());
                    response.setData(saveTransaction);

                }
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }

        } else if (errorCode.equals("003")) {

            t.setStatus("PENDING");
            t.setPaymentStatus(PaymentStatus.PENDING);
            t.setErrorDescription("PAYMENT PENDING");
            Transaction saveTransaction = transactionRepository.save(t);

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    saveTransaction.getTransactionId(), "Request Payment Confirmation Request");
            response.setData(saveTransaction);

            if (t.getTransactionType().equals(TransactionEnum.ORDER)
                    || t.getTransactionType().equals(TransactionEnum.COUPON)) {
                // CALL SYMPLIFIED BACKEND
                try {
                    OrderConfirm res = paymentService.groupOrderUpdateStatus(t.getTransactionId(),
                            t.getSpOrderId(), "PAYMENT_FAILED", "", errorMessage, "OZO-" + transactionType);
                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "res::", res);
                } catch (Exception ex) {
                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "Exception ::" + ex.getMessage());
                }
            }
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } else {
            t.setStatus("FAILED");
            t.setPaymentStatus(PaymentStatus.FAILED);
            t.setErrorDescription("PAYMENT FAILED");

            Transaction saveTransaction = transactionRepository.save(t);

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    saveTransaction.getTransactionId(), "Payment Confirmation Callback, Payment Failed: ",
                    saveTransaction);

            response.setData(saveTransaction);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

    }

    @Operation(summary = "MMpay payment method", description = "To trigger MMPay payment method")
    @PostMapping(path = { "/mmpayment/{transactionId}" })
    public ResponseEntity<HttpResponse> mmPaymentTransaction(HttpServletRequest request,
            @PathVariable("transactionId") String transactionId) throws Exception {
        HttpResponse response = new HttpResponse(request.getRequestURI());

        Transaction transaction = transactionRepository.findByTransactionId(transactionId).get();

        MMResponse mmResponse = paymentService.requestPayment(transaction);
        if (mmResponse.getCode().equals("000")) {
            transaction.setErrorDescription(mmResponse.getMessage());
            transaction.setOtpReferenceNo(mmResponse.getOtpReferenceNo());
            transaction.setTransactionErrorCode(mmResponse.getCode());
            response.setMessage("Success");
            response.setData(mmResponse);
            response.setStatus(HttpStatus.OK);
        } else {
            response.setStatus(HttpStatus.EXPECTATION_FAILED);
            if (mmResponse.getMessage() != null && !mmResponse.getMessage().isEmpty()) {
                response.setMessage(mmResponse.getMessage());
            }
            response.setData(mmResponse);
            transaction.setErrorDescription(mmResponse.getMessage());
            transaction.setTransactionErrorCode(mmResponse.getCode());
            transaction.setPaymentStatus(PaymentStatus.FAILED);
            transaction.setStatus("FAILED");
        }
        transactionRepository.save(transaction);

        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
     /**
     * ------------------------------------------------------------------------------------------------------------------------------------------------
     * OTP related endpoints
     * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    @Operation(summary = "Validate MMpay OTP", description = "Validate OTP related to MMpay payment method")
    @PostMapping(path = { "/otp/validate/{transactionId}" })
    public ResponseEntity<HttpResponse> validateOtp(HttpServletRequest request,
            @PathVariable("transactionId") String transactionId, @RequestParam String otpNo) throws Exception {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = request.getRequestURI() + " validateOtp() ";
        Date now = new Date();

        Transaction transaction = transactionRepository.findByTransactionId(transactionId).get();

        if (!PaymentStatus.PENDING.equals(transaction.getPaymentStatus())
                || !"PENDING".equals(transaction.getStatus())) {
            response.setData(transaction);
            response.setStatus(HttpStatus.OK, "Transaction already processed");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        transaction.setOtpNo(otpNo);

        transaction.setCallbackDate(now);
        transaction.setUpdatedDate(now);
        transaction.setPaidDate(now);
        transaction.setPaymentChannel("MMWALLET");

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "TransactionId", transaction);

        if (transaction.getPaymentStatus().equals(PaymentStatus.PENDING)) {
            MMResponse mmResponse = paymentService.validateOtp(transaction);
            transaction.setPaymentTransactionId(mmResponse.getSpTransactionId()); // Get Payment Transaction Id From The
                                                                                  // Payment Provider
            transaction.setPaymentErrorCode(mmResponse.getCode());
            transaction.setPaymentDescription(mmResponse.getMessage());
            if ("000".equals(mmResponse.getCode())) {

                transaction.setPaymentStatus(PaymentStatus.PAID);

                if (!transaction.getTransactionType().equals(TransactionEnum.ORDER)
                        && !transaction.getTransactionType().equals(TransactionEnum.COUPON)) {

                    ProductVariant productVariant = productVariantDb.findById(transaction.getProductVariantId()).get();
                    MtradePaymentRequest mtradePaymentRequest = new MtradePaymentRequest();
                    mtradePaymentRequest.setSenderMsisdn(transaction.getPhoneNo());
                    mtradePaymentRequest.setAccountNo(transaction.getAccountNo());
                    mtradePaymentRequest.setProductCode(productVariant.getWspProductCode());
                    mtradePaymentRequest.setProductId(productVariant.getProductId());
                    mtradePaymentRequest.setPayAmount(String.valueOf(transaction.getDenoAmount()));
                    mtradePaymentRequest.setVariantType(productVariant.getVariantType());
                    mtradePaymentRequest.setBillPhoneNumber(transaction.getBillPhoneNumber());
                    mtradePaymentRequest.setRecipientMsisdn(transaction.getAccountNo());
                    mtradePaymentRequest.setAccountNo(transaction.getAccountNo());
                    mtradePaymentRequest.setCustomerTransactionId(transaction.getTransactionId());
                    mtradePaymentRequest.setExtra1(transaction.getExtra1());
                    mtradePaymentRequest.setExtra2(transaction.getExtra2());
                    mtradePaymentRequest.setExtra3(transaction.getExtra3());
                    mtradePaymentRequest.setExtra4(transaction.getExtra4());

                    try {
                        MtradePaymentResponse responseData = wspRequestService.requestPaymentType(mtradePaymentRequest);
                        transaction.setWspTransactionId(responseData.getSystemTransactionId());
                        transaction.setErrorDescription(responseData.getResponseDescription());
                        transaction.setTransactionErrorCode(responseData.getResponseCode()); // t.setC(payment.get().getOrderDescription());

                        String responseCode = responseData.getResponseCode();
                        if ("000".equals(responseCode)) {
                            transaction.setStatus("PAID");
                        } else if ("001".equals(responseCode) || "203".equals(responseCode)
                                || "204".equals(responseCode)) {
                            transaction.setStatus("PROCESSING");
                        } else {
                            transaction.setStatus("FAILED");
                        }

                        Transaction savedTransaction = transactionRepository.save(transaction);
                        response.setData(savedTransaction);

                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                savedTransaction.getTransactionId(), "Request Payment Confirmation Request::",
                                savedTransaction);

                        return ResponseEntity.status(HttpStatus.OK).body(response);
                    } catch (Exception ex) {

                        transaction.setStatus("FAILED");
                        if (ex.getMessage() == null || ex.getMessage().isEmpty())
                            transaction.setErrorDescription("eByzarr System Failure");
                        else
                            transaction.setErrorDescription(ex.getMessage());

                        Transaction saveTransaction = transactionRepository.save(transaction);

                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                saveTransaction.getTransactionId(), "Saved with exception : ", saveTransaction);

                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                transaction.getTransactionId(), "Update WSP Exception ", ex.getMessage());

                        response.setData(saveTransaction);
                        return ResponseEntity.status(HttpStatus.OK).body(response);
                    }

                } else {
                    // CALL SYMPLIFIED BACKEND
                    try {
                        OrderConfirm res = paymentService.groupOrderUpdateStatus(transaction.getTransactionId(),
                                "G" + transaction.getSpOrderId(), "PAYMENT_CONFIRMED", "",
                                "Validated Otp Success. " + mmResponse.getMessage(), "MMWALLET");

                        transaction.setStatus("PAID");

                        Transaction saveTransaction = transactionRepository.save(transaction);
                        response.setData(saveTransaction);

                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                saveTransaction.getTransactionId(), "Request Payment Confirmation Request::",
                                saveTransaction);

                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                transaction.getTransactionId(), "groupOrderUpdateStatus reponse ::" + res);
                    } catch (Exception ex) {

                        transaction.setStatus("PAID");
                        transaction.setErrorDescription("Failed to Update Order Services");

                        Transaction saveTransaction = transactionRepository.save(transaction);

                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                saveTransaction.getTransactionId(), "Exception ::" + ex.getMessage());

                        response.setData(saveTransaction);
                    }
                    return ResponseEntity.status(HttpStatus.OK).body(response);
                }
            } else {
                response.setStatus(HttpStatus.EXPECTATION_FAILED);
                response.setData(mmResponse);

                transaction.setPaymentStatus(PaymentStatus.FAILED);
                transaction.setStatus(PaymentStatus.FAILED.name());

                transactionRepository.save(transaction);

                if (transaction.getTransactionType().equals(TransactionEnum.ORDER)
                        || transaction.getTransactionType().equals(TransactionEnum.COUPON)) {
                    // CALL SYMPLIFIED BACKEND
                    try {
                        OrderConfirm res = paymentService.groupOrderUpdateStatus(transaction.getTransactionId(),
                                "G" + transaction.getSpOrderId(), "PAYMENT_FAILED", "",
                                "OTP FAILED. " + mmResponse.getMessage(), "MMWALLET");
                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                transaction.getTransactionId(), "groupOrderUpdateStatus response ::" + res);
                    } catch (Exception ex) {
                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                transaction.getTransactionId(), "Exception ::" + ex.getMessage());
                    }
                }
            }
        } else {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    transaction.getTransactionId(), "Transaction Payment Confirmation Request, ",
                    "This transaction already " + transaction.getPaymentStatus().toString());
            response.setData(transaction);
            response.setMessage("This transaction already " + transaction.getPaymentStatus().toString());
            response.setStatus(HttpStatus.EXPECTATION_FAILED);
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Resend MMPay OTP", description = "Resend OTP related MMpay payment method")
    @PostMapping(path = { "/otp/resend/{transactionId}" })
    public ResponseEntity<HttpResponse> resendOtp(HttpServletRequest request,
            @PathVariable("transactionId") String transactionId) throws Exception {
        HttpResponse response = new HttpResponse(request.getRequestURI());

        Transaction transaction = transactionRepository.findByTransactionId(transactionId).get();

        MMResponse mmResponse = paymentService.resendOtp(transaction);
        if (mmResponse.getCode().equals("000")) {

            transaction.setErrorDescription(mmResponse.getMessage());
            transaction.setTransactionErrorCode(mmResponse.getCode());
            response.setMessage("Success");
            response.setData(mmResponse);
            response.setStatus(HttpStatus.OK);
        } else {
            response.setStatus(HttpStatus.EXPECTATION_FAILED);
            response.setData(mmResponse);
            transaction.setErrorDescription(mmResponse.getMessage());
            transaction.setTransactionErrorCode(mmResponse.getCode());
        }
        transactionRepository.save(transaction);

        return ResponseEntity.status(response.getStatus()).body(response);
    }

     /**
     * ------------------------------------------------------------------------------------------------------------------------------------------------
     * Other endpoints
     * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */
    
    @Operation(summary = "Get WSP exhange currency", description = "To retrieve exchange currency from WSP by country")
    @GetMapping(path = { "/exchange/currency/{country}" })
    public ResponseEntity<HttpResponse> exchangeCurrency(HttpServletRequest request,
            @PathVariable("country") String country) throws Exception {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        RetailRate retailRate = wspRequestService.requestRetailRate(country);
        if (retailRate != null) {
            response.setData(retailRate);
            response.setStatus(HttpStatus.OK);
        } else {
            response.setStatus(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Validate bill at WSP", description = "To validate bill at WSP")
    @PostMapping(path = { "/validate-bill" })
    public ResponseEntity<HttpResponse> validateBill(HttpServletRequest request,
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String method,
            @RequestParam(required = true) String productCode) throws Exception {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        ValidateBill validateBill = wspRequestService.requestValidateBill(productCode, accountNo, method);
        if (validateBill != null) {
            response.setData(validateBill);
            response.setStatus(HttpStatus.OK);
        } else {
            response.setStatus(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }


      /**
     * ------------------------------------------------------------------------------------------------------------------------------------------------
     * Private models 
     * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    @Getter
    @Setter
    public class MakePayment {

        String id;
        String transactionId;
        String status;
        String provider;
        String bank;
        String bankName;
        Double chargeAmount;
        Double totalAmount;
        Double amount;

        Long providerProductId;

        public String toString() {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(this);
        }

    }

    @Getter
    @Setter
    public static class CallbackResponse {

        String CustomerPaymentPageText;
        String OrderReference;
        String OrderDescription;
        String CurrencyText;
        Double Amount;
        String TransactionDate;
        String AuthorizationCode;
        String TransactionStatusText;
        String ResponseCode;
        String AvsResponse;
        String Cvv2Response;
        String ErrorCode;
        String ErrorMessage;
        String ProcessorMessage;
        String EntryType;

        Long providerProductId;

        public String toString() {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(this);
        }

    }

    @Getter
    @Setter
    public static class Payment {
        String userId;
        String email;
        String phoneNo;
        String name;
        Double transactionAmount;
        Double denoAmount;
        Integer productVariantId;
        String accountNo;
        String paymentMethod;
        String spOrderId;
        String spInvoiceId;
        TransactionEnum paymentEnum;
        String billPhoneNumber;
        String extra1;
        String extra2;
        String extra3;
        String extra4;
        String discountCode;
        Double fixFee;
    }
}
