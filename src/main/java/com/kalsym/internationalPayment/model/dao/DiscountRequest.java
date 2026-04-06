package com.kalsym.internationalPayment.model.dao;

import java.util.Date;

import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kalsym.internationalPayment.model.enums.CalculationType;
import com.kalsym.internationalPayment.model.enums.DiscountStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiscountRequest {
    
    private String name;
    private String discountCode;
    private Double discountValue;
    private Double maxDiscountAmount;
    private Double minimumSpend;
    private Integer totalQuantity;
    private DiscountStatus status;
    private CalculationType calculationType;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date endDate;

    private DiscountProductPage[] discountProductPage;
}
