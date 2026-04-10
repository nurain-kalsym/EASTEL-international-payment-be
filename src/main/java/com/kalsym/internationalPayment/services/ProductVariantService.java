package com.kalsym.internationalPayment.services;

import java.util.*;

import com.kalsym.internationalPayment.model.Product;
import com.kalsym.internationalPayment.model.ProductVariant;
import com.kalsym.internationalPayment.model.enums.Status;
import com.kalsym.internationalPayment.model.enums.VariantType;
import com.kalsym.internationalPayment.repositories.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service

public class ProductVariantService {

    @Autowired
    ProductVariantRepository productVariantRepository;

    public ProductVariant createProductVariant(ProductVariant productVariantBody) {
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

}
