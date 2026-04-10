package com.kalsym.internationalPayment.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.kalsym.internationalPayment.model.Transaction;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;

@Service
public class PdfService {
    
    @Autowired
    private TemplateEngine templateEngine;

    public byte[] generateReceipt(Transaction tx) throws IOException {
        Context context = new Context();

        //header
        context.setVariable("receiptNo", tx.getTransactionId());
        context.setVariable("date", tx.getCreatedDate());

        //company info
        context.setVariable("companyName", "Eastel");
          context.setVariable("street", "99 Lane, Building AA");
        context.setVariable("cityStateCountry", "Kuala Lumpur, Malaysia");
        context.setVariable("zip", "50000");
        context.setVariable("phone", "+603-23456789");
        context.setVariable("compEmail", "inquiry@eastel.my");

        // user info
        context.setVariable("customerName", tx.getName());
        context.setVariable("accountNumber", tx.getAccountNo());
        context.setVariable("email", tx.getEmail());

        // product/bill info
        String productName = tx.getProduct().getProductName();
        String productVarName = tx.getProductVariant().getVariantName();
        context.setVariable("product", productName);
        context.setVariable("variant", " - " +productVarName);

        //TODO: if have redemption PIN then display, eg: reload prepaid PIN

        DecimalFormat df = new DecimalFormat("0.00");
        context.setVariable("amount", df.format(tx.getDenoAmount()));
        context.setVariable("total", df.format(tx.getTransactionAmount()));
        context.setVariable("serviceFee", df.format(0));
        context.setVariable("grandTotal", df.format(tx.getTransactionAmount()));

        //TODO: if foreign the have the exhanged amount too


        String html = templateEngine.process("receipt-pdf", context);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(html, null);
        builder.toStream(outputStream);
        builder.run();

        return outputStream.toByteArray();
    }

    
    public byte[] generateReceiptTest() throws IOException {
        Context context = new Context();

        // header
        context.setVariable("receiptNo", "TXN123456789");
        context.setVariable("date", "2026-04-08 14:30:00");

        // company info
        context.setVariable("companyName", "Eastel Corporation");
        context.setVariable("street", "99 Lane, Building AA");
        context.setVariable("cityStateCountry", "Kuala Lumpur, Malaysia");
        context.setVariable("zip", "50000");
        context.setVariable("phone", "+603-23456789");
        context.setVariable("compEmail", "inquiry@eastel.my");

        // user info
        context.setVariable("customerName", "John Doe");
        context.setVariable("accountNumber", "ACC987654321");
        context.setVariable("email", "johndoe@email.com");

        // product/bill info
        context.setVariable("product", "Hotlink");
        context.setVariable("variant", " - " + "TOP UP 10");
        DecimalFormat df = new DecimalFormat("0.00");
        context.setVariable("amount", df.format(9.5));
        context.setVariable("total", df.format(9.5));
        context.setVariable("serviceFee", df.format(0.5));
        context.setVariable("grandTotal", df.format(10.0));

        String html = templateEngine.process("receipt-pdf", context);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(html, null);
        builder.toStream(outputStream);
        builder.run();

        return outputStream.toByteArray();
    }
}
