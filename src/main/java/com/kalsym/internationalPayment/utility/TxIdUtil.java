package com.kalsym.internationalPayment.utility;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/**
 *
 * @author saros
 */
public class TxIdUtil {

    public static String generateReferenceId(String prefix) {
        String referenceId = prefix;
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyMMddhhmmss");
        String datetime = ft.format(dNow);

        Random rnd = new Random();
        int n = 100 + rnd.nextInt(900);

        referenceId = referenceId + datetime + n;

        return referenceId;
    }
    
    
     /**
     * Generates a 16 character unique hex terminated and prefix appended Random
     * UUID
     *
     * @param appInitials prefix to be appended
     * @return
     */
    public static String GenerateSystemTransactionId(String appInitials) {
        int min = 256; // hex equivalant 100
        int max = 4095; // hex equivalant FFF

        Random r = new Random();
        int decRand = r.nextInt(max - min + 1) + min;
        //    System.err.println(decRand);
        String hexRand = Integer.toHexString(decRand);
        // System.out.println(hexRand);
        String Prefix = appInitials;

        DateFormat dateFormat = new SimpleDateFormat("ddMMyyHHmmssSSS");

        // get current date time with Calendar()
        Calendar cal = Calendar.getInstance();

        String dateStr = dateFormat.format(cal.getTime());
        String refID = Prefix + dateStr + hexRand;
        return refID;
    }
}
