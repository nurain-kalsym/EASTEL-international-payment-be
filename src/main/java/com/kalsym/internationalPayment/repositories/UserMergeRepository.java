package com.kalsym.internationalPayment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.kalsym.internationalPayment.model.UserMerge;
import com.kalsym.internationalPayment.model.enums.UserMergeStatus;

import java.util.List;

public interface UserMergeRepository extends JpaRepository<UserMerge, Long> {
    List<UserMerge> findByMergeStatus(@Param("mergeStatus") UserMergeStatus mergeStatus);
}