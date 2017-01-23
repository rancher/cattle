package io.github.ibuildthecloud.gdapi.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static Date parse(String date) throws ParseException {
        if (date == null)
            return null;

        if ("now".equals(date)) {
            return new Date();
        }

        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
        return df.parse(date);
    }

    public static String toString(Date date) {
        if (date == null)
            return null;

        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(date);
    }
}
