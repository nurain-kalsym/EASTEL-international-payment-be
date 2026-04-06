package com.kalsym.internationalPayment.model;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalsym.internationalPayment.model.enums.VariantType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "favourite")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class Favourite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    private String productCode;

    private String label;

    private String accountNo;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date created;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date updated;

    private String countryCode;

    @Enumerated(EnumType.STRING)
    private VariantType variantType;
    
    @OneToOne()
    @JoinColumn(name = "userId", referencedColumnName = "id", insertable = false, updatable = false)
    private User userDetails;

    @OneToOne()
    @JoinColumn(name = "productCode", referencedColumnName = "productCode", insertable = false, updatable = false)
    private Product product;

    public void updateData(Favourite reqProduct) {

        productCode = reqProduct.getProductCode();

        label = reqProduct.getLabel();

        accountNo = reqProduct.getAccountNo();

        updated = reqProduct.getUpdated();

    }


    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }
}
