package com.gsma.rcs.cms.utils;

import java.util.Iterator;

public class ListUtils {

    /**
     * Build a string of delimited items
     *
     * @param s an iterator over a CharSequence
     * @param delimiter a delimiter
     * @return the string of delimited items
     */
    public static String join(Iterable<? extends Number> s, String delimiter) {
        Iterator<? extends Number> iter = s.iterator();
        if (!iter.hasNext())
            return "";
        StringBuilder buffer = new StringBuilder(String.valueOf(iter.next()));
        while (iter.hasNext())
            buffer.append(delimiter).append(iter.next());
        return buffer.toString();
    }
}
