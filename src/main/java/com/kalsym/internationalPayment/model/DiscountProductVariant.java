package com.kalsym.internationalPayment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "discount_product_variant")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class DiscountProductVariant {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String discountId;

    private Integer productVariantId;

    private String productPage;
    
    @ManyToOne
    @JoinColumn(name = "productVariantId", referencedColumnName = "id", insertable = false, updatable = false)
    private ProductVariant productVariant;

}
