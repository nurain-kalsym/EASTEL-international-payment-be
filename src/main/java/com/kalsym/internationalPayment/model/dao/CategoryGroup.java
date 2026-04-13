package com.kalsym.internationalPayment.model.dao;

import java.util.List;

import com.kalsym.internationalPayment.model.ProductCategory;
import com.kalsym.internationalPayment.model.enums.CategorySection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CategoryGroup {
    private CategorySection section;
    private List<ProductCategory> items;

}