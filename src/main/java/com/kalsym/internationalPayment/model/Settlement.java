package com.kalsym.internationalPayment.model;

import java.util.Date;

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
import java.time.LocalDate;


@Entity
@Table(name = "settlement")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class Settlement {
    
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    private String paymentMethod;

    private Date transactionDate;

    private String wspProductCode;

    private String paymentTransactionId;

    private Double transactionAmount;

    private Double collectionAmount;

    private String productName;

    private LocalDate batchDate;

}
