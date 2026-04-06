package com.kalsym.ekedai.model.dao.Order;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrderUpdate {
    String comments;
    String created;
    String modifiedBy;
    String orderId;

    String status;
    String paymentChannel;

    public OrderUpdate(String comments, String created, String modifiedBy, String orderId, String status, String paymentChannel) {
        this.comments = comments;
        this.created = created;
        this.modifiedBy = modifiedBy;
        this.orderId = orderId;
        this.status = status;
        this.paymentChannel = paymentChannel;
    }
}