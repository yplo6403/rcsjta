package com.gsma.rcs.cms.imap.message;

import android.test.AndroidTestCase;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.mime.SmsMimeMessage;

import junit.framework.Assert;

public class SmsMimeMessageTest extends AndroidTestCase {
    
    public void test(){

        String body = "1";

        String payload= new StringBuilder()
                .append("From: +33642575779").append(Constants.CRLF)
                .append("To: tel:+33640332859").append(Constants.CRLF)
                .append("Date: mar., 29 09 2015 11:09:20.826 +0200").append(Constants.CRLF)
                .append("Conversation-ID: 1443517760826").append(Constants.CRLF)
                .append("Contribution-ID: 1443517760826").append(Constants.CRLF)
                .append("IMDN-Message-ID: 1443517760826").append(Constants.CRLF)
                .append("Message-Direction: received").append(Constants.CRLF)
                .append("Message-Correlator: 1").append(Constants.CRLF)
                .append("Message-Context: \"pager-message\"").append(Constants.CRLF)
                .append("Content-Type: Message/CPIM").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("From: +33642575779").append(Constants.CRLF)
                .append("To: +33640332859").append(Constants.CRLF)
                .append("imdn.Message-ID: 1443517760826").append(Constants.CRLF)
                .append("DateTime: mar., 29 09 2015 11:09:20.826 +0200").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("Content-Type: text/plain; charset=utf-8").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append(body).toString();
                
        SmsMimeMessage smsMimeMessage = new SmsMimeMessage(payload);
        Assert.assertEquals(body, smsMimeMessage.getBodyPart());
        
        }

}
