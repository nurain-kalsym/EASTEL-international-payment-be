package com.kalsym.internationalPayment.utility;

import org.springframework.util.StringUtils;

public class MsisdnUtil {
    public static String formatMsisdn(String msisdn) {
        if (StringUtils.hasText(msisdn)) {
            if (msisdn.startsWith("0")) {
                msisdn = "6" + msisdn;
            } else if (msisdn.startsWith("+6")) {
                msisdn = msisdn.substring(1); // Remove the first character (i.e., "+")
            }
        }
        return msisdn;
    }
}
