package com.kalsym.internationalPayment.services;


import com.kalsym.internationalPayment.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kalsym.internationalPayment.helper.CSVHelper;
import com.kalsym.internationalPayment.model.ProductVariant;
import com.kalsym.internationalPayment.repositories.ProductVariantRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

@Service
public class CSVService {

    @Autowired
    ProductVariantRepository productVariantRepository;

    @Autowired
    CSVHelper csvHelper;

    public List<ProductVariant> convertToList(MultipartFile file, String type) {
        try {
            if (type.equalsIgnoreCase("voucher")) {
                return csvHelper.csvToProductVariantVoucher(file.getInputStream());
            } else {
                return csvHelper.csvToProductVariant(file.getInputStream());
            }

            // productVariantRepository.saveAll(tutorials);
        } catch (IOException e) {
            throw new RuntimeException("fail to store excel data: " + e.getMessage());
        }
    }

    public List<ProductVariant> convertToListToUpdate(MultipartFile file, String type) {
        try {
            List<ProductVariant> tutorials;
            if (type.equalsIgnoreCase("voucher")) {
                tutorials = csvHelper.csvToUpdateProductVariantVoucher(file.getInputStream());
            } else {
                tutorials = csvHelper.csvToUpdateProductVariant(file.getInputStream());
            }
            return tutorials;
            // productVariantRepository.saveAll(tutorials);
        } catch (IOException e) {
            throw new RuntimeException("fail to store excel data: " + e.getMessage());
        }
    }

    public List<ProductVariant> getAllTutorials() {
        return productVariantRepository.findAll();
    }

    public ByteArrayInputStream productsToCSV(List<Product> products) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PrintWriter writer = new PrintWriter(out);

        // CSV Header
        writer.println("Product Name,Country Code,Variant ID,Variant Name,Variant Type,Variant Price,Variant Deno");

        // CSV Rows
        for (Product product : products) {
            if (product.getProductVariant() != null && !product.getProductVariant().isEmpty()) {
                // Iterate over each product variant
                for (ProductVariant variant : product.getProductVariant()) {

                    writer.println(String.format("%s,%s,%d,\"%s\",%s,%.2f,%.2f,%s,%s",
                            product.getProductName(),
                            product.getCountryCode(),
                            variant.getId(),
                            variant.getVariantName(),  // Wrap in quotes to handle commas
                            variant.getVariantType(),
                            Optional.ofNullable(variant.getPrice()).orElse(0.00),  // Handle null value for price
                            Optional.ofNullable(variant.getDeno()).orElse(0.00)   // Handle null value for deno
                    ));
                }
            }
        }

        writer.flush();
        return new ByteArrayInputStream(out.toByteArray());
    }
}