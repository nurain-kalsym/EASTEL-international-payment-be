package com.kalsym.ekedai.scheduler;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.kalsym.ekedai.model.Commission;
import com.kalsym.ekedai.model.ProductVariant;
import com.kalsym.ekedai.model.Settlement;
import com.kalsym.ekedai.model.Summary;
import com.kalsym.ekedai.model.Transaction;
import com.kalsym.ekedai.model.enums.PaymentStatus;
import com.kalsym.ekedai.repositories.CommissionRepository;
import com.kalsym.ekedai.repositories.ProductVariantRepository;
import com.kalsym.ekedai.repositories.SettlementRepository;
import com.kalsym.ekedai.repositories.SummaryRepository;
import com.kalsym.ekedai.repositories.TransactionRepository;
import com.kalsym.ekedai.utility.Logger;

@Service
@Profile({"!dev"})
public class SettlementSheduler {

    @Autowired
    SettlementRepository settlementRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    CommissionRepository commissionRepository;

    @Autowired
    SummaryRepository summaryRepository;

    @Autowired
    ProductVariantRepository productVariantRepository;

    /**
     * Cron Breakdown:
     * - 0: At the start of the 30th minute (i.e., zero seconds).
     * - 30: At the 30th minute.
     * - 22: At 10 PM (22nd hour in 24-hour format).
     * - *: Every day of the month.
     * - *: Every month.
     * - ?: No specific day of the week.
     * 
     * In summary: This runs daily at 10:30 PM.
     */
    @Scheduled(cron = "${summary-transaction:0 30 22 * * ?}")
    public void QueryPendingTransaction() throws ParseException {
        // String location = Thread.currentThread().getStackTrace()[1].getMethodName();

        Logger.application.info("Cron of summary started");

        // Calculate start date (3 days ago at 10:01 pm)
        Date startDate = calculateStartDate();
        // Calculate end date (2 days ago at 10 pm)
        Date endDate = calculateEndDate();
        // for transaction of 2 days ago
        LocalDate twoDaysAgoDate = getTwoDaysAgoDate();

        List<Transaction> transactions = transactionRepository
                .findByPaidDateBetweenAndPaymentChannelStartingWithAndPaymentStatus(startDate, endDate, "OZO-",
                        PaymentStatus.PAID);

        summaryAndSettlementForTransactions(transactions, twoDaysAgoDate);

        Logger.application.info("Cron of summary ended");

    }

    private void summaryAndSettlementForTransactions(List<Transaction> transactions, LocalDate twoDaysAgoDate) {

        Summary summary = new Summary();

        double totalAmount = 0;
        double totalCollectionAmount = 0;
        int totaltx = 0;

        for (Transaction transaction : transactions) {
                    Settlement settlement = new Settlement();

            String type = transaction.getPaymentChannel().substring(4);
            ProductVariant productVariant = productVariantRepository.findById(transaction.getProductVariantId()).get();
            Commission commission = commissionRepository.findByPaymentType(type);

            double amount = transaction.getTransactionAmount();
            double rate = commission.getRate();
            double collected = 0;

            System.out.println("commission.getFixed()"+commission.getFixed());

            if(commission.getFixed())
                collected = amount - rate;
            else
                collected = amount - (amount * (rate/100));

            // set for settlement
            settlement.setPaymentMethod(type);
            settlement.setWspProductCode(productVariant.getWspProductCode());
            settlement.setCollectionAmount(collected);
            settlement.setTransactionAmount(amount);
            settlement.setProductName(computeProductName(transaction, productVariant));
            settlement.setPaymentTransactionId(transaction.getPaymentTransactionId());
            settlement.setTransactionDate(transaction.getPaidDate());
            settlement.setBatchDate(twoDaysAgoDate);

            totalCollectionAmount += collected;
            totalAmount += amount;
            totaltx++;

            // save settlement 1 by 1
            settlementRepository.save(settlement);
        }

        // set for summary
        summary.setTotalTransaction(totaltx);
        summary.setDate(twoDaysAgoDate);
        summary.setTotalCollection(totalCollectionAmount);
        summary.setTotalAmount(totalAmount);

        // save summary
        summaryRepository.save(summary);
    }

    private String computeProductName(Transaction transaction, ProductVariant productVariant) {
        String productName = transaction.getProduct().getProductName();
        if (!productName.equals(productVariant.getVariantName())) {
            return productName + " - " + productVariant.getVariantName();
        }
        return productName;
    }

    private Date calculateStartDate() {
        Calendar startCal = Calendar.getInstance();

        startCal.add(Calendar.DAY_OF_MONTH, -3);
        startCal.set(Calendar.HOUR_OF_DAY, 22);
        startCal.set(Calendar.MINUTE, 1);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        return startCal.getTime();
    }

    private Date calculateEndDate() {
        Calendar endCal = Calendar.getInstance();

        endCal.add(Calendar.DAY_OF_MONTH, -2);
        endCal.set(Calendar.HOUR_OF_DAY, 22);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 59);// Set second to 59
        endCal.set(Calendar.MILLISECOND, 0);
        return endCal.getTime();
    }

    private LocalDate getTwoDaysAgoDate() {
        return LocalDate.now().minusDays(2);
    }
}
