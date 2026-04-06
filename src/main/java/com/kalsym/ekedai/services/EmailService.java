package com.kalsym.ekedai.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.utility.Logger;
import com.kalsym.ekedai.model.Transaction;
import com.kalsym.ekedai.model.User;
import com.kalsym.ekedai.repositories.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Value("${receiver.email:fraud@mcoopon.my}")
    String receiverEmail;

    @Value("${channel.name:e-kedai}")
    String channelName;

    public void sendFraudAlert(Transaction transaction) throws MessagingException, IOException {

        // Do not send email if channel name is not e-kedai
        if (!channelName.equals("e-kedai")) {
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, "sendFraudAlert",
                    "Fraud alert email not sent for transaction: " + transaction.getTransactionId()
                            + " because channel name is not e-kedai");
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());

        String body = buildEmailBody(transaction);

        helper.setFrom("notification@mcoopon.my", "Ebyzarr Fraud Detection Alert");
        helper.setTo(receiverEmail);
        helper.setSubject("🚨 Fraud Alert: Suspicious Transaction Detected");
        helper.setText(body, true);

        mailSender.send(message);
    }

    private String buildEmailBody(Transaction transaction) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/fraud-alert-email.html");
        String template;
        try (InputStream inputStream = resource.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            template = reader.lines().collect(Collectors.joining("\n"));
        }

        String currentYear = String.valueOf(Year.now().getValue());
        String paymentMethod = "V".equals(transaction.getPaymentChannel()) ? "Visa" : "Mastercard";

        Optional<User> user = userRepository.findById(transaction.getUserId());
        String userName = transaction.getName();
        String userNationality = "N/A";
        String userEmail = transaction.getEmail();
        String userPhone = transaction.getPhoneNo();

        if (user.isPresent()) {
            User foundUser = user.get();
            userName = foundUser.getFullName() != null ? foundUser.getFullName() : userName;
            userNationality = foundUser.getNationality() != null ? foundUser.getNationality() : "N/A";
            userEmail = foundUser.getEmail() != null ? foundUser.getEmail() : userEmail;
            userPhone = foundUser.getPhoneNumber() != null ? foundUser.getPhoneNumber() : userPhone;
        }

        return template
                .replace("{{createdDate}}", transaction.getCreatedDate().toString())
                .replace("{{transactionId}}", transaction.getTransactionId())
                .replace("{{transactionAmount}}", String.format("%.2f", transaction.getTransactionAmount()))
                .replace("{{paymentMethod}}", "Credit/Debit - " + paymentMethod)
                .replace("{{productName}}", transaction.getProduct().getProductName())
                .replace("{{variantName}}", transaction.getProductVariant().getVariantName())
                .replace("{{paidDate}}", transaction.getPaidDate().toString())
                .replace("{{updatedDate}}", transaction.getUpdatedDate().toString())
                .replace("{{fullName}}", userName)
                .replace("{{nationality}}", userNationality)
                .replace("{{email}}", userEmail)
                .replace("{{phoneNo}}", userPhone)
                .replace("{{year}}", currentYear);

    }

}
