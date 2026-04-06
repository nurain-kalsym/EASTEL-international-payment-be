package com.kalsym.ekedai.model.dao.Order;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class OrderGroup {

    private String id;
    private String subTotal;
    private Double total;
    private String regionCountryId;
}
