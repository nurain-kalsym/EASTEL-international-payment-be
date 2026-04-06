package com.kalsym.internationalPayment.model;

import com.kalsym.internationalPayment.model.enums.VariantType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class FavouriteRequest {

    private Long id;

    private String productCode;

    private String label;

    private String accountNo;

    private String countryCode;

    @Enumerated(EnumType.STRING)
    private VariantType variantType;
}
