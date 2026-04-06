package com.kalsym.ekedai.model;

import java.util.List;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.kalsym.ekedai.model.enums.ExtraStep;
import com.kalsym.ekedai.model.enums.Status;
import com.kalsym.ekedai.model.enums.VariantType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class ProductRequest {

    private Integer id;
    private String productCode;
    private String productName;
    private String imageId;
    @Enumerated(EnumType.STRING)
    private Status status;
    private String countryCode;
    private List<ProductVariantRequest> productVariant;
    private Integer categoryId;
    private Integer maxAmount;
    private Integer minAmount;
    @Enumerated(EnumType.STRING)
    private VariantType productType;
    private String description;
    private String purchaseDescription;
    private String tnc;
    private List<ProductRequiredInfoRequest> productRequiredInfo;
    @Enumerated(EnumType.STRING)
    private ExtraStep extraStep;
}
