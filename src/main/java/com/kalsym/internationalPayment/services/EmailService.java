package com.kalsym.internationalPayment.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.stream.Collectors;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String otpCode, String username, String receiverEmail) throws MessagingException, IOException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());

        String body = buildOtpCodeEmailBody(otpCode, username);

        //temp
        receiverEmail = "nurain@kalsym.com";

        helper.setFrom("noreply-notify@eastel.my", "Eastel International Payment");
        helper.setTo(receiverEmail);
        helper.setSubject("Your OTP for Eastel Internation Payment");
        helper.setText(body, true);

        mailSender.send(message);
    }

    private String buildOtpCodeEmailBody(String otpCode, String username) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/otp-code-email.html");
        String template;
        try (InputStream inputStream = resource.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            template = reader.lines().collect(Collectors.joining("\n"));
        }

        String currentYear = String.valueOf(Year.now().getValue());
      
        return template
                .replace("{{username}}", username)
                .replace("{{otpCode}}", otpCode)
                .replace("{{year}}", currentYear);

    }

    public void sendRefundEmail(String username, String receiverEmail, String amount, String transactionId) throws MessagingException, IOException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());

        String body = buildRefundEmailBody(username, amount, transactionId);

        //temp
        receiverEmail = "nurain@kalsym.com";

        helper.setFrom("noreply-notify@eastel.my", "Eastel International Payment");
        helper.setTo(receiverEmail);
        helper.setSubject("Your transaction has been refunded");
        helper.setText(body, true);

        mailSender.send(message);
    }

    private String buildRefundEmailBody(String username, String amount, String transactionId) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/refund-email.html");
        String template;
        try (InputStream inputStream = resource.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            template = reader.lines().collect(Collectors.joining("\n"));
        }

        String currentYear = String.valueOf(Year.now().getValue());
      
        return template
                .replace("{{username}}", username)
                .replace("{{amount}}", amount)
                .replace("{{transactionId}}", transactionId)
                .replace("{{year}}", currentYear);

    }



    
}
