package com.kalsym.internationalPayment.repositories;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.Product;
import com.kalsym.internationalPayment.model.ProductCategory;
import com.kalsym.internationalPayment.model.enums.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByProductCode(@Param("productCode") String productCode);

    Optional<Product> findByProductCodeContains(@Param("productCode") String productCode);

    Optional<Product> findByProductName(String productName);

    Specification<Product> getProductByProductNameAndCategoryId(String productName, Integer categoryId);

    @Query(value = "SELECT DISTINCT (countryCode) from product where categoryId =:categoryId", nativeQuery = true)
    List<Object[]> distinctQueryCountry(@Param("categoryId") Integer categoryId);

    List<Product> getProductsByCategoryIdAndCountryCode(Integer id, String countryCode);

    // Custom query to get categories by countryCode and active status
    @Query("SELECT DISTINCT pv.category FROM Product p JOIN p.productVariant pv WHERE p.countryCode = :countryCode AND p.status = :status AND pv.category IS NOT NULL")
    List<String> findDistinctCategoriesByCountryCodeAndStatus(@Param("countryCode") String countryCode,
            @Param("status") Status status);

    // Query to find categories that have active products for a specific country
    // code
    @Query("SELECT DISTINCT p.productCategoryDetails FROM Product p WHERE p.status = :status AND p.countryCode = :countryCode AND (:parentCategoryId IS NULL OR p.productCategoryDetails.parentCategoryId = :parentCategoryId)")
    List<ProductCategory> findActiveCategoriesByCountryCodeAndParentCategory(@Param("status") Status status,
            @Param("countryCode") String countryCode, @Param("parentCategoryId") Integer parentCategoryId);

    @Query("SELECT COUNT(DISTINCT p.id) AS product_count " +
            "FROM Product p " +
            "LEFT JOIN p.productVariant pv " +
            "ON pv.productId = p.id " +
            "WHERE (p.categoryId IS NULL " +
            "OR (pv.id IS NULL OR pv.variantType IS NULL)) " +
            "AND (p.productType <> 'BILLPAYMENT' or p.productType IS NULL)")
    Long countProductsNotPhysicalOrBillPaymentAndNoVariant();

    @Query("SELECT COUNT(DISTINCT p.id) AS product_count " +
            "FROM Product p " +
            "LEFT JOIN p.productVariant pv ON pv.productId = p.id " +
            "LEFT JOIN p.productRequiredInfo pri ON pri.productCode = p.productCode " +
            "WHERE (p.categoryId IS NULL " +
            "OR pv.id IS NULL OR pv.variantType IS NULL " +
            "OR pri.id IS NULL OR pri.fieldValue IS NULL) " +
            "AND p.productType = 'BILLPAYMENT'")
    Long countProductsPaymentAndNoRequiredField();

}
