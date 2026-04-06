package com.kalsym.internationalPayment.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.ProductDiscount;

@Repository
public interface ProductDiscountRepository extends JpaRepository<ProductDiscount,Integer> {
    
    @Query(value = "CALL getProductDiscount(:productVariantId);", nativeQuery = true)
    List<Object[]> getProductDiscount(@Param("productVariantId") Integer productVariantId);

    
}
