package com.kalsym.internationalPayment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalsym.internationalPayment.model.enums.ExtraStep;
import com.kalsym.internationalPayment.model.enums.Status;
import com.kalsym.internationalPayment.model.enums.VariantType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "product")
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Product implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String productCode;

    private String productName;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Integer categoryId;

    private Integer maxAmount;

    private Integer minAmount;

    private String countryCode;

    private String description;

    private String purchaseDescription;

    private String tnc;

    @Enumerated(EnumType.STRING)
    private ExtraStep extraStep;

    // private String wspProductCodeBillPrepaid;

    @OneToOne()
    @JoinColumn(name = "categoryId", referencedColumnName = "id", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private ProductCategory productCategoryDetails;

    private String imageId;

    @OneToOne()
    @JoinColumn(name = "imageId", referencedColumnName = "id", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private ImageAssets imageDetails;

    @Transient
    private String featuredImage;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "productId", referencedColumnName = "id", insertable = false, updatable = false)
    // @NotFound(action = NotFoundAction.IGNORE)
    private List<ProductVariant> productVariant;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "productCode", referencedColumnName = "productCode", insertable = false, updatable = false)
    // @NotFound(action = NotFoundAction.IGNORE)
    private List<ProductRequiredInfo> productRequiredInfo;

    @Enumerated(EnumType.STRING)
    private VariantType productType;

    private String serviceId;

    public static Product castReference(ProductRequest req) {

        Product body = new Product();
        // set the id for update data
        if (req.getId() != null) {
            body.setId(req.getId());
        }

        body.setProductCode(req.getProductCode());
        body.setProductName(req.getProductName());
        body.setImageId(req.getImageId());
        body.setStatus(req.getStatus());
        body.setCategoryId(req.getCategoryId());
        body.setMaxAmount(req.getMaxAmount());
        body.setMinAmount(req.getMinAmount());
        body.setCountryCode(req.getCountryCode());
        body.setProductType(req.getProductType());
        body.setDescription(req.getDescription());
        body.setPurchaseDescription(req.getPurchaseDescription());
        body.setTnc(req.getTnc());
        body.setExtraStep(req.getExtraStep());
        return body;
    }

    public void updateData(Product reqProduct) {

        productCode = reqProduct.getProductCode();

        productName = reqProduct.getProductName();

        imageId = reqProduct.getImageId();

        status = reqProduct.getStatus();

        maxAmount = reqProduct.getMaxAmount();

        minAmount = reqProduct.getMinAmount();

        countryCode = reqProduct.getCountryCode();

        productType = reqProduct.getProductType();

        description = reqProduct.getDescription();

        purchaseDescription = reqProduct.getPurchaseDescription();

        tnc = reqProduct.getTnc();

        extraStep = reqProduct.getExtraStep();

        categoryId = reqProduct.getCategoryId();

    }

}
