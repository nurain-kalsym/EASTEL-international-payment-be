/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kalsym.internationalPayment.utility;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import com.kalsym.internationalPayment.model.Country;
import com.kalsym.internationalPayment.model.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

public class GeneratePdfUtils {

    public static byte[] transactionReceipt(Transaction transaction, Country country) {
        try (PDDocument document = new PDDocument()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Draw content
                drawSection(contentStream, 50, 700, "Paid Amount",
                        "RM" + formatAmount(transaction.getTransactionAmount()), true);
                drawMultilineText(contentStream, 50, 630, formatTransactionInformation(transaction));
                drawMultilineText(contentStream, 50, 480, formatBillingInformation(transaction, country));
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void drawMultilineText(PDPageContentStream contentStream, float x, float y, String text)
            throws IOException {
        String[] lines = text.split("\n");
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 12);
        contentStream.newLineAtOffset(x, y);
        for (String line : lines) {
            contentStream.showText(line);
            contentStream.newLineAtOffset(0, -20);
        }
        contentStream.endText();
    }

    @SuppressWarnings("unused")
    private static void addText(PDPageContentStream contentStream, String text, int yPos) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 12);
        contentStream.newLineAtOffset(100, yPos);
        contentStream.showText(text);
        contentStream.endText();
    }

    @SuppressWarnings("deprecation")
    private static void drawSection(PDPageContentStream contentStream, float x, float y, String title, String content,
            boolean isEmphasized) throws IOException {
        String[] lines = content.split("\n");
        float contentHeight = isEmphasized ? 20 : (15 * lines.length) + 20;

        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
        graphicsState.setNonStrokingAlphaConstant(0.5f);
        contentStream.setGraphicsStateParameters(graphicsState);

        // Draw the content box
        contentStream.setNonStrokingColor(224, 224, 224); // Light grey background
        contentStream.addRect(x, y - 20, 500, -contentHeight);
        contentStream.fill();
        contentStream.setNonStrokingColor(0, 0, 0); // Reset color to black for text

        drawMultilineText(contentStream, x + 5, y - (isEmphasized ? 15 : 95), content);
    }

    private static String formatTransactionInformation(Transaction details) {
        return "ID: " + details.getTransactionId() +
                "\nDate & Time: " + details.getCreatedDate() +
                "\nPayment Method: " + details.getPaymentMethod() +
                "\nPayment Status: " + details.getPaymentStatus();
    }

    private static String formatBillingInformation(Transaction details, Country country) {
        String category;
        String childCategory;

        if (details.getProduct().getProductCategoryDetails().getParentCategory() != null) {
            category = details.getProduct().getProductCategoryDetails().getParentCategory().getCategory();
            childCategory = details.getProduct().getProductCategoryDetails().getCategory();
        } else {
            category = details.getProduct().getProductCategoryDetails().getCategory();
            childCategory = titleCase(String.valueOf(details.getProductVariant().getVariantType()));
        }

        return "Service: " + category + " (" + childCategory + ")" +
                "\nCountry of Origin: " + country.getCountryName() +
                "\nBiller: " + details.getProduct().getProductName() +
                "\nDeno Amount: "
                + (details.getDenoAmount() != null ? country.getCurrencySymbol() + formatAmount(details.getDenoAmount())
                        : "N/A")
                +
                "\nAccount No.: " + (details.getAccountNo() != null ? details.getAccountNo() : "N/A") +
                "\nTransaction Status: " + details.getStatus();
    }

    private static String titleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder titleCase = new StringBuilder(input.length());
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }

    private static String formatAmount(Double amount) {
        DecimalFormat formatter = new DecimalFormat("#0.00");
        return formatter.format(amount);
    }

    public static void addWatermark(PDDocument document, String imageAssetPath) throws IOException {
        for (PDPage page : document.getPages()) {
            PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, false);

            // Watermark
            PDImageXObject imageXObject = PDImageXObject.createFromFile(imageAssetPath + "/GF_Logo_Watermark.png",
                    document);
            float scale = 0.50f; // Adjust this value to scale the image
            float imageWidth = imageXObject.getWidth() * scale;
            float imageHeight = imageXObject.getHeight() * scale;
            PDRectangle pageSize = page.getCropBox();
            float x = (pageSize.getUpperRightX() - imageWidth) / 2;
            float y = (pageSize.getUpperRightY() / 2) - imageHeight;

            contentStream.drawImage(imageXObject, x, y, imageWidth, imageHeight);
            contentStream.close();
        }
    }

}
