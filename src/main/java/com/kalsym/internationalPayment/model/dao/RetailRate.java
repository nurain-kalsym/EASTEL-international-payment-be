package com.kalsym.internationalPayment.model.dao;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetailRate {
    private Integer id;
    private String currencyCode;
    private Double rate;
    private String status;
}
