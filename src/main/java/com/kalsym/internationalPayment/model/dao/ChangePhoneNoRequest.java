package com.kalsym.internationalPayment.model.dao;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePhoneNoRequest {
    private String oldPhoneNumber;
    private String newPhoneNumber;

    public ChangePhoneNoRequest(String oldPhoneNumber, String newPhoneNumber) {
        this.oldPhoneNumber = oldPhoneNumber;
        this.newPhoneNumber = newPhoneNumber;
    }
}
