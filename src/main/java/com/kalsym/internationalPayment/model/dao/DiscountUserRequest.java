package com.kalsym.internationalPayment.model.dao;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiscountUserRequest {

    private String discountCode;
    private String userPhoneNumber;
    
}
