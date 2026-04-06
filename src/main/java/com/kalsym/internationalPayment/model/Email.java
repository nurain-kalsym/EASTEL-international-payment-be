package com.kalsym.internationalPayment.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class Email {

    private String[] to;
    private String subject;
    private AccountVerificationEmailBody userAccountBody;
    private String from;
    private String fromName;    
    // private String domain;

}
