package com.kalsym.internationalPayment.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@ToString
@Getter
@Setter
public class ProductDiscountRequest {
    
    private Integer id;
    
    private Integer productVariantId;

    private String calculationType;

    private Double discountAmount;

    private Integer discountId;

}
