package com.gsma.rcs.cms.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtils {

    private static final String CMS_DATE_FORMAT = "E, dd MM yyyy HH:mm:ss.S Z";

    public static String getDateAsString(long date){        
        return new SimpleDateFormat(CMS_DATE_FORMAT).format(date);
    }
    
    public static Long parseDate(String date){
        long d = -1;
        try {
            d= new SimpleDateFormat(CMS_DATE_FORMAT).parse(date).getTime();
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
    } 
}
