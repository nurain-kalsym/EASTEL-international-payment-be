package com.kalsym.internationalPayment.model.dao;

import java.io.Serializable;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductVariantPaymentId implements Serializable {

    private Integer productVariantId;

    private Long paymentChannelId;
}
