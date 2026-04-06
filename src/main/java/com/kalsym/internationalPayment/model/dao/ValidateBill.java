package com.kalsym.internationalPayment.model.dao;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidateBill {
    private String billStatus;
    private String responseDescription;
    public JsonNode showBill;
    public JsonNode planList;

}
