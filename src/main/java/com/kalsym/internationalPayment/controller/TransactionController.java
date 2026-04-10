package com.kalsym.internationalPayment.controller;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.controller.PaymentController.Payment;
import com.kalsym.internationalPayment.model.Country;
import com.kalsym.internationalPayment.model.Product;
import com.kalsym.internationalPayment.model.ProductVariant;
import com.kalsym.internationalPayment.model.Transaction;
import com.kalsym.internationalPayment.model.User;
import com.kalsym.internationalPayment.model.dao.RetailRate;
import com.kalsym.internationalPayment.model.enums.PaymentStatus;
import com.kalsym.internationalPayment.model.enums.TransactionEnum;
import com.kalsym.internationalPayment.model.enums.VariantType;
import com.kalsym.internationalPayment.repositories.CountryRepository;
import com.kalsym.internationalPayment.repositories.ProductRepository;
import com.kalsym.internationalPayment.repositories.ProductVariantRepository;
import com.kalsym.internationalPayment.repositories.TransactionRepository;
import com.kalsym.internationalPayment.services.PdfService;
import com.kalsym.internationalPayment.services.UserService;
import com.kalsym.internationalPayment.services.WSPRequestService;
import com.kalsym.internationalPayment.utility.HttpResponse;
import com.kalsym.internationalPayment.utility.Logger;
import com.kalsym.internationalPayment.utility.StringUtility;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import static com.kalsym.internationalPayment.filter.SessionRequestFilter.HEADER_STRING;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    UserService userService;
    
    @Autowired
    ProductVariantRepository productVariantDb;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CountryRepository countryRepository;

    @Autowired
    WSPRequestService wspRequestService;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    PdfService pdfService;

    @Operation(summary = "Create transaction", description = "To create new transaction")
    @PostMapping(path = { "/create" })
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
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "User Not Found");

            return ResponseEntity.status(response.getStatus()).body(response);
        }

        Transaction transaction = new Transaction();

        ProductVariant productVariant = null;
        if (payment.getProductVariantId() != null) {
            productVariant = productVariantDb.findById(payment.getProductVariantId()).orElse(null);
            // Check if the exchange rate is found
            if (productVariant == null) {
                response.setStatus(HttpStatus.NOT_FOUND, "Product Variant Not Found");

                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        transaction.getTransactionId(), "Product Variant Not Found : ", transaction);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            Product product = productRepository.findById(productVariant.getProductId()).orElse(null);
            if (product == null) {
                response.setStatus(HttpStatus.NOT_FOUND, "Product Not Found");

                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        transaction.getTransactionId(), "Product Not Found : ", transaction);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            transaction.setProduct(product);
            transaction.setProductVariantId(productVariant.getId());

            if (productVariant.getVariantType().equals(VariantType.BILLPAYMENT)) {
                // Check retail rate first before save into for bill payment only
                Country country = countryRepository.findById(product.getCountryCode()).get();

                if (country == null) {

                    response.setStatus(HttpStatus.NOT_FOUND, "Country Not Found");

                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            transaction.getTransactionId(), "Country Not Found : ", transaction);

                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }

                RetailRate retailRate = wspRequestService.requestRetailRate(country.getWspCountryCode());

                // Check if the exchange rate is found
                if (retailRate == null) {
                    response.setStatus(HttpStatus.NOT_FOUND, "Retail Rate Not Found");

                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
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

                transaction.setTransactionAmount(transactionAmount);

            } else {
                transaction.setDenoAmount(productVariant.getDeno());
                Double transactionAmount = productVariant.getPrice() + fixFee;

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
            systemTransactionId = StringUtility.CreateRefID("EIP");
        }
        transaction.setTransactionId(systemTransactionId);

        Transaction savedTransaction = transactionRepository.save(transaction);
        response.setData(savedTransaction);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Get transaction by ID", description = "To retrieve transaction by ID")
    @GetMapping(path = { "/{transactionId}" })
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

    @Operation(summary = "Get transactions for currently logged in user", description = "Get transaction history by pagination/filter.")
    @GetMapping(path = { "/history/pagination" })
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

        Pageable pageable = null;
        if (sortingOrder.equalsIgnoreCase("desc")) {
            pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).descending());
        } else {
            pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).ascending());
        }
        Page<Transaction> transactions = transactionRepository
                .findAll(
                        getTransactionHistoryByUser(from, to, example, variantType, status, paymentStatus,
                                globalSearch, withRefund),
                        pageable);
        List<Transaction> tempResultList = transactions.getContent();

        System.out.println("============" +tempResultList.size());

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
    
    public static Specification<Transaction> getTransactionHistoryByUser(
            Date from, Date to, Example<Transaction> example, VariantType variantType, String status,
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

    @Operation(summary = "Get transaction status by ID", description = "To retrieve transaction status by ID")
    @GetMapping("/{transactionId}/status")
    public ResponseEntity<HttpResponse> getStatusByTransactionId(HttpServletRequest request,
            @PathVariable String transactionId) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = request.getRequestURI() + " getStatusByTransactionId() ";
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "transactionId: ",
                transactionId);
        try {
            Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
            Transaction transaction = transactionOpt.get();
            if (transaction != null) {
                response.setStatus(HttpStatus.OK);
                response.setData(transaction.getStatus());
            } else {
                response.setStatus(HttpStatus.NOT_FOUND);
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "transactionId: ",
                        transactionId,
                        "NOT_FOUND");
            }
        } catch (Exception e) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Exception: ",
                    e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "", description = "")
    @GetMapping("/receipt/download/pdf/{transactionId}")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable String transactionId) throws IOException {

        byte[] pdf = null;
        Optional<Transaction> optTrans = transactionRepository.findByTransactionId(transactionId);
        if(optTrans.isPresent()){
            Transaction tx = optTrans.get();
            pdf = pdfService.generateReceipt(tx);
             System.out.println("found");
        } else {
            //testing
            pdf = pdfService.generateReceiptTest();
             System.out.println("not found");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "", description = "")
    @GetMapping("/receipt/view/pdf/{transactionId}")
    public ResponseEntity<byte[]> viewReceipt(@PathVariable String transactionId) throws IOException {

        byte[] pdf = null;
        Optional<Transaction> optTrans = transactionRepository.findByTransactionId(transactionId);
        if(optTrans.isPresent()){
            Transaction tx = optTrans.get();
            pdf = pdfService.generateReceipt(tx);
             System.out.println("found");
        } else {
            //testing
            pdf = pdfService.generateReceiptTest();
             System.out.println("not found");
        }

        //TODO: FE PART -> window.open(`/api/transactions/download/receipt/pdf/${id}`, "_blank");
        // OR <iframe src="/api/transactions/download/receipt/pdf/123" width="100%" height="600px"></iframe>

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=receipt.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
    
}
