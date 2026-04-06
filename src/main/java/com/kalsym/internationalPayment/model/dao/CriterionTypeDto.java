package com.kalsym.internationalPayment.model.dao;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.List;

import com.kalsym.internationalPayment.model.CampaignCriteria;
import com.kalsym.internationalPayment.model.enums.Operator;


@Getter
@Setter
public class CriterionTypeDto {
    private CampaignCriteria.CampaignCriterionType type;
    private String typeLabel;
    @Enumerated(EnumType.STRING)
    private CriterionInput input; // "input", "selection", "date"
    private String inputType;
    private List<String> selectionOptions;
    private List<Operator> operators;

    public CriterionTypeDto(CampaignCriteria.CampaignCriterionType type, String typeLabel, CriterionInput input, String inputType, List<String> selectionOptions, List<Operator> operators) {
        this.type = type;
        this.typeLabel = typeLabel;
        this.input = input;
        this.inputType = inputType;
        this.selectionOptions = selectionOptions;
        this.operators = operators;
    }

    public enum CriterionInput {
        INPUT,
        SELECTION,
        DATE
    }
}
