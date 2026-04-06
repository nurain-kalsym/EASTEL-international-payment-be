package com.kalsym.internationalPayment.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.ProductRequiredInfo;


@Repository
public interface ProductRequiredInfoRepository extends JpaRepository<ProductRequiredInfo,Integer>, JpaSpecificationExecutor<ProductRequiredInfo> {
    
    List<ProductRequiredInfo> findByProductCode(@Param("productCode") String productCode);

}
