package com.kalsym.internationalPayment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.Summary;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Integer> {
}