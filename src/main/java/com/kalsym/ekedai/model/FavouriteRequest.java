package com.kalsym.ekedai.model;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.kalsym.ekedai.model.enums.VariantType;

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
