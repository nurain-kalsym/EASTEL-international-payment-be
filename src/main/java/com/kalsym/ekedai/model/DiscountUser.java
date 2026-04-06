package com.kalsym.ekedai.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.kalsym.ekedai.model.enums.DiscountUserStatus;

import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "discount_user")
@IdClass(DiscountUserId.class)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class DiscountUser {
    @Id
    private String discountId;

    @Id
    private String userPhoneNumber;

    @Enumerated(EnumType.STRING)
    private DiscountUserStatus status;

    @ManyToOne
    @JoinColumn(name = "discountId", referencedColumnName = "id", insertable = false, updatable = false)
    private Discount discount;

}
