package com.kalsym.ekedai.model.dao;

import com.kalsym.ekedai.model.enums.DiscountUserStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    private String discountCode;
    private String userPhoneNumber;
    private DiscountUserStatus status;
    
}
