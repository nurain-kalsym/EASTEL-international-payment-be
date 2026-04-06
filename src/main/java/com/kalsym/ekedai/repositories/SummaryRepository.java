package com.kalsym.ekedai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kalsym.ekedai.model.Summary;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Integer> {
}