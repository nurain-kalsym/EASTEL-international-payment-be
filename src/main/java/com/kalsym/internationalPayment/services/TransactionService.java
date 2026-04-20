package com.kalsym.internationalPayment.services;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.controller.PaymentController.Payment;
import com.kalsym.internationalPayment.model.Country;
import com.kalsym.internationalPayment.model.Product;
import com.kalsym.internationalPayment.model.ProductVariant;
import com.kalsym.internationalPayment.model.Transaction;
import com.kalsym.internationalPayment.model.User;
import com.kalsym.internationalPayment.model.Wallet;
import com.kalsym.internationalPayment.model.dao.RetailRate;
import com.kalsym.internationalPayment.model.dao.TransactionDto;
import com.kalsym.internationalPayment.model.enums.PaymentStatus;
import com.kalsym.internationalPayment.model.enums.Status;
import com.kalsym.internationalPayment.model.enums.TransactionEnum;
import com.kalsym.internationalPayment.model.enums.VariantType;
import com.kalsym.internationalPayment.repositories.CountryRepository;
import com.kalsym.internationalPayment.repositories.ProductRepository;
import com.kalsym.internationalPayment.repositories.ProductVariantRepository;
import com.kalsym.internationalPayment.repositories.TransactionRepository;
import com.kalsym.internationalPayment.repositories.WalletRepository;
import com.kalsym.internationalPayment.utility.HttpResponse;
import com.kalsym.internationalPayment.utility.Logger;
import com.kalsym.internationalPayment.utility.StringUtility;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

@Service
public class TransactionService {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    UserService userService;

    @Autowired
    PdfService pdfService;
    
    @Autowired
    ProductVariantRepository productVariantDb;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CountryRepository countryRepository;

    @Autowired
    WSPRequestService wspRequestService;

    @Autowired
    WalletRepository walletRepository;

    public HttpResponse createTransaction(HttpResponse response, String logPrefix, TransactionDto tx, User user) {

        try {
            String systemTransactionId = null;
            Double fixFee = tx.getFixFee();
            Transaction transaction = new Transaction();

            ProductVariant productVariant = null;
            if (tx.getProductVariantId() != null) {
                productVariant = productVariantDb.findById(tx.getProductVariantId()).orElse(null);
                // Check if the exchange rate is found
                if (productVariant == null) {
                    response.setStatus(HttpStatus.NOT_FOUND, "Product Variant Not Found");

                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                            transaction.getTransactionId(), "Product Variant Not Found : ", transaction);

                    return response;
                }
                Product product = productRepository.findById(productVariant.getProductId()).orElse(null);
                if (product == null) {
                    response.setStatus(HttpStatus.NOT_FOUND, "Product Not Found");

                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                            transaction.getTransactionId(), "Product Not Found : ", transaction);

                    return response;
                }
                transaction.setProduct(product);
                transaction.setProductVariantId(productVariant.getId());

                // Check retail rate first before save into for bill payment only
                if (productVariant.getVariantType().equals(VariantType.BILLPAYMENT)) {
                    Country country = countryRepository.findById(product.getCountryCode()).get();

                    if (country == null) {
                        response.setStatus(HttpStatus.NOT_FOUND, "Country Not Found");
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                transaction.getTransactionId(), "Country Not Found : ", transaction);

                        return response;
                    }

                    //RetailRate retailRate = wspRequestService.requestRetailRate(country.getWspCountryCode());
                    RetailRate retailRate = null;

                    // Check if the exchange rate is found
                    if (retailRate == null) {
                        //TEMP SOLUTION WHILE WSP IS NOT UP
                        //response.setStatus(HttpStatus.NOT_FOUND, "Retail Rate Not Found");
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                transaction.getTransactionId(), "Retail Rate Not Found : ", transaction);
                        retailRate = new RetailRate(); 
                        retailRate.setRate(1.00);
                    }

                    transaction.setDenoAmount(tx.getTransactionAmount());

                    double valueAfterRate = (tx.getTransactionAmount() / retailRate.getRate());

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

            transaction.setPaymentMethod("WALLET");
            transaction.setAccountNo(tx.getAccountNo());
            transaction.setUserId(user.getId());
            transaction.setEmail(user.getEmail());
            transaction.setName(user.getFullName());
            transaction.setPhoneNo(user.getPhoneNumber());
            transaction.setTransactionType(tx.getPaymentEnum());
            transaction.setStatus("PAID");
            transaction.setPaymentStatus(PaymentStatus.PAID);
            transaction.setCreatedDate(new Date());
            transaction.setBillPhoneNumber(tx.getBillPhoneNumber());
            transaction.setExtra1(tx.getExtra1());
            transaction.setExtra2(tx.getExtra2());
            transaction.setExtra3(tx.getExtra3());
            transaction.setExtra4(tx.getExtra4());
            systemTransactionId = StringUtility.CreateRefID("EIP");
            transaction.setTransactionId(systemTransactionId);

            // recalculate wallet
            Optional<Wallet> walletOpt = walletRepository.findByUserIdAndStatus(user.getId(), Status.ACTIVE);
            if (!walletOpt.isPresent()) {
                response.setStatus(HttpStatus.NOT_FOUND, "Wallet Not Found / Inactive");
                
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                    transaction.getTransactionId(), "Wallet Not Found / Inactive : ", transaction);
                    
                return response;
            }
            
            Wallet wallet = walletOpt.get();

            Double balance = wallet.getBalance() - transaction.getTransactionAmount();
            if (balance < 0) {
                response.setStatus(HttpStatus.EXPECTATION_FAILED, "Insufficent balance: RM "  + wallet.getBalance());
                
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                    transaction.getTransactionId(), "Insufficent balance: RM " + wallet.getBalance());
                    
                return response;
            }

            wallet.setBalance(balance);
            wallet.setRemarks("Last action: Usage for TX. Ref ID: " + transaction.getTransactionId());
            wallet.setUpdated(new Date());
            walletRepository.save(wallet);
            
            Transaction savedTransaction = transactionRepository.save(transaction);
            response.setData(savedTransaction);

        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "Exception: ",
                    e.getMessage());
            response.setStatus(HttpStatus.EXPECTATION_FAILED, e.getLocalizedMessage());
        }



        return response;
    }

    public HttpResponse getTransactionById(HttpResponse response, String logPrefix, String transactionId) {

        try { 
            Optional<Transaction> transaction = transactionRepository.findByTransactionId(transactionId);
    
            if (transaction.isPresent()) {
                response.setData(transaction.get());
                response.setStatus(HttpStatus.OK);
            } else {
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setMessage("Transaction not found");
            }
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "Exception: ",
                    e.getMessage());
            response.setStatus(HttpStatus.EXPECTATION_FAILED, e.getLocalizedMessage());
        }

        return response;

    }

    public HttpResponse getTransactionHistory(HttpResponse response, String logPrefix, String userId,
                                                String sortingOrder, int page, int pageSize, String sortBy,
                                                Date from, Date to, VariantType variantType, String status,
                                                PaymentStatus paymentStatus, String globalSearch
    ) {

        try {
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
    
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
                                    globalSearch),
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
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "Exception: ",
                    e.getMessage());
            response.setStatus(HttpStatus.EXPECTATION_FAILED, e.getLocalizedMessage());
        }

     
        return response;
    }

    public static Specification<Transaction> getTransactionHistoryByUser(
            Date from, Date to, Example<Transaction> example, VariantType variantType, String status,
            PaymentStatus paymentStatus, String globalSearch) {
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

    public HttpResponse getStatusByTxId(HttpResponse response, String logPrefix, String transactionId) {
         try {
            Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
            Transaction transaction = transactionOpt.get();
            if (transaction != null) {
                response.setStatus(HttpStatus.OK);
                response.setData(transaction.getStatus());
            } else {
                response.setStatus(HttpStatus.NOT_FOUND);
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "transactionId: ",
                        transactionId,
                        "NOT_FOUND");
            }
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "Exception: ",
                    e.getMessage());
            response.setStatus(HttpStatus.EXPECTATION_FAILED, e.getLocalizedMessage());;
        }

        return response;

    }

    public byte[] getTxPdf(byte[] pdf,String logPrefix, String transactionId) {
        try {
            Optional<Transaction> optTrans = transactionRepository.findByTransactionId(transactionId);

            if(optTrans.isPresent()){
                Transaction tx = optTrans.get();
                pdf = pdfService.generateReceipt(tx);
            } else {
                //testing
                pdf = pdfService.generateReceiptTest();
            }
            
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "Exception: ",
                    e.getMessage());
            pdf = null;
        }

        return pdf;
    }

    public HttpResponse getExchangeAmount(HttpResponse response, String logPrefix, Integer productVariantId, 
                                    String countryCode, Double amount) {

            ProductVariant productVariant = null;
            if (productVariantId != null) {
                productVariant = productVariantDb.findById(productVariantId).orElse(null);
                // Check if the exchange rate is found
                if (productVariant == null) {
                    response.setStatus(HttpStatus.NOT_FOUND, "Product Variant Not Found");

                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, 
                        "Product Variant Not Found: " + productVariantId);

                    return response;
                }

                if (productVariant.getVariantType().equals(VariantType.BILLPAYMENT)) {
                    Country country = countryRepository.findById(countryCode).get();

                    if (country == null) {
                        response.setStatus(HttpStatus.NOT_FOUND, "Country Not Found");
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, 
                                "Country Not Found: " + countryCode);

                        return response;
                    }

                    //RetailRate retailRate = wspRequestService.requestRetailRate(country.getWspCountryCode());
                    RetailRate retailRate = null;

                    // Check if the exchange rate is found
                    if (retailRate == null) {
                        //TEMP SOLUTION WHILE WSP IS NOT UP
                        //response.setStatus(HttpStatus.NOT_FOUND, "Retail Rate Not Found");
                         Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, 
                                "Retail Rate Not Found: " + countryCode);
                        retailRate = new RetailRate(); 
                        retailRate.setRate(1.00);
                    }

                    double valueAfterRate = (amount / retailRate.getRate());

                    // Round the value to two decimal places
                    double roundedValue = (Math.round(valueAfterRate * 100.0) / 100.0);

                    // Create a DecimalFormat object with two decimal places
                    DecimalFormat df = new DecimalFormat("0.00");
                    df.setRoundingMode(RoundingMode.HALF_UP);

                    // Format the double as a two-decimal-point Double
                    String formattedValue = df.format(roundedValue);

                    // Convert the formatted string back to a Double
                    Double transactionAmount = Double.parseDouble(formattedValue);
                    response.setData(transactionAmount);
                    response.setStatus(HttpStatus.OK);
                } else {
                    response.setStatus(HttpStatus.BAD_REQUEST, "Variant Type is not BILLPAYMENT");
                }
            }

        return response;

    }

}
