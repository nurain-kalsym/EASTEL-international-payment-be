package com.kalsym.internationalPayment.utility;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomTimestampDeserializer implements JsonDeserializer<Date> {
    private static final String[] DATE_FORMATS = new String[] {
            "yyyy-MM-dd HH:mm:ss.SSS", // With milliseconds
            "yyyy-MM-dd HH:mm:ss"      // Without milliseconds
    };

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String dateStr = json.getAsString();
        for (String format : DATE_FORMATS) {
            try {
                return new SimpleDateFormat(format).parse(dateStr);
            } catch (ParseException e) {
                // Try the next format
            }
        }
        throw new JsonParseException("Unparseable date: \"" + dateStr + "\"");
    }
}