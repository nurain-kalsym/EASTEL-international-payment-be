package com.kalsym.internationalPayment.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kalsym.internationalPayment.model.enums.CalculationType;
import com.kalsym.internationalPayment.model.enums.DiscountStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "discount")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class DiscountWithDetails {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String name;

    @Column(unique = true)
    private String discountCode;

    private String deleteReason;

    private Double discountValue;

    private Double maxDiscountAmount;

    private Double minimumSpend;

    private Integer totalQuantity;

    @Enumerated(EnumType.STRING)
    private DiscountStatus status;

    @Enumerated(EnumType.STRING)
    private CalculationType calculationType;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date endDate;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date createdDate;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date updatedDate;

    @OneToMany
    @JoinColumn(name = "discountId", insertable = false, updatable = false)
    private List<DiscountProductVariant> discountProductVariant;

}