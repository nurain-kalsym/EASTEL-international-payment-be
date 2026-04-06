package com.kalsym.ekedai.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.model.*;
import com.kalsym.ekedai.model.dao.*;
import com.kalsym.ekedai.model.dao.Order.OrderConfirm;
import com.kalsym.ekedai.model.dao.loyalty.OverallCoinsData;
import com.kalsym.ekedai.model.enums.DiscountUserStatus;
import com.kalsym.ekedai.model.enums.PaymentStatus;
import com.kalsym.ekedai.model.enums.TransactionEnum;
import com.kalsym.ekedai.model.enums.VariantType;
import com.kalsym.ekedai.repositories.*;
import com.kalsym.ekedai.services.*;
import com.kalsym.ekedai.utility.HttpResponse;
import com.kalsym.ekedai.utility.Logger;
import com.kalsym.ekedai.utility.StringUtility;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.kalsym.ekedai.filter.SessionRequestFilter.HEADER_STRING;

@RestController
@RequestMapping("/payment")
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
    DiscountRepository discountRepository;

    @Autowired
    DiscountUserService discountUserService;

    @Autowired
    SymplifiedOrderService symplifiedOrderService;

    @Autowired
    LoyaltyService loyaltyService;

    @Autowired
    DiscountController discountController;

    @Autowired
    CampaignService campaignService;

    @Autowired
    PaymentChannelRepository paymentChannelRepository;

    @Autowired
    ProductVariantPaymentChannelRepository productVariantPaymentChannelRepository;

    @Autowired
    ProductVariantRepository productVariantRepository;

    // @Autowired
    // private EmailService emailService;

    // @Autowired
    // private FraudCheckService fraudCheckService;

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

    @GetMapping(path = { "/available-coins/{phone}" })
    public ResponseEntity<HttpResponse> getAvailableCoins(HttpServletRequest request,
            @PathVariable("phone") String phone) throws Exception {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        Gson gson = new Gson();

        try {
            // Call the service to fetch available coins
            HttpResponse httpResponse = loyaltyService.getAvailableCoins(phone);
            OverallCoinsData overallCoinsData = loyaltyService.getDataAsOverallCoinsData(httpResponse.getData());

            response.setData(overallCoinsData);
            response.setStatus(HttpStatus.OK);
        } catch (HttpStatusCodeException e) {
            HttpResponse requestResponse = gson.fromJson(e.getResponseBodyAsString(),
                    HttpResponse.class);

            // Set the status and extract the message from the error response
            response.setStatus(e.getStatusCode());
            response.setMessage(requestResponse.getMessage());
        } catch (RestClientException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setMessage("An error occurred while processing the request.");
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping(path = { "/payment-channel/{id}" })
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

    @PostMapping(path = { "/validateBill" })
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

    @PostMapping(path = { "/transaction" })
    public ResponseEntity<HttpResponse> createTransaction(HttpServletRequest request,
            @Valid @RequestBody Payment payment) throws Exception {
        String logprefix = request.getRequestURI() + " createTransaction() ";
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String systemTransactionId = null;

        Double fixFee = payment.getFixFee();

        User user = userService.getUser(request.getHeader(HEADER_STRING));

        if (user == null) {
            response.setStatus(HttpStatus.NOT_FOUND);
            response.setMessage("User Not Found");
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "User Not Found");

            return ResponseEntity.status(response.getStatus()).body(response);
        }

        Transaction transaction = new Transaction();

        // Set default
        transaction.setDiscountAmount(0.00);
        transaction.setCoinsRedeemed(0.00);
        transaction.setIsFraud(false);

        ProductVariant productVariant = null;
        if (payment.getProductVariantId() != null) {
            productVariant = productVariantDb.findById(payment.getProductVariantId()).orElse(null);
            // Check if the exchange rate is found
            if (productVariant == null) {
                response.setStatus(HttpStatus.NOT_FOUND, "Product Variant Not Found");

                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        transaction.getTransactionId(), "Product Variant Not Found : ", transaction);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            Product product = productRepository.findById(productVariant.getProductId()).orElse(null);
            if (product == null) {
                response.setStatus(HttpStatus.NOT_FOUND, "Product Not Found");

                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        transaction.getTransactionId(), "Product Not Found : ", transaction);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            transaction.setProduct(product);
            transaction.setProductVariantId(productVariant.getId());
            Gson gson = new Gson();

            if (productVariant.getVariantType().equals(VariantType.BILLPAYMENT)) {

                // Check retail rate first before save into for bill payment only
                Country country = countryRepository.findById(product.getCountryCode()).get();

                if (country == null) {

                    response.setStatus(HttpStatus.NOT_FOUND, "Country Not Found");

                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            transaction.getTransactionId(), "Country Not Found : ", transaction);

                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }

                RetailRate retailRate = wspRequestService.requestRetailRate(country.getWspCountryCode());

                // Check if the exchange rate is found
                if (retailRate == null) {
                    response.setStatus(HttpStatus.NOT_FOUND, "Retail Rate Not Found");

                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            transaction.getTransactionId(), "Retail Rate Not Found : ", transaction);

                }

                transaction.setDenoAmount(payment.getDenoAmount());

                double valueAfterRate = (payment.getDenoAmount() / retailRate.getRate());

                // Round the value to two decimal places
                double roundedValue = (Math.round(valueAfterRate * 100.0) / 100.0) + fixFee;

                // Create a DecimalFormat object with two decimal places
                DecimalFormat df = new DecimalFormat("0.00");
                df.setRoundingMode(RoundingMode.HALF_UP);

                // Format the double as a two-decimal-point Double
                String formattedValue = df.format(roundedValue);

                // Convert the formatted string back to a Double
                Double transactionAmount = Double.parseDouble(formattedValue);

                if (payment.getDiscountCode() != null && !payment.getDiscountCode().isEmpty()) {
                    // Calculate discount amount
                    Optional<Discount> optionalDiscount = discountRepository
                            .findByDiscountCode(payment.getDiscountCode());

                    if (optionalDiscount.isPresent()) {
                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                "Discount code found : " + payment.getDiscountCode());

                        Double originalAmount = transactionAmount;
                        // Overwrite with new amount
                        transactionAmount = paymentService.calculateTransactionDiscount(optionalDiscount.get(),
                                originalAmount, payment.getPhoneNo());

                        transaction.setDiscountAmount(originalAmount - transactionAmount);
                        transaction.setDiscountId(optionalDiscount.get().getId());
                    }
                }

                // Redeem coins - get available coins for BILLPAYMENT
                if (payment.getRedeemCoins()) {

                    try {
                        // Call the service to fetch available coins
                        HttpResponse httpResponse = loyaltyService.getAvailableCoins(payment.getPhoneNo());
                        OverallCoinsData overallCoinsData = loyaltyService
                                .getDataAsOverallCoinsData(httpResponse.getData());

                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                "Referral coins available: " + overallCoinsData.getAvailableCoins());

                        // Determine the amount of coins to redeem without making transactionAmount
                        // negative
                        if (transactionAmount < overallCoinsData.getAvailableCash()) {
                            // If transactionAmount is less than available coins, redeem only the
                            // transactionAmount
                            transaction.setCoinsRedeemed(transactionAmount);
                            transactionAmount = 0.0; // Set transactionAmount to 0 as the entire amount is redeemed
                        } else {
                            // Otherwise, redeem the full available cash amount
                            transactionAmount = transactionAmount - overallCoinsData.getAvailableCash();
                            transaction.setCoinsRedeemed(overallCoinsData.getAvailableCash());
                        }
                    } catch (HttpStatusCodeException e) {

                        HttpResponse requestResponse = gson.fromJson(e.getResponseBodyAsString(), HttpResponse.class);

                        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                "Error parsing available coins: " + e.getMessage());

                        Map<String, String> errorDetails = new HashMap<>();
                        errorDetails.put("error", "An error occurred while parsing available coins");
                        errorDetails.put("message", requestResponse.getMessage());
                        response.setData(errorDetails);
                        response.setStatus(e.getStatusCode());
                        return ResponseEntity.status(response.getStatus()).body(response);
                    } catch (RestClientException e) {
                        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                        response.setMessage("An error occurred while processing the request.");
                        return ResponseEntity.status(response.getStatus()).body(response);
                    }
                }

                transaction.setTransactionAmount(transactionAmount);

            } else {
                transaction.setDenoAmount(productVariant.getDeno());
                Double transactionAmount = productVariant.getPrice() + fixFee;

                if (payment.getDiscountCode() != null && !payment.getDiscountCode().isEmpty()) {
                    // Calculate discount amount
                    Optional<Discount> optionalDiscount = discountRepository
                            .findByDiscountCode(payment.getDiscountCode());

                    if (optionalDiscount.isPresent()) {
                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                "Discount code found : " + payment.getDiscountCode());

                        Double originalAmount = transactionAmount;
                        // Overwrite with new amount
                        transactionAmount = paymentService.calculateTransactionDiscount(optionalDiscount.get(),
                                originalAmount, payment.getPhoneNo());

                        transaction.setDiscountAmount(originalAmount - transactionAmount);
                        transaction.setDiscountId(optionalDiscount.get().getId());
                    }
                }

                // Redeem coins - get available coins for other than BILLPAYMENT
                if (payment.getRedeemCoins()) {

                    try {
                        // Call the service to fetch available coins
                        HttpResponse httpResponse = loyaltyService.getAvailableCoins(payment.getPhoneNo());
                        OverallCoinsData overallCoinsData = loyaltyService
                                .getDataAsOverallCoinsData(httpResponse.getData());

                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                "Referral coins available: " + overallCoinsData.getAvailableCoins());

                        // Determine the amount of coins to redeem without making transactionAmount
                        // negative
                        if (transactionAmount < overallCoinsData.getAvailableCash()) {
                            // If transactionAmount is less than available coins, redeem only the
                            // transactionAmount
                            transaction.setCoinsRedeemed(transactionAmount);
                            transactionAmount = 0.0; // Set transactionAmount to 0 as the entire amount is redeemed
                        } else {
                            // Otherwise, redeem the full available cash amount
                            transactionAmount = transactionAmount - overallCoinsData.getAvailableCash();
                            transaction.setCoinsRedeemed(overallCoinsData.getAvailableCash());
                        }
                    } catch (HttpStatusCodeException e) {

                        HttpResponse requestResponse = gson.fromJson(e.getResponseBodyAsString(), HttpResponse.class);

                        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                "Error parsing available coins: " + e.getMessage());

                        Map<String, String> errorDetails = new HashMap<>();
                        errorDetails.put("error", "An error occurred while parsing available coins");
                        errorDetails.put("message", requestResponse.getMessage());
                        response.setData(errorDetails);
                        response.setStatus(e.getStatusCode());
                        return ResponseEntity.status(response.getStatus()).body(response);
                    } catch (RestClientException e) {
                        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                        response.setMessage("An error occurred while processing the request.");
                        return ResponseEntity.status(response.getStatus()).body(response);
                    }
                }

                transaction.setTransactionAmount(transactionAmount);
            }
        }

        transaction.setPaymentMethod(payment.getPaymentMethod());
        transaction.setAccountNo(payment.getAccountNo());
        transaction.setUserId(user.getId());
        transaction.setEmail(user.getEmail());
        transaction.setName(user.getFullName());
        transaction.setPhoneNo(user.getPhoneNumber());
        transaction.setTransactionType(payment.getPaymentEnum());
        transaction.setStatus("PENDING");
        transaction.setPaymentStatus(PaymentStatus.PENDING);
        transaction.setCreatedDate(new Date());
        transaction.setBillPhoneNumber(payment.getBillPhoneNumber());
        transaction.setExtra1(payment.getExtra1());
        transaction.setExtra2(payment.getExtra2());
        transaction.setExtra3(payment.getExtra3());
        transaction.setExtra4(payment.getExtra4());

        // Overwrite certain values
        if (payment.getSpOrderId() != null) {
            systemTransactionId = StringUtility.CreateRefID("EKP");
            transaction.setTransactionType(payment.getPaymentEnum()); // ORDER or COUPON
            transaction.setSpOrderId(payment.getSpOrderId());
            transaction.setSpInvoiceId(payment.getSpInvoiceId());
            transaction.setTransactionAmount(payment.transactionAmount);

            // Overwrite status and payment status if FREECOUPON
            if (productVariant != null && productVariant.getVariantType().equals(VariantType.FREECOUPON)) {
                transaction.setStatus("PAID");
                transaction.setPaymentStatus(PaymentStatus.PAID);
            }
        } else {
            systemTransactionId = StringUtility.CreateRefID("EKD");
        }
        transaction.setTransactionId(systemTransactionId);

        Transaction savedTransaction = transactionRepository.save(transaction);
        response.setData(savedTransaction);

        // try {
        //     fraudCheckService.asyncCheckFraud1st(request, savedTransaction, logprefix);
        // } catch (Exception e) {
        //     // It should not trigger exception because it is an async method
        // }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping(path = { "/get/transaction/{transactionId}" })
    public ResponseEntity<HttpResponse> getByTransactionId(
            HttpServletRequest request,
            @PathVariable(name = "transactionId") String transactionId) throws Exception {
        // String logprefix = request.getRequestURI() + " getByTransactionId() ";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        Optional<Transaction> transaction = transactionRepository.findByTransactionId(transactionId);
        if (transaction.isPresent()) {
            response.setData(transaction.get());
            response.setStatus(HttpStatus.OK);
        } else {
            response.setStatus(HttpStatus.NOT_FOUND);
            response.setMessage("Transaction not found");
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping(path = { "/transaction/history" })
    public ResponseEntity<HttpResponse> getTransactionHistory(HttpServletRequest request,
            @RequestParam(defaultValue = "ASC", required = false) String sortingOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdDate", required = false) String sortBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to,
            @RequestParam(required = false) VariantType variantType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) String globalSearch,
            @RequestParam(required = false) Boolean withRefund) throws Exception {
        String logprefix = request.getRequestURI() + " get transaction history ";

        HttpResponse response = new HttpResponse(request.getRequestURI());

        User user = userService.getUser(request.getHeader(HEADER_STRING));

        if (user == null) {
            response.setStatus(HttpStatus.NOT_FOUND);
            response.setMessage("User Not Found");
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "User Not Found");

            return ResponseEntity.status(response.getStatus()).body(response);
        }

        Transaction transaction = new Transaction();
        transaction.setUserId(user.getId());

        ExampleMatcher matcher = ExampleMatcher
                .matchingAll()
                .withIgnoreCase()
                .withMatcher("userId", new ExampleMatcher.GenericPropertyMatcher().exact())
                // .withMatcher("status", new ExampleMatcher.GenericPropertyMatcher().exact())
                .withIgnoreNullValues()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
        Example<Transaction> example = Example.of(transaction, matcher);

        Pageable pageable = null;
        if (sortingOrder.equalsIgnoreCase("desc")) {
            pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).descending());
        } else {
            pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).ascending());
        }
        Page<Transaction> transactions = transactionRepository
                .findAll(
                        getTransactionHistoryByUser(from, to, example, user.getId(), variantType, status, paymentStatus,
                                globalSearch, withRefund),
                        pageable);
        List<Transaction> tempResultList = transactions.getContent();

        tempResultList = tempResultList.stream()
                .map(x -> {
                    if (x.getTransactionType() != null && !x.getTransactionType().equals(TransactionEnum.ORDER)
                            && x.getProduct() != null) {
                        x.getProduct().setProductVariant(null);
                    }
                    return x;
                })
                .collect(Collectors.toList());
        response.setData(transactions);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @GetMapping(path = { "/transaction/symplified-history" })
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
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
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
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Exception");
                response.setStatus(HttpStatus.OK);
                response.setData(Collections.emptyList());
                return ResponseEntity.status(response.getStatus()).body(response);
            }
        }
        response.setData(transactions);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PostMapping(path = { "/callback" })
    public ResponseEntity<HttpResponse> callback(HttpServletRequest request, @RequestHeader Map<String, String> headers,
            @RequestBody MultiValueMap<String, String> formData) throws Exception {

        Date now = new Date();

        String host = request.getHeader("Host");

        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION,
                "Incoming request Host Callback: " + host);

        String logprefix = request.getRequestURI();

        HttpResponse response = new HttpResponse(request.getRequestURI());

        headers.forEach((key, value) -> {
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Headers: " + (String.format("Header '%s' = %s", key, value)));

        });

        Set<String> keys = formData.keySet();

        for (String key : keys) {
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix, "Key = " + key);
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Values = " + formData.get(key));
        }

        try {

            String requestURL = request.getRequestURL() != null ? request.getRequestURL().toString() : "Unknown";
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Request URL: " + requestURL);

            String transactionId = extractParam(formData, "OrderDescription");
            String errorCode = extractParam(formData, "ResponseCode");
            String refId = extractParam(formData, "OrderReference");
            String errorMessage = extractParam(formData, "TransactionStatusText");
            String transactionType = extractParam(formData, "TransactionTypeText");
            String transactionDate = extractParam(formData, "TransactionDate");
            String providerError = extractParam(formData, "ProcessorMessage");

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix, "Body: " + formData);
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
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix, "Error sending message : ",
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

        // boolean isFraudCheckPassed = false;
        // try {
        //     // isFraudCheckPassed = fraudCheckService.checkFraud2nd(request, t, errorCode, logprefix);
        // } catch (Exception e) {
        //     t.setErrorDescription("POTENTIAL FRAUD - 2nd Check: Exception occurred");
        //     t.setIsFraud(true);
        //     Transaction savedTransaction = transactionRepository.save(t);
        //     try {
        //         // Send fraud alert email
        //         emailService.sendFraudAlert(savedTransaction);
        //         Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
        //                 "Fraud alert email sent successfully for transaction: "
        //                         + savedTransaction.getTransactionId());
        //     } catch (Exception x) {
        //         Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
        //                 "Failed to send fraud alert email: " + x.getMessage());
        //     }
        // }

        // if (!isFraudCheckPassed) {
        //     Logger.application.warn(Logger.pattern, EkedaiApplication.VERSION, logprefix,
        //             t.getTransactionId(), "Fraud check failed, skipping WSP/Symplified call.");
        //     return;
        // }

        // 000 is PAID
        if ("000".equals(errorCode)) {
            Optional<ProductVariant> variantOpt = productVariantDb.findById(t.getProductVariantId());
            if (!variantOpt.isPresent()) {
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
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

                // Update discount code status
                if (t.getDiscount() != null) {
                    try {
                        UserUpdateRequest userUpdateRequest = new UserUpdateRequest();
                        userUpdateRequest.setUserPhoneNumber(t.getPhoneNo());
                        userUpdateRequest.setDiscountCode(t.getDiscount().getDiscountCode());
                        userUpdateRequest.setStatus(DiscountUserStatus.REDEEMED);
                        discountUserService.updateStatus(userUpdateRequest);
                    } catch (Exception ex) {
                        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                t.getDiscount().getDiscountCode(), "Error update discount code status ",
                                ex.getMessage());
                    }
                }

                try {
                    MtradePaymentResponse responseData = wspRequestService
                            .requestPaymentType(mtradePaymentRequest);

                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            "WSP Request : " + responseData);

                    t.setWspTransactionId(responseData.getSystemTransactionId());
                    t.setTransactionErrorCode(responseData.getResponseCode());
                    t.setErrorDescription(responseData.getResponseDescription());

                    String responseCode = responseData.getResponseCode();
                    if ("000".equals(responseCode)) {
                        t.setStatus("PAID");

                        // Handle active campaign and issue reward after successful wsp request
                        handleActiveCampaignAndReward(t, logprefix, request);

                        // Handle loyalty and referral after successful wsp request
                        loyaltyService.handleLoyaltyAndReferralCoins(t, logprefix, productVariant);

                    } else if ("001".equals(responseCode) || "203".equals(responseCode)
                            || "204".equals(responseCode)) {
                        t.setStatus("PROCESSING");
                    } else {
                        t.setStatus("FAILED");
                    }

                    Transaction savedTransaction = transactionRepository.save(t);

                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            savedTransaction.getTransactionId(), "Request Payment Confirmation Request::",
                            savedTransaction);

                } catch (Exception ex) {
                    t.setStatus("FAILED");

                    if (ex.getMessage() == null || ex.getMessage().isEmpty())
                        t.setErrorDescription("eByzarr System Failure");
                    else
                        t.setErrorDescription(ex.getMessage());

                    Transaction saveTransaction = transactionRepository.save(t);
                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            t.getTransactionId(), "Saved with exception : ", saveTransaction);
                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            t.getTransactionId(), "Update WSP Exception ", ex.getMessage());

                }
            } else {
                // CALL SYMPLIFIED BACKEND
                try {
                    OrderConfirm res = paymentService.groupOrderUpdateStatus(t.getTransactionId(),
                            t.getSpOrderId(), "PAYMENT_CONFIRMED", "", errorMessage, "OZO-" + transactionType);

                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            t.getTransactionId(), "res::", res);

                    t.setStatus("PAID");

                    // Handle active campaign and issue reward in Symplified section
                    handleActiveCampaignAndReward(t, logprefix, request);

                    // Handle loyalty and referral in Symplified section
                    loyaltyService.handleLoyaltyAndReferralCoins(t, logprefix, productVariant);

                    Transaction saveTransaction = transactionRepository.save(t);

                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "Request Payment Confirmation Request::",
                            saveTransaction);

                } catch (Exception ex) {
                    t.setStatus("PAID");
                    t.setErrorDescription("Failed to Update Order Services");

                    Transaction saveTransaction = transactionRepository.save(t);

                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "Exception ::" + ex.getMessage());

                }
            }
        } else if ("003".equals(errorCode)) {
            t.setStatus("PENDING");
            t.setErrorDescription("PAYMENT PENDING");
            Transaction saveTransaction = transactionRepository.save(t);

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    saveTransaction.getTransactionId(), "Request Payment Confirmation Request");

            if (t.getTransactionType().equals(TransactionEnum.ORDER)
                    || t.getTransactionType().equals(TransactionEnum.COUPON)) {
                // CALL SYMPLIFIED BACKEND
                try {
                    OrderConfirm res = paymentService.groupOrderUpdateStatus(t.getTransactionId(),
                            t.getSpOrderId(), "PAYMENT_FAILED", "", errorMessage, "OZO-" + transactionType);
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "res::", res);
                } catch (Exception ex) {
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
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
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "res::", res);
                } catch (Exception ex) {
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "Exception ::" + ex.getMessage());
                }
            }

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    saveTransaction.getTransactionId(), "Payment Confirmation Callback, Payment Failed: ",
                    saveTransaction);
        }
    }

    @PostMapping(path = { "/ozopaymanualcallback/{transactionId}" })
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

                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
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

                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
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
                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            t.getTransactionId(), "Saved with exception : ", saveTransaction);
                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            t.getTransactionId(), "Update WSP Exception ", ex.getMessage());
                    response.setData(saveTransaction);
                    return ResponseEntity.status(HttpStatus.OK).body(response);
                }
            } else {
                // CALL SYMPLIFIED BACKEND
                try {
                    OrderConfirm res = paymentService.groupOrderUpdateStatus(t.getTransactionId(),
                            t.getSpOrderId(), "PAYMENT_CONFIRMED", "", errorMessage, "OZO-" + transactionType);

                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            t.getTransactionId(), "res::", res);

                    t.setStatus("PAID");

                    Transaction saveTransaction = transactionRepository.save(t);
                    response.setData(saveTransaction);

                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "Request Payment Confirmation Request::",
                            saveTransaction);

                } catch (Exception ex) {
                    t.setStatus("PAID");
                    t.setErrorDescription("Failed to Update Order Services");

                    Transaction saveTransaction = transactionRepository.save(t);

                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
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

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    saveTransaction.getTransactionId(), "Request Payment Confirmation Request");
            response.setData(saveTransaction);

            if (t.getTransactionType().equals(TransactionEnum.ORDER)
                    || t.getTransactionType().equals(TransactionEnum.COUPON)) {
                // CALL SYMPLIFIED BACKEND
                try {
                    OrderConfirm res = paymentService.groupOrderUpdateStatus(t.getTransactionId(),
                            t.getSpOrderId(), "PAYMENT_FAILED", "", errorMessage, "OZO-" + transactionType);
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "res::", res);
                } catch (Exception ex) {
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            saveTransaction.getTransactionId(), "Exception ::" + ex.getMessage());
                }
            }
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } else {
            t.setStatus("FAILED");
            t.setPaymentStatus(PaymentStatus.FAILED);
            t.setErrorDescription("PAYMENT FAILED");

            Transaction saveTransaction = transactionRepository.save(t);

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    saveTransaction.getTransactionId(), "Payment Confirmation Callback, Payment Failed: ",
                    saveTransaction);

            response.setData(saveTransaction);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

    }

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

    @PostMapping(path = { "/validateOtp/{transactionId}" })
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

        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix, "TransactionId", transaction);

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

                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
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

                        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                saveTransaction.getTransactionId(), "Saved with exception : ", saveTransaction);

                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
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

                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                saveTransaction.getTransactionId(), "Request Payment Confirmation Request::",
                                saveTransaction);

                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                transaction.getTransactionId(), "groupOrderUpdateStatus reponse ::" + res);
                    } catch (Exception ex) {

                        transaction.setStatus("PAID");
                        transaction.setErrorDescription("Failed to Update Order Services");

                        Transaction saveTransaction = transactionRepository.save(transaction);

                        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
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
                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                transaction.getTransactionId(), "groupOrderUpdateStatus response ::" + res);
                    } catch (Exception ex) {
                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                transaction.getTransactionId(), "Exception ::" + ex.getMessage());
                    }
                }
            }
        } else {
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    transaction.getTransactionId(), "Transaction Payment Confirmation Request, ",
                    "This transaction already " + transaction.getPaymentStatus().toString());
            response.setData(transaction);
            response.setMessage("This transaction already " + transaction.getPaymentStatus().toString());
            response.setStatus(HttpStatus.EXPECTATION_FAILED);
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping(path = { "/resendOtp/{transactionId}" })
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

        // Handle loyalty and referral for test callback
        loyaltyService.handleLoyaltyAndReferralCoins(transaction, logprefix, productVariant);

        // Handle active campaign and issue reward for test callback
        handleActiveCampaignAndReward(transaction, logprefix, request);

        if (transaction.getTransactionType().equals(TransactionEnum.ORDER)
                || transaction.getTransactionType().equals(TransactionEnum.COUPON)) {
            // Symplified order service
            OrderConfirm res = paymentService.groupOrderUpdateStatus(transaction.getTransactionId(),
                    transaction.getSpOrderId(), "PAYMENT_CONFIRMED", "", "UAT transaction", paymentChannel);

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, "callbackTest()",
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

            // Update discount code status
            if (transaction.getDiscount() != null) {
                try {
                    UserUpdateRequest userUpdateRequest = new UserUpdateRequest();
                    userUpdateRequest.setUserPhoneNumber(transaction.getPhoneNo());
                    userUpdateRequest.setDiscountCode(transaction.getDiscount().getDiscountCode());
                    userUpdateRequest.setStatus(DiscountUserStatus.REDEEMED);
                    discountUserService.updateStatus(userUpdateRequest);
                } catch (Exception ex) {
                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            transaction.getDiscount().getDiscountCode(), "Error update discount code status ",
                            ex.getMessage());
                }
            }

            try {
                MtradePaymentResponse responseData = wspRequestService
                        .requestPaymentType(mtradePaymentRequest);

                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
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

                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
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
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        transaction.getTransactionId(), "Saved with exception : ", saveTransaction);
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
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

    @GetMapping("/get-service-id")
    public ResponseEntity<HttpResponse> getServiceId(HttpServletRequest request,
            @RequestParam Integer productId) {
        HttpResponse response = new HttpResponse(request.getRequestURI());

        Optional<Product> optionalProduct = productRepository.findById(productId);

        if (optionalProduct.isPresent()) {

            Product product = optionalProduct.get();
            ServiceId serviceId = loyaltyService.getServiceIdValues(product.getServiceId());
            response.setData(serviceId);
            response.setStatus(HttpStatus.OK);

        } else {
            response.setStatus(HttpStatus.NOT_FOUND);

        }
        return ResponseEntity.status(response.getStatus()).body(response);

    }

    public static Specification<Transaction> getTransactionHistoryByUser(
            Date from, Date to, Example<Transaction> example, String userId, VariantType variantType, String status,
            PaymentStatus paymentStatus, String globalSearch, Boolean withRefund) {
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

            if (variantType != null) {
                Join<Transaction, ProductVariant> productVariant = root.join("productVariant");
                predicates.add(builder.equal(productVariant.get("variantType"), variantType));
            }

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            if (paymentStatus != null) {
                if (withRefund) {
                    predicates.add(
                            builder.or(
                                    builder.equal(root.get("paymentStatus"), PaymentStatus.REFUNDED),
                                    builder.equal(root.get("paymentStatus"), paymentStatus)));
                } else
                    predicates.add(builder.equal(root.get("paymentStatus"), paymentStatus));
            }

            if (globalSearch != null) {
                Join<Transaction, Product> product = root.join("product");
                // Predicate for Employee Projects data
                predicates.add(builder.or(
                        builder.like(product.get("productName"), "%" + globalSearch + "%"),
                        builder.like(root.get("accountNo"), "%" + globalSearch + "%")));
            }

            // Exclude records where both status and paymentStatus are PENDING
            predicates.add(builder.not(
                    builder.and(
                            builder.equal(root.get("status"), "PENDING"),
                            builder.equal(root.get("paymentStatus"), PaymentStatus.PENDING))));

            predicates.add(QueryByExamplePredicateBuilder.getPredicate(root, builder, example));

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Specification<Transaction> getTransactionVouchersByUser(
            Date from, Date to, Example<Transaction> example, String userId, String search, String variantCategory) {
        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();
            Join<Transaction, ProductVariant> productVariant = root.join("productVariant");
            Join<Transaction, Product> product = root.join("product");

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

            // Filter variant type by voucher
            predicates.add(builder.equal(productVariant.get("variantType"), VariantType.VOUCHER));

            if (variantCategory != null && !"PREPAID".equals(variantCategory) && !"GAMES".equals(variantCategory)) {
                predicates.add(builder.equal(productVariant.get("category"), variantCategory));
            }

            if (variantCategory != null && ("PREPAID".equals(variantCategory) || "GAMES".equals(variantCategory))) {
                predicates.add(builder.equal(product.get("productType"), variantCategory));
            }

            // Filter status by paid
            predicates.add(builder.equal(root.get("status"), "PAID"));
            predicates.add(builder.equal(root.get("paymentStatus"), PaymentStatus.PAID));

            if (search != null) {
                predicates.add(builder.or(
                        builder.like(product.get("productName"), "%" + search + "%"),
                        builder.like(productVariant.get("variantName"), "%" + search + "%")));
            }

            predicates.add(QueryByExamplePredicateBuilder.getPredicate(root, builder, example));

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

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

    @GetMapping("/{transactionId}/status")
    public ResponseEntity<HttpResponse> getStatusByTransactionId(HttpServletRequest request,
            @PathVariable String transactionId) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = request.getRequestURI() + " getStatusByTransactionId() ";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix, "transactionId: ",
                transactionId);
        try {
            Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
            Transaction transaction = transactionOpt.get();
            if (transaction != null) {
                response.setStatus(HttpStatus.OK);
                response.setData(transaction.getStatus());
            } else {
                response.setStatus(HttpStatus.NOT_FOUND);
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix, "transactionId: ",
                        transactionId,
                        "NOT_FOUND");
            }
        } catch (Exception e) {
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix, "Exception: ",
                    e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<HttpResponse> verifyTransaction(HttpServletRequest request,
            @RequestParam String transactionId, @RequestParam boolean isFraud) {
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

                    if (isFraud) {
                        if (transaction.getPaymentStatus().equals(PaymentStatus.PAID)) {
                            // Process refund for fraudulent transaction
                            transaction.setPaymentStatus(PaymentStatus.PENDING_REFUND);
                            transaction = transactionRepository.save(transaction);
                        }
                        response.setStatus(HttpStatus.OK);
                        response.setData(transaction);
                    } else {
                        // Set isFraud to false
                        transaction.setIsFraud(false);

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
                            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                    "WSP Request : " + responseData);

                            transaction.setWspTransactionId(responseData.getSystemTransactionId());
                            transaction.setTransactionErrorCode(responseData.getResponseCode());
                            transaction.setErrorDescription(responseData.getResponseDescription());

                            String responseCode = responseData.getResponseCode();
                            if ("000".equals(responseCode)) {
                                transaction.setStatus("PAID");

                                // Handle active campaign and issue reward after successful wsp request
                                handleActiveCampaignAndReward(transaction, logPrefix, request);

                                // Handle loyalty and referral after successful wsp request
                                loyaltyService.handleLoyaltyAndReferralCoins(transaction, logPrefix, productVariant);

                            } else if ("001".equals(responseCode) || "203".equals(responseCode)
                                    || "204".equals(responseCode)) {
                                transaction.setStatus("PROCESSING");
                            } else {
                                transaction.setStatus("FAILED");
                            }

                            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                    transaction.getTransactionId(), " Request Payment Confirmation Request::");

                        } catch (Exception e) {
                            transaction.setStatus("FAILED");

                            if (e.getMessage() == null || e.getMessage().isEmpty())
                                transaction.setErrorDescription("eByzarr System Failure");
                            else
                                transaction.setErrorDescription(e.getMessage());

                            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                    transaction.getTransactionId(), "Saved with exception : ", transaction);
                            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                    transaction.getTransactionId(), "Update WSP Exception ", e.getMessage());
                        }
                        Transaction savedTransaction = transactionRepository.save(transaction);
                        response.setStatus(HttpStatus.OK);
                        response.setData(savedTransaction);

                    }
                } else {
                    response.setStatus(HttpStatus.NOT_FOUND,
                            "Product Variant not found: " + transaction.getProductVariantId());
                }

            } else {
                response.setStatus(HttpStatus.NOT_FOUND, "Transaction not found: " + transactionId);
            }

        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix, "Exception: ",
                    e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    private void handleActiveCampaignAndReward(Transaction transaction, String logPrefix, HttpServletRequest request) {

        // Get user
        Optional<User> userOpt = userService.optionalUserById(transaction.getUserId());

        // Check for campaign
        Optional<Campaign> activeCampaign = campaignService.findAndValidateTransactionCriteria(transaction,
                userOpt.orElse(null), logPrefix);
        if (activeCampaign.isPresent()) {

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                    "Campaign criteria met for transaction ID: " + transaction.getTransactionId());

            // Handle the transaction as it meets the campaign criteria
            Campaign campaign = activeCampaign.get();
            // Issue reward
            switch (campaign.getRewardType()) {
                case DISCOUNT_CODE:
                    // Give discount code if applicable
                    DiscountUserRequest discountUserRequest = new DiscountUserRequest();
                    discountUserRequest.setDiscountCode(campaign.getRewardValue());
                    discountUserRequest.setUserPhoneNumber(transaction.getPhoneNo());

                    // Claim discount process
                    HttpResponse claimDiscountResponse = discountUserService.claimDiscount(request, discountUserRequest,
                            logPrefix, null);

                    if (claimDiscountResponse.getStatus() == 200) {
                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                "Reward " + campaign.getRewardValue() + " issued to " + transaction.getPhoneNo());
                    } else {
                        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                "Error in issuing reward to " + transaction.getPhoneNo() + ": "
                                        + claimDiscountResponse.getMessage());
                    }
                    break;

                case EXTERNAL_VOUCHER_CODE:
                    // Placeholder: Implement the process for issuing an external voucher code if
                    // applicable
                    break;
                default:
                    // Log if an unsupported value is found in the campaign rewardType
                    Logger.application.warn(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "Unsupported campaign rewardType: " + campaign.getRewardType());
                    break;
            }
        } else {
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                    "No active campaign");
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
        Boolean redeemCoins;
        Double fixFee;
    }
}
