package com.kalsym.ekedai.model.dao;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ServiceIdWithProductsResponse {
    private String serviceId;
    private List<ProductDto> products;
}