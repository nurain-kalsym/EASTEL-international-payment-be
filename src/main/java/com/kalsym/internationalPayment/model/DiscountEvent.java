package com.kalsym.internationalPayment.model;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "discount_event")
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscountEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String discountName;

    @Temporal(jakarta.persistence.TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date startDate;  

    @Temporal(jakarta.persistence.TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date endDate;  

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "discountId", insertable = false, updatable = false)
    private List<ProductDiscount> productDiscount;

    public static DiscountEvent castReference(DiscountEventRequest req){

        DiscountEvent body = new DiscountEvent();
        //set the id for update data
        if(req.getId() != null){

            body.setId(req.getId());

        }

        body.setDiscountName(req.getDiscountName());
        body.setStartDate(req.getStartDate());
        body.setEndDate(req.getEndDate());

        return body;
    }

    public void updateData(DiscountEvent req){
        
        discountName = req.getDiscountName();
        startDate = req.getStartDate();
        endDate = req.getEndDate();

    }

}
