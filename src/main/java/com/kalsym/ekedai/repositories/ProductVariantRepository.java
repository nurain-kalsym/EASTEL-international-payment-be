package com.kalsym.ekedai.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    Optional<ProductVariant> findByWspProductCode(String wspProductCode);

    Optional<ProductVariant> findByVariantName(@Param("variantName") String variantName);

}
