package com.kalsym.ekedai.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kalsym.ekedai.model.ProductRequiredInfo;
import com.kalsym.ekedai.repositories.ProductRequiredInfoRepository;

@Service

public class ProductRequiredInfoService {
   
    @Autowired
    ProductRequiredInfoRepository productRequiredInfoRepository;

    public List<ProductRequiredInfo> getProductRequiredInfosByProductCode(String productCode){

        return productRequiredInfoRepository.findByProductCode(productCode);
        
    }

    public ProductRequiredInfo createProductRequiredInfo(ProductRequiredInfo body){

        return productRequiredInfoRepository.save(body);
    }

    public ProductRequiredInfo updateProductRequiredInfo(Integer id, ProductRequiredInfo body){

        ProductRequiredInfo data = productRequiredInfoRepository.findById(id).get();
        data.updateData(body);

        return productRequiredInfoRepository.save(data);                                
    }

    public void deleteProductRequiredInfo(Integer id){
        Optional<ProductRequiredInfo> data = productRequiredInfoRepository.findById(id);

        if (data.isPresent()){
            productRequiredInfoRepository.deleteById(id);
        } 

    }
    
}
