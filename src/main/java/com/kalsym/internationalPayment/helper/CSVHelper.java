package com.kalsym.internationalPayment.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kalsym.internationalPayment.model.ProductVariant;
import com.kalsym.internationalPayment.model.VariantCategory;
import com.kalsym.internationalPayment.model.enums.Status;
import com.kalsym.internationalPayment.model.enums.VariantType;
import com.kalsym.internationalPayment.repositories.VariantCategoryRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
// import java.util.Arrays;
import java.util.List;

@Service
public class CSVHelper {

    @Autowired
    private VariantCategoryRepository variantCategoryRepository;

    public static String TYPE = "text/csv";
    static String[] NEWHEADERs = { "WSP Product Code", "Variant Name", "Variant Type", "Price", "Deno",
            "Status", "OzoPay Method", "HelloSIM Method", "MM Wallet Method"};
    static String[] UPDATEDHEADERs = { "Variant Id", "WSP Product Code", "Variant Name", "Variant Type", "Price",
            "Deno", "Status", "OzoPay Method", "HelloSIM Method", "MM Wallet Method" };

    static String[] VOUCHERNEWHEADERs = { "WSP Product Code", "Variant Name", "Category", "Variant Type", "Price",
            "Deno",
            "Status", "OzoPay Method", "HelloSIM Method", "MM Wallet Method" };
    static String[] VOUCHERUPDATEDHEADERs = { "Variant Id", "WSP Product Code", "Variant Name", "Category",
            "Variant Type", "Price",
            "Deno", "Status", "OzoPay Method", "HelloSIM Method", "MM Wallet Method" };

    static String SHEET = "Book1";

    public static boolean hasCSVFormat(MultipartFile file) {
        return TYPE.equals(file.getContentType());
    }

    private static boolean parseBooleanValue(String value) {
        if ("1".equals(value) || "true".equalsIgnoreCase(value)) {
            return true;
        } else if ("0".equals(value) || "false".equalsIgnoreCase(value)) {
            return false;
        }

        throw new IllegalArgumentException("Invalid boolean value: " + value);
    }

    public List<ProductVariant> csvToProductVariant(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        List<ProductVariant> productVariants = new ArrayList<>();
        String line;
        int rowNumber = 0;

        while ((line = reader.readLine()) != null) {
            String[] values = line.split(",(?![^()]*\\))"); // Splitting by comma, but excluding commas inside
                                                            // parentheses

            if (rowNumber == 0) {

                if (values.length < NEWHEADERs.length) {
                    throw new IllegalArgumentException(
                            "Row " + (rowNumber + 1) + " has fewer columns than expected. Expected: "
                                    + NEWHEADERs.length + ", Found: " + values.length);
                }

                // Validate headers here
                for (int i = 0; i < NEWHEADERs.length; i++) {
                    if (!NEWHEADERs[i].equalsIgnoreCase(values[i].trim())) {
                        throw new IllegalArgumentException("\nInvalid header found. \nExpected: " + NEWHEADERs[i]
                                + "\nActual: " + values[i]);
                    }
                }
                rowNumber++;
                continue;
            }

            ProductVariant productVariant = new ProductVariant();

            try {
                for (int cellIdx = 0; cellIdx < values.length; cellIdx++) {
                    String currentValue = values[cellIdx].trim();
                    currentValue = currentValue.replaceAll("^\"|\"$", ""); // Remove double quotes if they're at the
                                                                           // start or end

                    switch (cellIdx) {
                        case 0:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("WSP Product Code Field Is Missing.");
                            }
                            productVariant.setWspProductCode(currentValue);
                            break;

                        case 1:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Variant Name Field Is Missing.");
                            }
                            productVariant.setVariantName(currentValue);
                            break;

                        case 2:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Variant Type Field Is Missing.");
                            }

                            boolean isValidVariantType = false;
                            StringBuilder validValues = new StringBuilder();
                            for (VariantType variantType : VariantType.values()) {
                                if (variantType.name().equals(currentValue.toUpperCase())) {
                                    isValidVariantType = true;
                                    productVariant.setVariantType(variantType);
                                    break;
                                }
                                if (!variantType.name().equals("PREPAID") || !variantType.name().equals("GAMES")
                                        || !variantType.name().equals("ENTERTAINMENT"))
                                    validValues.append(variantType.name()).append(", ");
                            }

                            if (!isValidVariantType) {
                                int lastIndex = validValues.length() - 2;
                                String validValuesString = validValues.substring(0, lastIndex);
                                throw new IllegalArgumentException(
                                        "Invalid Variant Type '" + currentValue + "'. Valid Values: "
                                                + validValuesString);
                            }
                            break;

                        case 3:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Price Field Is Missing.");
                            }
                            productVariant.setPrice(Double.parseDouble(currentValue));
                            break;

                        case 4:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Deno Field Is Missing.");
                            }
                            productVariant.setDeno(Double.parseDouble(currentValue));
                            break;

                        case 5:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Status Field Is Missing.");
                            }

                            boolean isValidStatus = false;
                            StringBuilder validStatusValues = new StringBuilder();
                            for (Status status : Status.values()) {
                                if (status.name().equals(currentValue.toUpperCase())) {
                                    isValidStatus = true;
                                    productVariant.setStatus(status);
                                    break;
                                }
                                validStatusValues.append(status.name()).append(", ");
                            }

                            if (!isValidStatus) {
                                int lastIndex = validStatusValues.length() - 2;
                                String validValuesString = validStatusValues.substring(0, lastIndex);
                                throw new IllegalArgumentException(
                                        "Invalid Status '" + currentValue + "'. Valid Values: " + validValuesString);
                            }
                            break;

                        case 6:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("OzoPay Method Field Is Missing.");
                            }
                            productVariant.setOzoPayMethod(parseBooleanValue(currentValue));
                            break;

                        case 7:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Hellosim Method Field Is Missing.");
                            }
                            productVariant.setHellosimMethod(parseBooleanValue(currentValue));
                            break;

                        case 8:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("MMWallet Method Field Is Missing.");
                            }
                            productVariant.setMmWalletMethod(parseBooleanValue(currentValue));
                            break;
                      
                        default:
                            break;
                    }
                }

            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Error processing field at row " + (rowNumber + 1)
                        + ". Cause: " + ex.getMessage());
            }

            productVariants.add(productVariant);
            rowNumber++;
        }

        return productVariants;
    }

    public List<ProductVariant> csvToUpdateProductVariant(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        List<ProductVariant> productVariants = new ArrayList<>();
        String line;
        int rowNumber = 0;

        while ((line = reader.readLine()) != null) {
            String[] values = line.split(",(?![^()]*\\))"); // Splitting by comma, but excluding commas inside
                                                            // parentheses

            if (rowNumber == 0) {
                // Validate headers here
                for (int i = 0; i < UPDATEDHEADERs.length; i++) {

                    if (!UPDATEDHEADERs[i].equalsIgnoreCase(values[i].trim())) {
                        throw new IllegalArgumentException("\nInvalid header found. \nExpected: " + UPDATEDHEADERs[i]
                                + "\nActual: " + values[i]);
                    }
                }
                rowNumber++;
                continue;
            }

            ProductVariant productVariant = new ProductVariant();

            try {
                for (int cellIdx = 0; cellIdx < values.length; cellIdx++) {
                    String currentValue = values[cellIdx].trim();
                    currentValue = currentValue.replaceAll("^\"|\"$", ""); // Remove double quotes if they're at the
                                                                           // start or end

                    switch (cellIdx) {
                        case 0:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Variant Id Field Is Missing.");
                            }
                            Integer id = Integer.parseInt(currentValue);
                            productVariant.setId(id);
                            break;

                        case 1:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("WSP Product Code Field Is Missing.");
                            }
                            productVariant.setWspProductCode(currentValue);
                            break;

                        case 2:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Variant Name Field Is Missing.");
                            }
                            productVariant.setVariantName(currentValue);
                            break;

                        case 3:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Variant Type Field Is Missing.");
                            }

                            boolean isValidVariantType = false;
                            StringBuilder validValues = new StringBuilder();
                            for (VariantType variantType : VariantType.values()) {
                                if (variantType.name().equals(currentValue.toUpperCase())) {
                                    isValidVariantType = true;
                                    productVariant.setVariantType(variantType);
                                    break;
                                }
                                if (!variantType.name().equals("PREPAID") || !variantType.name().equals("GAMES")
                                        || !variantType.name().equals("ENTERTAINMENT"))
                                    validValues.append(variantType.name()).append(", ");
                            }

                            if (!isValidVariantType) {
                                int lastIndex = validValues.length() - 2; // Index of the last comma
                                String validValuesString = validValues.substring(0, lastIndex);
                                throw new IllegalArgumentException(
                                        "Invalid Variant Type '" + currentValue + "' \nValid Values: "
                                                + validValuesString);
                            }
                            break;

                        case 4:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Price Field Is Missing.");
                            }
                            double priceValue = Double.parseDouble(currentValue);
                            productVariant.setPrice(priceValue);
                            break;

                        case 5:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Deno Field Is Missing.");
                            }
                            double denoValue = Double.parseDouble(currentValue);
                            productVariant.setDeno(denoValue);
                            break;

                        case 6:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Status Field Is Missing.");
                            }
                            boolean isValidStatus = false;
                            StringBuilder validStatusValues = new StringBuilder();
                            for (Status status : Status.values()) {
                                if (status.name().equals(currentValue.toUpperCase())) {
                                    isValidStatus = true;
                                    productVariant.setStatus(status);
                                    break;
                                }
                                validStatusValues.append(status.name()).append(", ");
                            }

                            if (!isValidStatus) {
                                int lastIndex = validStatusValues.length() - 2; // Index of the last comma
                                String validValuesString = validStatusValues.substring(0, lastIndex);
                                throw new IllegalArgumentException(
                                        "Invalid Status Type '" + currentValue + "' \nValid Values: "
                                                + validValuesString);
                            }
                            break;

                        case 7:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("OzoPay Method Field Is Missing.");
                            }
                            productVariant.setOzoPayMethod(parseBooleanValue(currentValue));
                            break;

                        case 8:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Hellosim Method Field Is Missing.");
                            }
                            productVariant.setHellosimMethod(parseBooleanValue(currentValue));
                            break;

                        case 9:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("MMWallet Method Field Is Missing.");
                            }
                            productVariant.setMmWalletMethod(parseBooleanValue(currentValue));
                            break;

                        default:
                            break;
                    }

                }

            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Error processing field at row " + (rowNumber + 1)
                        + ". Cause: " + ex.getMessage());
            }

            productVariants.add(productVariant);
            rowNumber++;
        }

        return productVariants;
    }

    public List<ProductVariant> csvToProductVariantVoucher(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        List<ProductVariant> productVariants = new ArrayList<>();
        String line;
        int rowNumber = 0;

        while ((line = reader.readLine()) != null) {
            String[] values = line.split(",(?![^()]*\\))"); // Splitting by comma, but excluding commas inside
                                                            // parentheses

            if (rowNumber == 0) {
                // Validate headers here
                for (int i = 0; i < VOUCHERNEWHEADERs.length; i++) {
                    if (!VOUCHERNEWHEADERs[i].equalsIgnoreCase(values[i].trim())) {
                        throw new IllegalArgumentException("\nInvalid header found. \nExpected: " + VOUCHERNEWHEADERs[i]
                                + "\nActual: " + values[i]);
                    }
                }
                rowNumber++;
                continue;
            }

            ProductVariant productVariant = new ProductVariant();

            try {
                for (int cellIdx = 0; cellIdx < values.length; cellIdx++) {
                    String currentValue = values[cellIdx].trim();
                    currentValue = currentValue.replaceAll("^\"|\"$", ""); // Remove double quotes if they're at the
                                                                           // start or end

                    switch (cellIdx) {
                        case 0:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("WSP Product Code Field Is Missing.");
                            }
                            productVariant.setWspProductCode(currentValue);
                            break;

                        case 1:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Variant Name Field Is Missing.");
                            }
                            productVariant.setVariantName(currentValue);
                            break;

                        case 2:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Category Field Is Missing.");
                            }
                            boolean isValidVCategory = false;
                            StringBuilder validCategoriesValues = new StringBuilder();
                            List<VariantCategory> variantCategories = variantCategoryRepository.findAll();
                            for (VariantCategory variantCategory : variantCategories) {
                                if (variantCategory.getName().equals(currentValue.toUpperCase())) {
                                    isValidVCategory = true;
                                    productVariant.setCategory(variantCategory.getName());
                                    break;
                                }
                                validCategoriesValues.append(variantCategory.getName()).append(", ");
                            }

                            if (!isValidVCategory) {
                                int lastIndex = validCategoriesValues.length() - 2; // Index of the last comma
                                String validCategoriesValuesString = validCategoriesValues.substring(0, lastIndex);
                                throw new IllegalArgumentException(
                                        "Invalid Variant Type '" + currentValue + "' \nValid Values: "
                                                + validCategoriesValuesString);
                            }
                            break;

                        case 3:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Variant Type Field Is Missing.");
                            }

                            boolean isValidVariantType = false;
                            StringBuilder validValues = new StringBuilder();
                            for (VariantType variantType : VariantType.values()) {
                                if (variantType.name().equals(currentValue.toUpperCase())) {
                                    isValidVariantType = true;
                                    productVariant.setVariantType(variantType);
                                    break;
                                }
                                if (!variantType.name().equals("PREPAID") || !variantType.name().equals("GAMES")
                                        || !variantType.name().equals("ENTERTAINMENT"))
                                    validValues.append(variantType.name()).append(", ");
                            }

                            if (!isValidVariantType) {
                                int lastIndex = validValues.length() - 2;
                                String validValuesString = validValues.substring(0, lastIndex);
                                throw new IllegalArgumentException(
                                        "Invalid Variant Type '" + currentValue + "'. Valid Values: "
                                                + validValuesString);
                            }
                            break;

                        case 4:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Price Field Is Missing.");
                            }
                            productVariant.setPrice(Double.parseDouble(currentValue));
                            break;

                        case 5:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Deno Field Is Missing.");
                            }
                            productVariant.setDeno(Double.parseDouble(currentValue));
                            break;

                        case 6:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Status Field Is Missing.");
                            }

                            boolean isValidStatus = false;
                            StringBuilder validStatusValues = new StringBuilder();
                            for (Status status : Status.values()) {
                                if (status.name().equals(currentValue.toUpperCase())) {
                                    isValidStatus = true;
                                    productVariant.setStatus(status);
                                    break;
                                }
                                validStatusValues.append(status.name()).append(", ");
                            }

                            if (!isValidStatus) {
                                int lastIndex = validStatusValues.length() - 2;
                                String validValuesString = validStatusValues.substring(0, lastIndex);
                                throw new IllegalArgumentException(
                                        "Invalid Status '" + currentValue + "'. Valid Values: " + validValuesString);
                            }
                            break;

                        case 7:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("OzoPay Method Field Is Missing.");
                            }
                            productVariant.setOzoPayMethod(parseBooleanValue(currentValue));
                            break;

                        case 8:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Hellosim Method Field Is Missing.");
                            }
                            productVariant.setHellosimMethod(parseBooleanValue(currentValue));
                            break;

                        case 9:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("MMWallet Method Field Is Missing.");
                            }
                            productVariant.setMmWalletMethod(parseBooleanValue(currentValue));
                            break;

                        default:
                            break;
                    }
                }

            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Error processing field at row " + (rowNumber + 1)
                        + ". Cause: " + ex.getMessage());
            }

            productVariants.add(productVariant);
            rowNumber++;
        }

        return productVariants;
    }

    public List<ProductVariant> csvToUpdateProductVariantVoucher(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        List<ProductVariant> productVariants = new ArrayList<>();
        String line;
        int rowNumber = 0;

        while ((line = reader.readLine()) != null) {
            String[] values = line.split(",(?![^()]*\\))"); // Splitting by comma, but excluding commas inside
                                                            // parentheses

            if (rowNumber == 0) {
                // Validate headers here
                for (int i = 0; i < VOUCHERUPDATEDHEADERs.length; i++) {
                    if (!VOUCHERUPDATEDHEADERs[i].equalsIgnoreCase(values[i].trim())) {
                        throw new IllegalArgumentException(
                                "\nInvalid header found. \nExpected: " + VOUCHERUPDATEDHEADERs[i]
                                        + "\nActual: " + values[i]);
                    }
                }
                rowNumber++;
                continue;
            }

            ProductVariant productVariant = new ProductVariant();

            try {
                for (int cellIdx = 0; cellIdx < values.length; cellIdx++) {
                    String currentValue = values[cellIdx].trim();
                    currentValue = currentValue.replaceAll("^\"|\"$", ""); // Remove double quotes if they're at the
                                                                           // start or end

                    switch (cellIdx) {
                        case 0:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Variant Id Field Is Missing.");
                            }
                            Integer id = Integer.parseInt(currentValue);
                            productVariant.setId(id);
                            break;

                        case 1:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("WSP Product Code Field Is Missing.");
                            }
                            productVariant.setWspProductCode(currentValue);
                            break;

                        case 2:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Variant Name Field Is Missing.");
                            }
                            productVariant.setVariantName(currentValue);
                            break;

                        case 3:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Category Field Is Missing.");
                            }
                            boolean isValidVCategory = false;
                            StringBuilder validCategoriesValues = new StringBuilder();
                            List<VariantCategory> variantCategories = variantCategoryRepository.findAll();
                            for (VariantCategory variantCategory : variantCategories) {
                                if (variantCategory.getName().equals(currentValue.toUpperCase())) {
                                    isValidVCategory = true;
                                    productVariant.setCategory(variantCategory.getName());
                                    break;
                                }
                                validCategoriesValues.append(variantCategory.getName()).append(", ");
                            }

                            if (!isValidVCategory) {
                                int lastIndex = validCategoriesValues.length() - 2; // Index of the last comma
                                String validValuesString = validCategoriesValues.substring(0, lastIndex);
                                throw new IllegalArgumentException(
                                        "Invalid Variant Type '" + currentValue + "' \nValid Values: "
                                                + validValuesString);
                            }

                            break;

                        case 4:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Variant Type Field Is Missing.");
                            }

                            boolean isValidVariantType = false;
                            StringBuilder validValues = new StringBuilder();
                            for (VariantType variantType : VariantType.values()) {
                                if (variantType.name().equals(currentValue.toUpperCase())) {
                                    isValidVariantType = true;
                                    productVariant.setVariantType(variantType);
                                    break;
                                }
                                if (!variantType.name().equals("PREPAID") || !variantType.name().equals("GAMES")
                                        || !variantType.name().equals("ENTERTAINMENT"))
                                    validValues.append(variantType.name()).append(", ");
                            }

                            if (!isValidVariantType) {
                                int lastIndex = validValues.length() - 2; // Index of the last comma
                                String validValuesString = validValues.substring(0, lastIndex);
                                throw new IllegalArgumentException(
                                        "Invalid Variant Type '" + currentValue + "' \nValid Values: "
                                                + validValuesString);
                            }
                            break;

                        case 5:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Price Field Is Missing.");
                            }
                            double priceValue = Double.parseDouble(currentValue);
                            productVariant.setPrice(priceValue);
                            break;

                        case 6:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Deno Field Is Missing.");
                            }
                            double denoValue = Double.parseDouble(currentValue);
                            productVariant.setDeno(denoValue);
                            break;

                        case 7:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Status Field Is Missing.");
                            }
                            boolean isValidStatus = false;
                            StringBuilder validStatusValues = new StringBuilder();
                            for (Status status : Status.values()) {
                                if (status.name().equals(currentValue.toUpperCase())) {
                                    isValidStatus = true;
                                    productVariant.setStatus(status);
                                    break;
                                }
                                validStatusValues.append(status.name()).append(", ");
                            }

                            if (!isValidStatus) {
                                int lastIndex = validStatusValues.length() - 2; // Index of the last comma
                                String validValuesString = validStatusValues.substring(0, lastIndex);
                                throw new IllegalArgumentException(
                                        "Invalid Status Type '" + currentValue + "' \nValid Values: "
                                                + validValuesString);
                            }
                            break;

                        case 8:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("OzoPay Method Field Is Missing.");
                            }
                            productVariant.setOzoPayMethod(parseBooleanValue(currentValue));
                            break;

                        case 9:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("Hellosim Method Field Is Missing.");
                            }
                            productVariant.setHellosimMethod(parseBooleanValue(currentValue));
                            break;

                        case 10:
                            if (currentValue.isEmpty()) {
                                throw new IllegalArgumentException("MMWallet Method Field Is Missing.");
                            }
                            productVariant.setMmWalletMethod(parseBooleanValue(currentValue));
                            break;

                        default:
                            break;
                    }
                }

            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Error processing field at row " + (rowNumber + 1)
                        + ". Cause: " + ex.getMessage());
            }

            productVariants.add(productVariant);
            rowNumber++;
        }

        return productVariants;
    }

}
