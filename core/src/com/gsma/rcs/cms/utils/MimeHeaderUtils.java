package com.gsma.rcs.cms.utils;

import com.gsma.rcs.cms.Constants;

public class MimeHeaderUtils {

    private final static String SIP_INF = "<";
    private final static String SIP_SUP = ">";

    public static String asSipContact(String contact){
        StringBuilder sb = new StringBuilder(SIP_INF);
        if(!contact.startsWith(Constants.TEL_PREFIX)){
            sb.append(Constants.TEL_PREFIX);
        }
        sb.append(contact);
        sb.append(SIP_SUP);
        return sb.toString();
    }

    public static String asContact(String sipContact){
        if(sipContact.startsWith(SIP_INF) && sipContact.endsWith(SIP_SUP)){
            sipContact = sipContact.substring(SIP_INF.length(), sipContact.lastIndexOf(SIP_SUP));
        }

        if(sipContact.toLowerCase().startsWith(Constants.TEL_PREFIX)){
            sipContact = sipContact.substring(Constants.TEL_PREFIX.length());
        }
        return sipContact;
    }

}
