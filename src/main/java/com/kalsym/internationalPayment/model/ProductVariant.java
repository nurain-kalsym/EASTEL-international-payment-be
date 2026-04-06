package com.kalsym.internationalPayment.model;

import com.kalsym.internationalPayment.model.enums.Status;
import com.kalsym.internationalPayment.model.enums.VariantType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "product_variant")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer productId;

    private String variantName;

    private Double price;

    @Transient
    private Double discountedPrice;

    private String title;

    private String description;

    private String purchaseDescription;

    private Boolean limited;

    private String validity;

    private String category;

    @Enumerated(EnumType.STRING)
    private VariantType variantType;

    private String wspProductCode;

    private Double deno;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Boolean ozoPayMethod;

    private Boolean hellosimMethod;

    private Boolean mmWalletMethod;

    private String planId;

    private String serviceId;

    private Boolean isRisky;

    private Double fixFee;

    public static ProductVariant castReference(ProductVariantRequest req) {

        ProductVariant body = new ProductVariant();
        // set the id for update data
        if (req.getId() != null) {

            body.setId(req.getId());

        }

        body.setVariantName(req.getVariantName());
        body.setPrice(req.getPrice());

        body.setTitle(req.getTitle());
        body.setDescription(req.getDescription());
        body.setPurchaseDescription(req.getPurchaseDescription());
        body.setLimited(req.getLimited());
        body.setValidity(req.getValidity());
        body.setVariantType(req.getVariantType());
        body.setCategory(req.getCategory());
        body.setWspProductCode(req.getWspProductCode());
        body.setDeno(req.getDeno());
        body.setStatus(req.getStatus());

        body.setOzoPayMethod(req.getOzoPayMethod());
        body.setHellosimMethod(req.getHellosimMethod());
        body.setMmWalletMethod(req.getMmWalletMethod());

        body.setServiceId(req.getServiceId());
        body.setIsRisky(req.getIsRisky());

        body.setFixFee(req.getFixFee());

        return body;
    }

    public void updateData(ProductVariant reqBody) {

        productId = reqBody.getProductId();
        variantName = reqBody.getVariantName();
        price = reqBody.getPrice();

        title = reqBody.getTitle();
        description = reqBody.getDescription();
        purchaseDescription = reqBody.getPurchaseDescription();
        limited = reqBody.getLimited();
        validity = reqBody.getValidity();

        variantType = reqBody.getVariantType();
        category = reqBody.getCategory();

        wspProductCode = reqBody.getWspProductCode();
        deno = reqBody.getDeno();

        status = reqBody.getStatus();

        ozoPayMethod = reqBody.getOzoPayMethod();
        hellosimMethod = reqBody.getHellosimMethod();
        mmWalletMethod = reqBody.getMmWalletMethod();

        serviceId = reqBody.getServiceId();
        isRisky = reqBody.getIsRisky();
        fixFee = reqBody.getFixFee();
    }

}
