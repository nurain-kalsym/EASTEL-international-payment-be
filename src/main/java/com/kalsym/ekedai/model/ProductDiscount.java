package com.kalsym.ekedai.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "product_discount")
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer productVariantId;

    private String calculationType;

    private Double discountAmount;

    private Integer discountId;

    public static ProductDiscount castReference(ProductDiscountRequest req) {

        ProductDiscount body = new ProductDiscount();
        // set the id for update data
        if (req.getId() != null) {

            body.setId(req.getId());

        }

        body.setProductVariantId(req.getProductVariantId());
        body.setCalculationType(req.getCalculationType());
        body.setDiscountAmount(req.getDiscountAmount());
        body.setDiscountId(req.getDiscountId());

        return body;
    }

    public void updateData(ProductDiscount reqProductDiscount) {

        productVariantId = reqProductDiscount.getProductVariantId();
        calculationType = reqProductDiscount.getCalculationType();
        discountAmount = reqProductDiscount.getDiscountAmount();

    }

}
