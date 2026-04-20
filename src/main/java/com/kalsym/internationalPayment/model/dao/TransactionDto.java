package com.kalsym.internationalPayment.model.dao;

import com.kalsym.internationalPayment.model.enums.TransactionEnum;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionDto {
    private Double transactionAmount; //pass amount here if varianttype = billpayment
    private Double denoAmount; //pass amount here if varianttype != billpayment
    private Integer productVariantId;
    private String accountNo;
    private TransactionEnum paymentEnum;
    private String billPhoneNumber;
    private String extra1;
    private String extra2;
    private String extra3;
    private String extra4;
    private Double fixFee;
}