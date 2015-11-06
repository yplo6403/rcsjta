package com.gsma.rcs.cms.imap.message.mime;

import android.test.AndroidTestCase;

public class SmsMimeBodyTest extends AndroidTestCase{

    public static final String content = "myContent";

    public void test(){
        SmsMimeBody mimebody = new SmsMimeBody();
        mimebody.parsePayload(content);
        assertEquals(content, mimebody.toString());
    }

}
