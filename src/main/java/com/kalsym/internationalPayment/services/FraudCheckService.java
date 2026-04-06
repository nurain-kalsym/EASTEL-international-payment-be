package com.kalsym.internationalPayment.services;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.Transaction;
import com.kalsym.internationalPayment.model.enums.PaymentStatus;
import com.kalsym.internationalPayment.repositories.TransactionRepository;
import com.kalsym.internationalPayment.utility.HttpResponse;
import com.kalsym.internationalPayment.utility.Logger;

@Service
public class FraudCheckService {

    @Autowired
    private FraudCheckClient fraudCheckClient;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EmailService emailService;

    @Async
    @Retryable(value = { Exception.class }, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    public void asyncCheckFraud1st(HttpServletRequest request, Transaction transaction, String logprefix) {
        try {
            HttpResponse responseCheckFraud = fraudCheckClient.firstCheckFraud(request, transaction.getTransactionId());
            if (responseCheckFraud.getStatus() == 200 && "1".equals(responseCheckFraud.getData())) {
                transaction.setStatus("REJECTED");
                transaction.setPaymentStatus(PaymentStatus.FAILED);
                transaction.setErrorDescription("POTENTIAL FRAUD - 1st Check");
                transaction.setIsFraud(true);
                transactionRepository.save(transaction);
            }

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "success check 1st: status passed");

        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "error check 1st fraud: failed to check -> " + e.getMessage());
            throw e; // Let Spring retry
        }
    }

    @Retryable(value = { Exception.class }, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    public boolean checkFraud2nd(HttpServletRequest request, Transaction transaction, String errorCode,
            String logprefix) {
        try {
            HttpResponse responseCheckFraud = fraudCheckClient.secondCheckFraud(request,
                    transaction.getTransactionId());
            if (responseCheckFraud.getStatus() == 200 && responseCheckFraud.getData() == "1") {
                if ("000".equals(errorCode)) {
                    transaction.setPaymentStatus(PaymentStatus.PAID);
                } else if ("003".equals(errorCode)) {
                    transaction.setPaymentStatus(PaymentStatus.PENDING);
                } else {
                    transaction.setPaymentStatus(PaymentStatus.FAILED);
                    transaction.setStatus("FAILED");
                }
                transaction.setErrorDescription("POTENTIAL FRAUD - 2nd Check: POSSIBLE FRAUD");
                transaction.setIsFraud(true);
                Transaction savedTransaction = transactionRepository.save(transaction);
                try {
                    // Send fraud alert email
                    emailService.sendFraudAlert(savedTransaction);
                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Fraud alert email sent successfully for transaction: "
                                    + savedTransaction.getTransactionId());
                } catch (Exception e) {
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Failed to send fraud alert email: " + e.getMessage());
                }

                return false;
            }
            return true;
        } catch (Exception e) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "failed check 2nd fraud: failed to check" + e.getMessage());
            throw e; // Let Spring retry
        }
    }

}
