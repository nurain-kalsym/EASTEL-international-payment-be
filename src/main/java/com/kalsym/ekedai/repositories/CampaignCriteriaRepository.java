package com.kalsym.ekedai.repositories;

import com.kalsym.ekedai.model.CampaignCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import jakarta.transaction.Transactional;
import java.util.List;

public interface CampaignCriteriaRepository extends JpaRepository<CampaignCriteria, String> {
    List<CampaignCriteria> findByCampaignId(String campaignId);

    @Transactional
    @Modifying
    void deleteByCampaignId(String campaignId);
}
