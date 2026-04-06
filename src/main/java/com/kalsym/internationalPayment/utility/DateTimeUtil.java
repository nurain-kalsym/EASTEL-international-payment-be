package com.kalsym.internationalPayment.utility;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateTimeUtil {

    /**
     * *
     * Generate current timestamp string with format 'yyyy-MM-dd HH:mm:ss.SSS'
     *
     * @return
     */
    public static Date currentTimestamp() {
        return Date.from(Instant.now());
    }

    /**
     * *
     * Generate current timestamp string with format 'yyyy-MM-dd HH:mm:ss'
     *
     * @return
     */
    public static String currentTimestampToString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date currentDate = new Date();
        String currentTimeStamp = sdf.format(currentDate);
        return currentTimeStamp;
    }

    /**
     * *
     * Generate expiry time by adding seconds
     *
     * @param seconds
     * @return
     */
    public static Date expiryTimestamp(int seconds) {
        Date expiryDate = Date.from(Instant.now().plusSeconds(seconds));
        return expiryDate;
    }
    
    
    public static Date convertToDateViaInstant(LocalDateTime dateToConvert, ZoneId timezone) {
    return java.util.Date
      .from(dateToConvert.atZone(timezone)
      .toInstant());
    }
    
    
    public static LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert, ZoneId timezone) {
        return dateToConvert.toInstant()
          .atZone(timezone)
          .toLocalDateTime();
    }
    
    
    public static Date ConvertDateToTimeZone(Date dateToConvert, ZoneId timezone) {
       LocalDateTime ldt = LocalDateTime.ofInstant(dateToConvert.toInstant(), timezone);
       Date out = Date.from(ldt.atZone(timezone).toInstant());
       return out;
    }
}
