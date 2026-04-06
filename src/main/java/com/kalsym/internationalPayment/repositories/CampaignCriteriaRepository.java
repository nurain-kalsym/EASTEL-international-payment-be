package com.kalsym.internationalPayment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.kalsym.internationalPayment.model.CampaignCriteria;

import jakarta.transaction.Transactional;
import java.util.List;

public interface CampaignCriteriaRepository extends JpaRepository<CampaignCriteria, String> {
    List<CampaignCriteria> findByCampaignId(String campaignId);

    @Transactional
    @Modifying
    void deleteByCampaignId(String campaignId);
}
