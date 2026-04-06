package com.kalsym.ekedai.scheduler;

import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.model.Transaction;
import com.kalsym.ekedai.model.dao.MMResponse;
import com.kalsym.ekedai.model.dao.MtradePaymentResponse;
import com.kalsym.ekedai.model.enums.PaymentStatus;
import com.kalsym.ekedai.model.enums.TransactionEnum;
import com.kalsym.ekedai.repositories.ProductVariantRepository;
import com.kalsym.ekedai.repositories.TransactionRepository;
import com.kalsym.ekedai.services.PaymentService;
import com.kalsym.ekedai.services.SmsService;
import com.kalsym.ekedai.services.WSPRequestService;
import com.kalsym.ekedai.utility.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

@Service
@Profile({ "!dev" })
public class QueryTransactionSheduler {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    ProductVariantRepository productVariantRepository;

    @Autowired
    WSPRequestService wspRequestService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    SmsService smsService;

    @Value("${sms.brand:eByzarr}")
    private String smsBrand;

    @Scheduled(cron = "${pending-transaction:0 0/1 * * * ?}")
    public void queryPendingTransactions() {
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
        Logger.application.info("Query Pending Transaction");
        try {
            List<Transaction> processingTransactions = transactionRepository.findByPaymentStatusAndStatus(
                    PaymentStatus.PAID, "PROCESSING");

            for (Transaction transaction : processingTransactions) {
                Logger.application.info(
                        "QueryPendingTXN : Method {} , Transaction Id : {} , Payment_Status {}, Status {} ",
                        methodName, transaction.getId(), transaction.getStatus(), transaction.getPaymentStatus());
                if (transaction.getWspTransactionId() != null) {
                    processTransaction(transaction);
                }
            }
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION,
                    "queryPendingTransactions() Exception " + e.getMessage());
        }

    }

    private void processTransaction(Transaction transaction) {
        try {
            MtradePaymentResponse response = wspRequestService.queryTransaction(transaction);

            final String responseCode = response.getResponseCode();
            final String responseDescription = response.getResponseDescription();
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION,
                    "processTransaction, response : " + response);

            if (responseCode.isEmpty()) {
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, "queryPendingTransactions() "
                        + transaction.getTransactionId() + " Process Abort, Reason: Response Code null or empty");
                return;
            } else {

                final String status = getTransactionStatus(responseCode);
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, "status: " + status);

                transaction.setStatus(status);
                transaction.setUpdatedDate(new Date());
                transaction.setErrorDescription(responseDescription);
                transaction.setTransactionErrorCode(responseCode);

                if (transaction.getTransactionType().equals(TransactionEnum.VOUCHER)) {
                    setVoucherDetails(transaction, response);
                }

                Transaction newTransaction = transactionRepository.save(transaction);
                newTransaction.setProductVariant(null);
                newTransaction.setEditBy(null);
                newTransaction.setProduct(null);
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, "newTransaction: " + newTransaction);
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION,
                        "saved transaction: transaction error code : " + newTransaction.getTransactionErrorCode()
                                + " transaction status : " + newTransaction.getStatus()
                                + " transaction error description : " + transaction.getErrorDescription());

            }

        } catch (Exception e) {
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION,
                    "Exception() " + transaction.getTransactionId() + " Process Abort " + e.getMessage());
        }
    }

    private String getTransactionStatus(String responseCode) {
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION,
                "getTransactionStatus() responseCode: " + responseCode);

        if ("000".equals(responseCode)) {
            return "PAID";
        } else if ("001".equals(responseCode)) {
            return "PROCESSING";
        }

        return "FAILED";
    }

    private void setVoucherDetails(Transaction transaction, MtradePaymentResponse response) {
        transaction.setVoucherUrl(response.getVoucherUrl());
        transaction.setVoucherSerial(response.getVoucherSerial());
        transaction.setVoucherNo(response.getVoucherNo());
        transaction.setVoucherExpiryDate(response.getVoucherExpiryDate());
    }

    @Scheduled(cron = "${failed-transaction:0 * * * * ?}")
    public void QueryFailedTransaction() throws ParseException {
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        List<Transaction> transactions = transactionRepository.findByPaymentMethodAndPaymentStatusAndStatus("MMWALLET",
                PaymentStatus.PAID, "FAILED");
        Logger.application.info("Query Failed Transaction : " + transactions.size());

        for (Transaction transaction : transactions) {
            Logger.application.info(
                    "QueryFailedTXN : Location {} , Transaction Id : {} , Status {}, Payment_Status {} ", location,
                    transaction.getId(), transaction.getStatus(), transaction.getPaymentStatus());
            MMResponse mmResponse = paymentService.refundTransaction(transaction);
            transaction.setRefundCode(mmResponse.getCode());
            transaction.setRefundErrorMessage(mmResponse.getMessage());

            Logger.application
                    .info("Refund ssetNotificationSent Code: " + transaction.getRefundCode() + " , Status: "
                            + mmResponse.getStatus());
            if ("000".equals(mmResponse.getCode())) {
                transaction.setPaymentStatus(PaymentStatus.REFUNDED);

                String formattedAmount = String.format("%.2f", transaction.getDenoAmount());

                String message = "RM0 " + smsBrand + ": Your eByzarr transaction RM" + formattedAmount +
                        " has been refunded. Ref ID: " + transaction.getTransactionId() + ".";
                try {
                    smsService.sendHttpGetRequest(transaction.getPhoneNo(), message, false);
                    transaction.setNotificationSent(true);
                    Logger.application
                            .info("Refund success send message to: " + transaction.getPhoneNo());
                } catch (Exception e) {
                    transaction.setNotificationSent(false);
                    Logger.application
                            .info("Refund failed to send message to: " + transaction.getPhoneNo() + " , Exception: "
                                    + e.getMessage());
                }
            }
            transactionRepository.save(transaction);
        }
    }

}
