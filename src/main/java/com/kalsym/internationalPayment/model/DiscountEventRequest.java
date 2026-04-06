package com.kalsym.internationalPayment.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@ToString
@Getter
@Setter
public class DiscountEventRequest {
    
    private Integer id;

    private String discountName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date startDate;  

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date endDate;  

    private List<ProductDiscountRequest> productDiscount;

}
