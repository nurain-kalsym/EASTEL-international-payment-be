package com.kalsym.ekedai.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.DiscountUser;
import com.kalsym.ekedai.model.DiscountUserId;
import com.kalsym.ekedai.model.enums.DiscountUserStatus;

@Repository
public interface DiscountUserRepository extends JpaRepository<DiscountUser,DiscountUserId>, JpaSpecificationExecutor<DiscountUser> {
    
    Optional<DiscountUser> findById(DiscountUserId id);

    List<DiscountUser> getByDiscountId(@Param("discountId") String discountId);

    List<DiscountUser> getDiscountUserByUserPhoneNumber(@Param("userPhoneNumber") String userPhoneNumber);

    List<DiscountUser> getDiscountUserByStatus(@Param("status") DiscountUserStatus status);

    long countByDiscountId(String discountId);

}

