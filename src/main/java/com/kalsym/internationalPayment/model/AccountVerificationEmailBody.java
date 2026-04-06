package com.kalsym.internationalPayment.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author saros
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class AccountVerificationEmailBody {

    public enum ActionType {
        EMAIL_VERIFICATION("Email Verification"),
        PASSWORD_RESET("Password Reset");

        public final String label;

        private ActionType(String label) {
            this.label = label;
        }
    }

    private ActionType actionType;

    private String link;


}
