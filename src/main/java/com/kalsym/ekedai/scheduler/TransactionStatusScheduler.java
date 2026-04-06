package com.kalsym.ekedai.scheduler;

import com.kalsym.ekedai.model.Transaction;
import com.kalsym.ekedai.model.enums.PaymentStatus;
import com.kalsym.ekedai.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@Profile({ "!dev" })
public class TransactionStatusScheduler {

    @Autowired
    private TransactionRepository transactionRepository;

    // This will run every 5 minutes
    @Scheduled(fixedRate = 300000)
    public void updateStaleTransactions() {
        // Check for transactions that have been in a 'PENDING' state for more than 15
        // minutes
        // Subtract 15 minutes from the current LocalDateTime
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);

        // Convert LocalDateTime to Date
        Date cutoffDate = Date.from(fifteenMinutesAgo.atZone(ZoneId.systemDefault()).toInstant());

        // Call the method with the Date parameter
        List<Transaction> staleTransactions = transactionRepository.findStaleTransactions(cutoffDate);
        for (Transaction transaction : staleTransactions) {
            if ("PENDING".equals(transaction.getStatus())
                    && PaymentStatus.PENDING.equals(transaction.getPaymentStatus())) {
                // Update to CANCELLED
                transaction.setStatus("FAILED");
                transaction.setPaymentStatus(PaymentStatus.CANCELLED);
                transactionRepository.save(transaction);
            }
        }
    }
}
