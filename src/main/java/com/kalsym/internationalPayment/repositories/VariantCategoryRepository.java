package com.kalsym.internationalPayment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.VariantCategory;


@Repository
public interface VariantCategoryRepository extends JpaRepository<VariantCategory, String> {    
}
