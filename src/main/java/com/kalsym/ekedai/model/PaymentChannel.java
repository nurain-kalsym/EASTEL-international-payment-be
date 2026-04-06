package com.kalsym.ekedai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;

@Entity
@Table(name = "payment_channel")
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class PaymentChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String channelName;
    Integer minValue;
    Long parentId;
    Boolean status;
}
