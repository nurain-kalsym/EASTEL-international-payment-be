package com.kalsym.internationalPayment.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;

@Entity
@Table(name = "campaign_criteria")
@Getter
@Setter
public class CampaignCriteria {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String campaignId;

    @Enumerated(EnumType.STRING)
    private CampaignCriterionType criterionType;

    private String criterionValue;

    private String operator;

    private Integer groupId;

    public enum CampaignCriterionType {
        TRANSACTION_TYPE, // e.g. 'TOPUP', 'VOUCHER', 'BUNDLE', 'ORDER',
        TRANSACTION_AMOUNT,
        PRODUCT_TYPE,
        PRODUCT_COUNTRY_CODE, // e.g. 'MYS', 'NOT_MYS'
        USER_SEGMENT, // e.g. 'new', 'local', 'international'
        USER_AGE,
        USER_GENDER,
        PRODUCT_CATEGORY,
        PRODUCT_BRAND,
        TRANSACTION_DATE,
        TRANSACTION_FREQUENCY,
        USER_LOCATION,
        SHIPPING_LOCATION,
        PAYMENT_METHOD,
        REFERRAL_CODE
    }

}

