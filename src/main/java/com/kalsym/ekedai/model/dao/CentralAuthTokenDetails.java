package com.kalsym.ekedai.model.dao;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CentralAuthTokenDetails {
    private String appId;
    private String phoneNumber;

    public CentralAuthTokenDetails(String appId, String phoneNumber) {
        this.appId = appId;
        this.phoneNumber = phoneNumber;
    }

    public CentralAuthTokenDetails() {

    }
}
