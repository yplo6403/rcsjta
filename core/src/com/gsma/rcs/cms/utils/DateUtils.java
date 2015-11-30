package com.gsma.rcs.cms.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateUtils {

    private static final String CMS_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
    //private static final String CMS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    private static final String MMS_FILE_DATE_FORMAT = "yyyyMMdd_HHmmss";

    public static String getDateAsString(long date){        
        return new SimpleDateFormat(CMS_DATE_FORMAT, Locale.US).format(date);
    }

    public static String getMmsFileDate(long date){
        return new SimpleDateFormat(MMS_FILE_DATE_FORMAT, Locale.US).format(date);
    }

    public static Long parseDate(String date){
        long d = -1;
        try {
            d= new SimpleDateFormat(CMS_DATE_FORMAT, Locale.US).parse(date).getTime();
        }catch (ParseException e) {
            e.printStackTrace();
        }
        return d;
    }
    
    public static void main(String[] args){
        Long now = Calendar.getInstance().getTimeInMillis();
        String dateStr = getDateAsString(now);
        Long now2 = parseDate(dateStr);
        System.out.println(now);
        System.out.println(now2);


        dateStr= "Fri, 20 Nov 2015 10:33:50 +0100";
        now2 = parseDate(dateStr);
    } 
}
