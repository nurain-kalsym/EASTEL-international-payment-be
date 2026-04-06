package com.kalsym.ekedai.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@ToString
@Getter
@Setter
public class ProductRequiredInfoRequest {
    
    private Integer id;

    private String productCode;

    private String fieldLabel;

    private String fieldValue;

    private String regex;

    private String regexDescription;

    private Boolean mandatory;

}
