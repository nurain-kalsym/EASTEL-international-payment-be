package com.kalsym.internationalPayment.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.Product;
import com.kalsym.internationalPayment.model.ProductVariant;
import com.kalsym.internationalPayment.model.enums.Status;
import com.kalsym.internationalPayment.model.enums.VariantType;
import com.kalsym.internationalPayment.repositories.ProductVariantRepository;
import com.kalsym.internationalPayment.utility.Logger;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service

public class ProductVariantService {

    @Autowired
    ProductVariantRepository productVariantRepository;

    public ProductVariant createProductVariant(ProductVariant productVariantBody) {

        // TODO: Generate description
        return productVariantRepository.save(productVariantBody);
    }

    public ProductVariant updateProductVariant(Integer id, ProductVariant productVariantBody) {

        ProductVariant data = productVariantRepository.findById(id).get();
        data.updateData(productVariantBody);

        return productVariantRepository.save(data);
    }

    public Boolean deleteProductVariant(Integer id) {
        Optional<ProductVariant> productV = productVariantRepository.findById(id);

        if (productV.isPresent()) {
            productVariantRepository.deleteById(id);
            return true;

        } else {
            return false;
        }

    }

    public Optional<ProductVariant> getProductVariantById(Integer id) {
        return productVariantRepository.findById(id);
    }

    public Optional<ProductVariant> getProductVariantByVariantName(String variantName) {
        return productVariantRepository.findByVariantName(variantName);
    }

    // create or update product variant by admin
    public ProductVariant createUpdateProductVariant(
            String variantName,
            Double price,
            VariantType variantType,
            String wspProductCode,
            Product resultProduct,
            Double deno) {

        // Retrieve the existing product from the database by ID
        Optional<ProductVariant> existingVariant = productVariantRepository.findByWspProductCode(wspProductCode);

        if (existingVariant.isPresent()) {
            ProductVariant existingProductVariant = existingVariant.get();
            if (variantName != null && !"".equals(variantName))
                existingProductVariant.setVariantName(variantName);
            if (price != null)
                existingProductVariant.setPrice(price);
            if (deno != null)
                existingProductVariant.setDeno(deno);
            if (variantType != null)
                existingProductVariant.setVariantType(variantType);
            if (wspProductCode != null && !"".equals(wspProductCode))
                existingProductVariant.setWspProductCode(wspProductCode);
            // Save the updated product using the repository
            return productVariantRepository.save(existingProductVariant);
        } else {
            // Create a new Product instance
            ProductVariant newProductVariant = new ProductVariant();
            newProductVariant.setProductId(resultProduct.getId());
            newProductVariant.setVariantName(variantName);
            newProductVariant.setPrice(price);
            newProductVariant.setDeno(deno);
            newProductVariant.setVariantType(variantType);
            newProductVariant.setWspProductCode(wspProductCode);
            newProductVariant.setStatus(Status.INACTIVE);
            newProductVariant.setOzoPayMethod(Boolean.FALSE);
            newProductVariant.setHellosimMethod(Boolean.FALSE);
            newProductVariant.setMmWalletMethod(Boolean.FALSE);
            // Save the new product using the repository
            return productVariantRepository.save(newProductVariant);
        }
    }

    @Transactional
    public List<String> updateServiceIdsFromCSVOld(InputStream csvInputStream) throws Exception {
        List<String> skippedLines = new ArrayList<>();
        Map<Integer, String> variantUpdates = new HashMap<>();
        List<Integer> variantIds = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(csvInputStream))) {
            String line;
            int rowNumber = 1; // Initialize line counter

            // Skip the header
            bufferedReader.readLine();

            // Iterate over CSV rows
            while ((line = bufferedReader.readLine()) != null) {
                rowNumber++; // Increment line counter

                // Split CSV line by commas
                String[] values = line.split(",(?![^()]*\\))"); // Splitting by comma, but excluding commas inside
                                                                // parentheses

                // Check if there are enough columns to process
                if (values.length < 9) {
                    skippedLines.add(String.format("Skipping row %s due to insufficient columns", rowNumber));
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, "updateServiceIdsFromCSV",
                            "Skipping row due to insufficient columns: {}", line);
                    continue; // Skip this row
                }
                // CSV structure:
                // values[0] -> Variant ID
                // values[7] -> Loyalty Service ID
                // values[8] -> Referral Service ID

                Integer variantId = Integer.parseInt(values[2].trim()); // Variant ID from CSV
                String loyaltyServiceId = values[7].trim(); // Loyalty Service ID
                String referralServiceId = values[8].trim(); // Referral Service ID

                // Check for null, empty, or "null" string for both service IDs
                boolean isLoyaltyInvalid = isNullOrEmptyOrInvalid(loyaltyServiceId);
                boolean isReferralInvalid = isNullOrEmptyOrInvalid(referralServiceId);

                String formattedServiceId = null;

                if (!isLoyaltyInvalid || !isReferralInvalid) {
                    String formattedLoyalty = isLoyaltyInvalid ? "null" : loyaltyServiceId;
                    String formattedReferral = isReferralInvalid ? "null" : referralServiceId;
                    formattedServiceId = String.format("ref:%s;loy:%s;", formattedReferral, formattedLoyalty);
                }

                variantIds.add(variantId);
                variantUpdates.put(variantId, formattedServiceId);
            }

            // Fetch all variants by IDs in a single query
            List<ProductVariant> variants = productVariantRepository.findAllById(variantIds);

            // Update service IDs for the fetched variants
            for (ProductVariant variant : variants) {
                String serviceId = variantUpdates.get(variant.getId());
                variant.setServiceId(serviceId);
            }
            // Save all
            productVariantRepository.saveAll(variants);
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, "updateServiceIdsFromCSV",
                    "Exception " + e.getMessage());
            throw new Exception("Error while processing the CSV", e);
        }
        return skippedLines;
    }

    @Transactional
    public List<String> updateServiceIdsFromCSV(InputStream csvInputStream) throws Exception {
        List<String> skippedLines = new ArrayList<>();
        Map<Integer, String> variantUpdates = new HashMap<>();
        List<Integer> variantIds = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(csvInputStream))) {
            String[] values;
            int rowNumber = 0;

            // Skip the header
            csvReader.readNext();

            // Iterate over CSV rows
            while ((values = csvReader.readNext()) != null) {
                rowNumber++; // Increment row counter

                // Check if there are enough columns to process
                if (values.length < 9) {
                    skippedLines.add(String.format("Skipping row %s due to insufficient columns", rowNumber));
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, "updateServiceIdsFromCSV",
                            "Skipping row due to insufficient columns: {}", (Object) values);
                    continue; // Skip this row
                }

                Integer variantId = Integer.parseInt(values[2].trim()); // Variant ID from CSV
                String loyaltyServiceId = values[7].trim(); // Loyalty Service ID
                String referralServiceId = values[8].trim(); // Referral Service ID

                // Check for null, empty, or "null" string for both service IDs
                boolean isLoyaltyInvalid = isNullOrEmptyOrInvalid(loyaltyServiceId);
                boolean isReferralInvalid = isNullOrEmptyOrInvalid(referralServiceId);

                String formattedServiceId = null;

                if (!isLoyaltyInvalid || !isReferralInvalid) {
                    String formattedLoyalty = isLoyaltyInvalid ? "null" : loyaltyServiceId;
                    String formattedReferral = isReferralInvalid ? "null" : referralServiceId;
                    formattedServiceId = String.format("ref:%s;loy:%s;", formattedReferral, formattedLoyalty);
                }

                variantIds.add(variantId);
                variantUpdates.put(variantId, formattedServiceId);
            }

            // Fetch all variants by IDs in a single query
            List<ProductVariant> variants = productVariantRepository.findAllById(variantIds);

            // Update service IDs for the fetched variants
            for (ProductVariant variant : variants) {
                String serviceId = variantUpdates.get(variant.getId());
                variant.setServiceId(serviceId);
            }
            // Save all
            productVariantRepository.saveAll(variants);
        } catch (IOException | CsvException e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, "updateServiceIdsFromCSV",
                    "Exception " + e.getMessage());
            throw new Exception("Error while processing the CSV", e);
        }
        return skippedLines;
    }

    private boolean isNullOrEmptyOrInvalid(String value) {
        return value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null");
    }
}
