package com.kalsym.ekedai.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "campaign")
@Getter
@Setter
public class Campaign {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String campaignName;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(name = "createdAt", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updatedAt", nullable = false, updatable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String description;

    @Enumerated(EnumType.STRING)
    private CampaignRewardType rewardType;

    private String rewardValue;

//    @Enumerated(EnumType.STRING)
//    private CampaignStatus status;

    private Boolean isActive;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "campaignId", referencedColumnName = "id", insertable = false, updatable = false)
    // @NotFound(action = NotFoundAction.IGNORE)
    private List<CampaignCriteria> criteria;

    // Default constructor
    public Campaign() {
    }

    public Campaign(String campaignName, LocalDate startDate, LocalDate endDate, String description, CampaignRewardType rewardType, String rewardValue) {
        this.campaignName = campaignName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.rewardType = rewardType;
        this.rewardValue = rewardValue;
    }

    public enum CampaignRewardType {
        DISCOUNT_CODE,
        COINS,
        EXTERNAL_VOUCHER_CODE,
        INTERNAL_VOUCHER_CODE
    }

    public enum CampaignStatus {
        ACTIVE,
        INACTIVE
    }
}

