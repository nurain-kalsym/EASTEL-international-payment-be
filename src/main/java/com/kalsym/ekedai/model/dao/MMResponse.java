package com.kalsym.ekedai.model.dao;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MMResponse {

    String otpReferenceNo;
    Boolean isOtpRequired;
    String code;
    String message;
    String status;
    String token;
    String spTransactionId;

    String paymentStatus;


}
