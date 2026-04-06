package com.kalsym.internationalPayment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

//this entity for calculate discounted price purpose do not use this entity for crud operation

@Entity
@Table(name = "product_discount")
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDiscountedPricing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer productVariantId;

    private String calculationType;

    private Double discountAmount;

    private Integer discountId;

    @OneToOne()
    @JoinColumn(name = "discountId",referencedColumnName="id", insertable = false, updatable = false, nullable = true)
    // @Where(clause = "startDate <NOW() AND endDate>NOW()")
    private DiscountedEvent discountEvent;

    @Transient
    private Double discountedPrice;
}
