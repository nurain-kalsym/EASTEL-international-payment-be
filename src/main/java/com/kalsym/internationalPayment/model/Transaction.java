package com.kalsym.internationalPayment.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kalsym.internationalPayment.model.enums.PaymentStatus;
import com.kalsym.internationalPayment.model.enums.TransactionEnum;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.*;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Transaction {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String userId;

    private String name;

    private String email;

    private Double transactionAmount;

    private Double denoAmount;

    private String phoneNo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId")
    @NotFound(action = NotFoundAction.IGNORE)
    private Product product;

    private String accountNo;

    private String transactionId;

    private String status;

    private String paymentTransactionId;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date createdDate;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date updatedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date paidDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date callbackDate;

    private String otpReferenceNo;

    private String paymentProviderError;

    private String bank;

    private String bankName;

    private Double chargeAmount;

    private String transactionErrorCode;

    private String errorDescription;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editBy", referencedColumnName = "id", updatable = false, insertable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private User editUser;

    @JsonIgnore
    private String editBy;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productVariantId", referencedColumnName = "id", updatable = false, insertable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private ProductVariant productVariant;

    private Integer productVariantId;

    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    private TransactionEnum transactionType;

    private String spInvoiceId;

    private String spOrderId;

    private String paymentErrorCode;

    private String paymentDescription;

    private String wspTransactionId;

    private String voucherSerial;
    private String voucherNo;
    private String voucherExpiryDate;
    private String voucherUrl;

    private String refundCode;
    private String refundErrorMessage;

    private String billPhoneNumber;

    private Boolean notificationSent;

    private String extra1;
    private String extra2;
    private String extra3;
    private String extra4;

    private String paymentChannel;

    @Transient
    private String callbackToken;

    @Transient
    private String accessKey;

    @Transient
    protected String otpNo;

    private Double discountAmount;
    @JsonIgnore()
    private String discountId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discountId", referencedColumnName = "id", updatable = false, insertable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Discount discount;

    private Double coinsRedeemed;

    private Boolean isFraud;

}
