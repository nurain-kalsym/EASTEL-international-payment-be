package com.kalsym.ekedai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalsym.ekedai.model.dao.ProductVariantPaymentId;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_variant_payment_channel")
@Setter
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ProductVariantPaymentChannel {

    @EmbeddedId
    private ProductVariantPaymentId id;
    Boolean enabled;
}
