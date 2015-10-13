package com.gsma.rcs.cms.utils;

import com.gsma.rcs.utils.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class HeaderCorrelatorUtils {
    
    //TODO FGI : we have to finish to implement the message-correlator header algorithm 
    private static final int LIMIT_LENGTH = 160; 
    
    /*package private*/ static final String PREFIX = "=?utf-8?b?";
    /*package private*/ static final String SUFFIX = "?=";
    
    public static String buildHeader(String content){  
        if(content==null){
            return "";
        }
        
        int length = content.length();
        if(length > LIMIT_LENGTH){
            length = LIMIT_LENGTH;
        }
        content = content.substring(0,length);
        content = content.replaceAll("(\\r|\\n)", " ");    
        
        for(char c:content.toCharArray()){
            if(c > 128){
                // no US ASCII character : base 64 encoding
                try {
                    return new StringBuilder(PREFIX).append(new String(Base64.encodeBase64(content.getBytes("UTF-8")),"UTF-8")).append(SUFFIX).toString();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return "";
                }
            }
        }        
        return content;
    }
}
