package com.kalsym.internationalPayment.model.dao;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MtradePaymentResponse {

//    public String accountStatus;
//    public String billDate;
//    public String outStandingAmount;
//    public String message;
//
//    //EBILL MAKE PAYMENT RESPONSE
//    public String clientTransactionId;
//    public String responseCode;
//    public String responseDescription;
//    public String sysTransactionId;
//
//
//    public String accountName;
//
//    public String errorCode;
//    public String voucherImage;
//    public String voucherSerial;
//    public String expiryDate;

    String customerTransactionId;
    String responseCode;
    String responseDescription;
    String systemTransactionId;
    String voucherNo;
    String voucherSerial;
    String voucherExpiryDate;

    String voucherUrl;


    //Query Operator

    String productOwner;
    String productOwnerName;

}
