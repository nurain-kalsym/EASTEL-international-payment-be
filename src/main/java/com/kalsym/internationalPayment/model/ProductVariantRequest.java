package com.kalsym.internationalPayment.model;

import com.kalsym.internationalPayment.model.enums.Status;
import com.kalsym.internationalPayment.model.enums.VariantType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class ProductVariantRequest {

    private Integer id;

    private String variantName;

    private Double price;

    private String title;

    private String description;

    private String purchaseDescription;

    private Boolean limited;

    private String validity;

    private String category;

    @Enumerated(EnumType.STRING)
    private VariantType variantType;
    
    @Enumerated(EnumType.STRING)
    private Status status;

    private String wspProductCode;

    private Double deno;

    private Boolean ozoPayMethod;
    private Boolean hellosimMethod;
    private Boolean mmWalletMethod;
    
    private Double fixFee;
}