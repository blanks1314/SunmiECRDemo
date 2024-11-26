package com.pos.hardware.connection.demo.help;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
/**
 * @author: Dadong
 * @date: 2024/11/21
 */
public class DateHelper {

    public static String getDateFormatString(long millis, String pattern) {
        Date date = new Date(millis);
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        return dateFormat.format(date);
    }

    public static String getDateFormatString(long millis) {
        return getDateFormatString(millis, "yyyy-MM-dd HH:mm:ss");
    }

}