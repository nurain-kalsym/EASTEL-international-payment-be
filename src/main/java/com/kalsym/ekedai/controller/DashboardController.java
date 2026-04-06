package com.kalsym.ekedai.controller;

import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.model.Country;
import com.kalsym.ekedai.model.Transaction;
import com.kalsym.ekedai.model.User;
import com.kalsym.ekedai.model.enums.PaymentStatus;
import com.kalsym.ekedai.repositories.CountryRepository;
import com.kalsym.ekedai.repositories.TransactionRepository;
import com.kalsym.ekedai.repositories.UserRepository;
import com.kalsym.ekedai.utility.GeneratePdfUtils;
import com.kalsym.ekedai.utility.HttpResponse;
import com.kalsym.ekedai.utility.Logger;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("dashboard")
public class DashboardController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    private CountryRepository countryRepository;

    @GetMapping("/transactionCounts")
    public ResponseEntity<HttpResponse> getTransactionCounts(HttpServletRequest request,
            @RequestParam() @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam() @DateTimeFormat(pattern = "yyyy-MM-dd") Date to) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getUsers";

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(to);
        calendar.add(Calendar.DAY_OF_MONTH, 1); // To include the end date in the search
        Date updatedToDate = calendar.getTime();

        try {
            Map<String, Object> counts = new HashMap<>();
            // Box 1
            counts.put("txn_PENDING_payment_PENDING",
                    transactionRepository.countByStatusAndPaymentStatusAndCreatedDateBetween("PENDING",
                            PaymentStatus.PENDING, from, updatedToDate));
            counts.put("payment_FAILED", transactionRepository
                    .countByPaymentStatusAndCreatedDateBetween(PaymentStatus.FAILED, from, updatedToDate));

            // Box 2
            counts.put("payment_PAID", transactionRepository
                    .countByPaymentStatusAndCreatedDateBetween(PaymentStatus.PAID, from, updatedToDate));
            counts.put("trx_PENDING_payment_PAID",
                    transactionRepository.countByStatusAndPaymentStatusAndCreatedDateBetween("PENDING",
                            PaymentStatus.PAID, from, updatedToDate));

            // Box 3
            // counts.put("payment_PAID",
            // transactionRepository.countByPaymentStatusAndCreatedDateBetween(PaymentStatus.PAID,
            // from, updatedToDate));
            counts.put("trx_PAID",
                    transactionRepository.countByStatusAndCreatedDateBetween("PAID", from, updatedToDate));

            // Box 4
            counts.put("payment_REFUNDED", transactionRepository
                    .countByPaymentStatusAndCreatedDateBetween(PaymentStatus.REFUNDED, from, updatedToDate));
            counts.put("trx_FAILED_payment_PAID",
                    transactionRepository.countByStatusAndPaymentStatusAndCreatedDateBetween("FAILED",
                            PaymentStatus.PAID, from, updatedToDate));

            response.setStatus(HttpStatus.OK);
            response.setData(counts);

        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Failed to get transaction counts: " + e.getMessage());
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/userCounts")
    public ResponseEntity<HttpResponse> getUserCounts(HttpServletRequest request,
            @RequestParam() @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam() @DateTimeFormat(pattern = "yyyy-MM-dd") Date to) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getUsers";

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(to);
        calendar.add(Calendar.DAY_OF_MONTH, 1); // To include the end date in the search
        Date updatedToDate = calendar.getTime();

        try {
            long counts = userRepository.countByCreatedBetween(from, updatedToDate);

            response.setStatus(HttpStatus.OK);
            response.setData(counts);

        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Failed to get user counts: " + e.getMessage());
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping(path = {
            "/download-transaction-receipt/{transactionId}" }, name = "transaction-get-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> downloadTransactionReceipt(HttpServletRequest request,
            @PathVariable String transactionId) {

        String logprefix = request.getRequestURI() + "getPdf()";
        Optional<Transaction> optionalTransaction = transactionRepository.findByTransactionId(transactionId);

        if (!optionalTransaction.isPresent()) {
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Transaction not found with id: " + transactionId);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("Content-Disposition", "inline; filename=" + transactionId + ".pdf");

            return ResponseEntity
                    .ok()
                    .headers(responseHeaders)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(null);
        }

        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Transaction found with id: " + transactionId);
        Transaction transaction = optionalTransaction.get();

        // Get transaction country
        Optional<Country> countryOptional = countryRepository.findById(transaction.getProduct().getCountryCode());

        // Generate PDF
        byte[] pdfBytes = GeneratePdfUtils.transactionReceipt(transaction, countryOptional.get());

        ByteArrayInputStream resource = new ByteArrayInputStream(pdfBytes);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Content-Disposition", "inline; filename=" + transaction.getTransactionId() + ".pdf");

        return ResponseEntity
                .ok()
                .headers(responseHeaders)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(resource));

    }

    @GetMapping("/transactionCountsByUser")
    public ResponseEntity<HttpResponse> getTransactionCountsByUser(HttpServletRequest request,
                                                                   @RequestParam() String userId) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getTransactionCountsByUser";

        Optional<User> optionalUser = userRepository.findById(userId);

        if (!optionalUser.isPresent()) {
            response.setStatus(HttpStatus.NOT_FOUND);
            response.setMessage("User not found");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        try {

//            List<Transaction> transactions = transactionRepository.findByUserIdAndProductIdIsNotNullAndProductVariantIdIsNotNull(userId);
//
//            // Group transactions by status and count occurrences
//            Map<String, Long> statusCounts = transactions.stream()
//                    .collect(Collectors.groupingBy(Transaction::getStatus, Collectors.counting()));
//
//            // Convert the Map to a List of StatusCount
//            List<StatusCount> statusCountList = statusCounts.entrySet().stream()
//                    .map(entry -> new StatusCount(entry.getKey(), entry.getValue()))
//                    .collect(Collectors.toList());

            List<Object[]> queryResults = transactionRepository.getTransactionCountsByUser(userId);

            List<StatusCount> statusCountList = queryResults.stream()
                    .map(row -> new StatusCount((String) row[0], ((Number) row[1]).longValue()))
                    .collect(Collectors.toList());

            response.setStatus(HttpStatus.OK);
            response.setData(statusCountList);

        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Failed to get transaction counts: " + e.getMessage());
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Getter
    @Setter
    public static class StatusCount {
        private String status;
        private Long count;

        public StatusCount(String status, Long count) {
            this.status = status;
            this.count = count;
        }
    }
}
