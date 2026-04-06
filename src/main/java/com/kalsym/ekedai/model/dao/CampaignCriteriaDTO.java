package com.kalsym.ekedai.model.dao;

import com.kalsym.ekedai.model.CampaignCriteria;
import lombok.Getter;
import lombok.Setter;

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
