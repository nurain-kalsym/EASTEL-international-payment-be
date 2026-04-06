package com.kalsym.internationalPayment.model.dao;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiscountClaimRequest {
    private String discountCode;
    private String userPhoneNumber;
    private String token;
}
