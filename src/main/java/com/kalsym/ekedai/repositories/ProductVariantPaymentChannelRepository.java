package com.kalsym.ekedai.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.ProductVariantPaymentChannel;
import com.kalsym.ekedai.model.dao.ProductVariantPaymentId;

@Repository
public interface ProductVariantPaymentChannelRepository
                extends JpaRepository<ProductVariantPaymentChannel, ProductVariantPaymentId> {

        // Custom query to find all records by productVariantId
        @Query("SELECT p FROM ProductVariantPaymentChannel p WHERE p.enabled = :enabled AND p.id.productVariantId = :productVariantId")
        List<ProductVariantPaymentChannel> findByEnabledAndProductVariantId(@Param("enabled") Boolean enabled,
                        @Param("productVariantId") Long productVariantId);
}
