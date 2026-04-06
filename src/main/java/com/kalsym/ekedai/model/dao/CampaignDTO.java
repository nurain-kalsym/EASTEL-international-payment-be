package com.kalsym.ekedai.model.dao;

import com.kalsym.ekedai.model.Campaign;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;
import java.util.List;

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
