package com.kalsym.internationalPayment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.Commission;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Integer> {
    Commission findByPaymentType(String paymentType);
}