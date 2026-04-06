package com.kalsym.ekedai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.DiscountEvent;

@Repository
public interface DiscountEventRepository extends JpaRepository<DiscountEvent,Integer> {
    
}
