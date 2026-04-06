package com.kalsym.internationalPayment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.DiscountWithDetails;

@Repository
public interface DiscountWithDetailsRepository extends JpaRepository<DiscountWithDetails,String>, JpaSpecificationExecutor<DiscountWithDetails> {
}
