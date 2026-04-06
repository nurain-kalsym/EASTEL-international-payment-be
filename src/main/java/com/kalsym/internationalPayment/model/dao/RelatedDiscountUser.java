package com.kalsym.internationalPayment.model.dao;

import com.kalsym.internationalPayment.model.enums.DiscountUserStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RelatedDiscountUser {

    private String discountId;
    private String userPhoneNumber;
    private DiscountUserStatus status;
    
}
