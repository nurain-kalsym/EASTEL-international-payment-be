package com.kalsym.internationalPayment.model.dao;

import lombok.Getter;
import lombok.Setter;

import com.kalsym.internationalPayment.model.CampaignCriteria;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Getter
@Setter
public class CampaignCriteriaDTO {
    @Enumerated(EnumType.STRING)
    private CampaignCriteria.CampaignCriterionType criterionType;

    private String criterionValue;
    private String operator;
    private Integer groupId;

}
