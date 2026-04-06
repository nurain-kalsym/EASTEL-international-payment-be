package com.kalsym.internationalPayment.model.dao;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class SymplifiedOrderGroupResponse {

    private String id;
    private List<SymplifiedOrderDTO> orderList;

}
