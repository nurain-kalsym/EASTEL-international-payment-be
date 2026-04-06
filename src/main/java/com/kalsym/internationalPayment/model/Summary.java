package com.kalsym.internationalPayment.model;

import java.time.LocalDate;

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
@Table(name = "summary")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class Summary {
    
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    private LocalDate date;

    private Integer totalTransaction;

    private Double totalCollection;

    private Double totalAmount;

}
