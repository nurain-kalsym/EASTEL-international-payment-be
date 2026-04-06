package com.kalsym.internationalPayment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kalsym.internationalPayment.model.Campaign;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, String>, JpaSpecificationExecutor<Campaign> {

    // Query to find active campaigns within a specified date range
    @Query("SELECT c FROM Campaign c WHERE c.isActive IS TRUE AND c.id != :id AND c.startDate <= :endDate AND c.endDate >= :startDate")
    List<Campaign> findActiveCampaignsInDateRangeAndToExclude(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("id") String id);

    @Query("SELECT c FROM Campaign c WHERE c.isActive IS TRUE AND c.startDate <= :currentDate AND c.endDate >= :currentDate")
    Optional<Campaign> findOneActiveCampaign(@Param("currentDate") LocalDate currentDate);

    List<Campaign> findByRewardTypeAndRewardValue(Campaign.CampaignRewardType rewardType, String rewardValue);

}
