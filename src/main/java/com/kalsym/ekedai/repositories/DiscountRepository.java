package com.kalsym.ekedai.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.Discount;
import com.kalsym.ekedai.model.enums.CalculationType;
import com.kalsym.ekedai.model.enums.DiscountStatus;

@Repository
public interface DiscountRepository extends JpaRepository<Discount,String>, JpaSpecificationExecutor<Discount> {

    Optional<Discount> findByDiscountCode(@Param("discountCode") String discountCode);

    List<Discount> getDiscountByStatus(@Param("status") DiscountStatus status);

    List<Discount> getDiscountByCalculationType(@Param("calculationType") CalculationType calculationType);

    Specification<Discount> getDiscountByStatusAndCalculationType(DiscountStatus status, CalculationType calculationType);
}
