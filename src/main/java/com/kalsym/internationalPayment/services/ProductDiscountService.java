package com.kalsym.internationalPayment.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kalsym.internationalPayment.model.ProductDiscount;
import com.kalsym.internationalPayment.repositories.ProductDiscountRepository;

@Service
public class ProductDiscountService {
    
    @Autowired
    ProductDiscountRepository productDiscountRepository;

    public ProductDiscount createProductDiscount(ProductDiscount productDiscountBody){
        return productDiscountRepository.save(productDiscountBody);

    }

    public ProductDiscount updateProductDiscount(Integer id, ProductDiscount productDiscountBody){

        ProductDiscount data = productDiscountRepository.findById(id).get();
        data.updateData(productDiscountBody);

        return productDiscountRepository.save(data);                                
    }
}
