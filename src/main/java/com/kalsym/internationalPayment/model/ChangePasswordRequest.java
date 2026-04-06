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
public class ChangePasswordRequest {        
    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;
    private String msisdn;
}