package com.kalsym.internationalPayment.model.dao.Order;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
@Setter
@Getter
@ToString
public class Order {
    private String id;
    private String created;
    private String updated;
    private String invoiceId;
    private Object store;
    private List<?> orderItemWithDetails;
    private Object voucherDetail;
}

