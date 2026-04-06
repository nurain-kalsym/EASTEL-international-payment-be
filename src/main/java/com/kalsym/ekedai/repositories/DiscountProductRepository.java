package com.kalsym.ekedai.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.DiscountProduct;

@Repository
public interface DiscountProductRepository extends JpaRepository<DiscountProduct,String>, JpaSpecificationExecutor<DiscountProduct> {

    Optional<DiscountProduct> findById(@Param("id") String id);

    List<DiscountProduct> getDiscountProductByDiscountId(@Param("discountId") String discountId);

    List<DiscountProduct> getDiscountProductByProductCode(@Param("productCode") String productCode);

    Specification<DiscountProduct> getDiscountProductByDiscountIdAndProductCode(String discountId, String productCode);
}