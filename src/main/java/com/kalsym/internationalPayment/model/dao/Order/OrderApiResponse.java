package com.kalsym.internationalPayment.model.dao.Order;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderApiResponse<T> {
    private String timestamp;
    private int status;
    private List<T> data;
    private String path;
}
