package com.kalsym.ekedai.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "product_required_info")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class ProductRequiredInfo {
    
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    private String productCode;

    private String fieldLabel;

    private String fieldValue;

    private String regex;

    private String regexDescription;

    private Boolean mandatory;

    public static ProductRequiredInfo castReference(ProductRequiredInfoRequest req){

        ProductRequiredInfo body = new ProductRequiredInfo();
        //set the id for update data
        if(req.getId() != null){
            body.setId(req.getId());
        }

        body.setFieldLabel(req.getFieldLabel());
        body.setFieldValue(req.getFieldValue());
        body.setProductCode(req.getProductCode());
        body.setRegex(req.getRegex());
        body.setRegexDescription(req.getRegexDescription());
        body.setMandatory(req.getMandatory());

        return body;
    }

    public void updateData(ProductRequiredInfo reqBody){

        productCode = reqBody.getProductCode();
        fieldLabel = reqBody.getFieldLabel();
        fieldValue = reqBody.getFieldValue();
        regex = reqBody.getRegex();
        regexDescription = reqBody.getRegexDescription();
        mandatory = reqBody.getMandatory();
        
    }

}
