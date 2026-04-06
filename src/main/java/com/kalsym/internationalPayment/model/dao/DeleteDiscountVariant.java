package com.kalsym.internationalPayment.model.dao;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteDiscountVariant {
    
    private String discountId;
    private String discountProductVariantId;
    private String productVariantName;
}