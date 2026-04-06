package com.kalsym.ekedai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.ProductCategory;

import java.util.List;

@Repository

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Integer> {
    List<ProductCategory> findByParentCategoryIdIsNull();

    @Query("SELECT pc FROM ProductCategory pc WHERE pc.id NOT IN :excludeIds AND pc.parentCategoryId IS NULL")
    List<ProductCategory> findCategoriesExcluding(@Param("excludeIds") List<Integer> excludeIds);

    List<ProductCategory> findByParentCategoryId(Integer parentCategoryId);

}
