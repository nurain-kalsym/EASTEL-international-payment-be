package com.kalsym.internationalPayment.model.dao;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;
import java.util.List;

import com.kalsym.internationalPayment.model.Campaign;

@Getter
@Setter
public class CampaignDTO {
    private String campaignName;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;

    @Enumerated(EnumType.STRING)
    private Campaign.CampaignRewardType rewardType;

    private String rewardValue;

//    @Enumerated(EnumType.STRING)
//    private Campaign.CampaignStatus status;

    private List<CampaignCriteriaDTO> criteria;

}
