package com.kalsym.ekedai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.VariantCategory;


@Repository
public interface VariantCategoryRepository extends JpaRepository<VariantCategory, String> {    
}
