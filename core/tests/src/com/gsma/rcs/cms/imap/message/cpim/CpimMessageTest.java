package com.gsma.rcs.cms.imap.message.cpim;

import android.test.AndroidTestCase;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.HeaderPart;
import com.gsma.rcs.cms.imap.message.cpim.text.TextCpimBody;

import junit.framework.Assert;

public class CpimMessageTest extends AndroidTestCase{

    private String expectedPayload = new StringBuilder()
    .append("header1: value1").append(Constants.CRLF)
    .append("header2: value2").append(Constants.CRLF)
    .append(Constants.CRLF)
    .append("Content-Type: myContentType").append(Constants.CRLF)
    .append(Constants.CRLF)
    .append("myContent").toString();

    public void testFromPayload(){

        CpimMessage cpimMessage = new CpimMessage(new HeaderPart(), new TextCpimBody());
        cpimMessage.parsePayload(expectedPayload);
        Assert.assertEquals("value1",cpimMessage.getHeader("header1"));
        Assert.assertEquals("value2", cpimMessage.getHeader("header2"));
        Assert.assertEquals("myContentType", cpimMessage.getBody().getContentType());
        Assert.assertEquals("myContent", ((TextCpimBody) cpimMessage.getBody()).getContent());
    }

    public void test(){

        HeaderPart headers = new HeaderPart();
        headers.addHeader("header1", "value1");
        headers.addHeader("header2", "value2");
        CpimBody cpimBody = new TextCpimBody("myContentType", "myContent");

        CpimMessage cpimMessage = new CpimMessage(headers, cpimBody);
        Assert.assertEquals(expectedPayload, cpimMessage.toPayload());
    }

}
