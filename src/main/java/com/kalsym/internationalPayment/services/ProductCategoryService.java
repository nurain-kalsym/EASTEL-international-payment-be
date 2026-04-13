package com.kalsym.internationalPayment.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatcher;
import org.springframework.stereotype.Service;

import com.kalsym.internationalPayment.model.ProductCategory;
import com.kalsym.internationalPayment.repositories.ProductCategoryRepository;

@Service
public class ProductCategoryService {

    @Autowired
    ProductCategoryRepository productCategoryRepository;

    public ProductCategory updateProductCategory(Integer id, ProductCategory productCategoryBody) {

        ProductCategory data = productCategoryRepository.findById(id).get();
        data.updateData(productCategoryBody);

        return productCategoryRepository.save(data);
    }

    public ProductCategory createProductCategory(ProductCategory productCategoryBody) {

        return productCategoryRepository.save(productCategoryBody);
    }

    public Optional<ProductCategory> getProductCategoryId(Integer id) {
        return productCategoryRepository.findById(id);
    }

    public List<ProductCategory> getAllCategories() {
        List<ProductCategory> data = productCategoryRepository.findAllActiveCategory();
        return data;
    }
}
