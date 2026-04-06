package com.kalsym.internationalPayment.model;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class ResetPassword {
    
	private String newPassword;
	private String msisdn;

}
