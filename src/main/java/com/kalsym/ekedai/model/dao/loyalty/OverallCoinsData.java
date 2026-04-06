package com.kalsym.ekedai.model.dao.loyalty;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OverallCoinsData {
    public OverallCoinsData() {
    }
    private int availableCoins;
    private double availableCash;
    private int expiringCoins;
}
