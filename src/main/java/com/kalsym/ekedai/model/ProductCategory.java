package com.kalsym.ekedai.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "product_category")
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String category;

    private Integer parentCategoryId;

    private Boolean status;

    @ManyToOne
    @JoinColumn(name = "parentCategoryId", referencedColumnName = "id", insertable = false, updatable = false)
    private ProductCategory parentCategory;


    public void updateData(ProductCategory req) {

        category = req.getCategory();

        parentCategoryId = req.getParentCategoryId();

        status = req.getStatus();

    }

}
