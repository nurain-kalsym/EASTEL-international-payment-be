package com.kalsym.ekedai.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceId {
    private String referral;
    private String loyalty;

    public ServiceId(String referral, String loyalty) {
        this.referral = referral;
        this.loyalty = loyalty;
    }

    public ServiceId() {
    }
}
