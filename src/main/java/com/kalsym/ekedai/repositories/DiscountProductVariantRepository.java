package com.kalsym.ekedai.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.DiscountProductVariant;

@Repository
public interface DiscountProductVariantRepository extends JpaRepository<DiscountProductVariant,String>, JpaSpecificationExecutor<DiscountProductVariant> {

    List<DiscountProductVariant> getDiscountProductByDiscountId(@Param("discountId") String discountId);

    List<DiscountProductVariant> getDiscountProductByProductVariantId(@Param("productVariantId") Integer productVariantId);

    Optional<DiscountProductVariant> findByDiscountIdAndProductVariantId(String discountId, Integer productVariantId);
}