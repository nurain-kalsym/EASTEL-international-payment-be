package com.kalsym.internationalPayment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.DiscountEvent;

@Repository
public interface DiscountEventRepository extends JpaRepository<DiscountEvent,Integer> {
    
}
