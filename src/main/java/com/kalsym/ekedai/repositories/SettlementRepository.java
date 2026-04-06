package com.kalsym.ekedai.repositories;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kalsym.ekedai.model.Settlement;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Integer> {
    Page<Settlement> findByBatchDate(LocalDate batchDate, Pageable pageable);
}