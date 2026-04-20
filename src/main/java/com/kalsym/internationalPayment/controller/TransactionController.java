package com.kalsym.internationalPayment.controller;

import java.io.IOException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.controller.PaymentController.Payment;
import com.kalsym.internationalPayment.model.User;
import com.kalsym.internationalPayment.model.dao.TransactionDto;
import com.kalsym.internationalPayment.model.enums.PaymentStatus;
import com.kalsym.internationalPayment.model.enums.VariantType;
import com.kalsym.internationalPayment.repositories.TransactionRepository;
import com.kalsym.internationalPayment.services.PdfService;
import com.kalsym.internationalPayment.services.TransactionService;
import com.kalsym.internationalPayment.services.UserService;
import com.kalsym.internationalPayment.utility.HttpResponse;
import com.kalsym.internationalPayment.utility.Logger;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


import static com.kalsym.internationalPayment.filter.SessionRequestFilter.HEADER_STRING;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    UserService userService;
    
    @Autowired
    TransactionService transactionService;
    

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    PdfService pdfService;

  
    @Operation(summary = "Get transaction by ID", description = "To retrieve transaction by ID")
    @GetMapping(path = { "/{transactionId}" })
    public ResponseEntity<HttpResponse> getByTransactionId(
            HttpServletRequest request,
            @PathVariable(name = "transactionId") String transactionId) throws Exception {
        String logprefix = "getByTransactionId";
        HttpResponse response = new HttpResponse(request.getRequestURI());
        
        response = transactionService.getTransactionById(response, logprefix, transactionId);

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
            
        response = transactionService.getTransactionHistory(response, logprefix, user.getId(),
                                                            sortingOrder, page, pageSize, sortBy,
                                                            from, to, variantType, status, paymentStatus,
                                                            globalSearch);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }
    
   
    @Operation(summary = "Get transaction status by ID", description = "To retrieve transaction status by ID")
    @GetMapping("/{transactionId}/status")
    public ResponseEntity<HttpResponse> getStatusByTransactionId(HttpServletRequest request,
            @PathVariable String transactionId) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = request.getRequestURI() + " getStatusByTransactionId() ";
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "transactionId: ",
                transactionId);
    
        response = transactionService.getStatusByTxId(response, logprefix, transactionId);

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "", description = "")
    @GetMapping("/receipt/download/pdf/{transactionId}")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable String transactionId) throws IOException {
        String logPrefix = "downloadReceipt";
        byte[] pdf = null;

        pdf = transactionService.getTxPdf(pdf, logPrefix, transactionId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "", description = "")
    @GetMapping("/receipt/view/pdf/{transactionId}")
    public ResponseEntity<byte[]> viewReceipt(@PathVariable String transactionId) throws IOException {

        String logPrefix = "viewReceipt";
        byte[] pdf = null;

        pdf = transactionService.getTxPdf(pdf, logPrefix, transactionId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=receipt.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Create transaction", description = "To create new transaction")
    @PostMapping(path = { "/create" })
    public ResponseEntity<HttpResponse> createTransaction(HttpServletRequest request,
            @Valid @RequestBody TransactionDto tx) throws Exception {
        String logprefix = request.getRequestURI() + " createTransaction() ";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        User user = userService.getUser(request.getHeader(HEADER_STRING));

        if (user == null) {
            response.setStatus(HttpStatus.NOT_FOUND);
            response.setMessage("User Not Found");
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "User Not Found");
                    
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        response = transactionService.createTransaction(response, logprefix, tx, user);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Get exchanged tx amount", description = "To retrieve tx amount for billpayment in exchanged rate.")
    @GetMapping(path = { "/exchange-currency/quote" })
    public ResponseEntity<HttpResponse> quoteBillPayment(HttpServletRequest request,
            @RequestParam Integer variantId, @RequestParam String countryCode, Double amount) throws Exception {
        String logprefix = request.getRequestURI() + " quoteBillPayment() ";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        response = transactionService.getExchangeAmount(response, logprefix, variantId, countryCode, amount);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
}
