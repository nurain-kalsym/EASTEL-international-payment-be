package com.kalsym.internationalPayment.model.dao;

import com.kalsym.internationalPayment.model.enums.VariantType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MtradePaymentRequest {

    private String productCode;
    private Integer productId;
    private String senderMsisdn;
    private String recipientMsisdn;
    private String customerTransactionId;
    private String payAmount;
    private String routingId;
    private String accountNo;
    private VariantType variantType;
    private String billPhoneNumber;
    private String extra1;
    private String extra2;
    private String extra3;
    private String extra4;

}
