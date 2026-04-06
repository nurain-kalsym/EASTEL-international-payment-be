package com.kalsym.ekedai.repositories;

import com.kalsym.ekedai.model.DiscountWithDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountWithDetailsRepository extends JpaRepository<DiscountWithDetails,String>, JpaSpecificationExecutor<DiscountWithDetails> {
}
