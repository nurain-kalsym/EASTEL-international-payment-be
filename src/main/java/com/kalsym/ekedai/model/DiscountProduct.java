package com.kalsym.ekedai.model;

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
@Table(name = "discount_product")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class DiscountProduct {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String discountId;

    private String productCode;

    @ManyToOne
    @JoinColumn(name = "discountId", referencedColumnName = "id", insertable = false, updatable = false)
    private Discount discount;

    @ManyToOne
    @JoinColumn(name = "productCode", referencedColumnName = "productCode", insertable = false, updatable = false)
    private Product product;

}
