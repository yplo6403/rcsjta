package com.gsma.rcs.cms.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateUtils {

    public static final String CMS_IMAP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
    public static final String CMS_CPIM_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    private static final String MMS_FILE_DATE_FORMAT = "yyyyMMdd_HHmmss";

    public static String getDateAsString(long date, String format){
        return new SimpleDateFormat(format, Locale.US).format(date);
    }

    public static String getMmsFileDate(long date){
        return new SimpleDateFormat(MMS_FILE_DATE_FORMAT, Locale.US).format(date);
    }

    public static Long parseDate(String date, String format){
        long d = -1;
        try {
            d= new SimpleDateFormat(format, Locale.US).parse(date).getTime();
        }catch (ParseException e) {
            e.printStackTrace();
        }
        return d;
    }
}
