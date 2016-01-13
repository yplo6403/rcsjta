package com.gsma.rcs.cms.imap.message.cpim.text;

import android.test.AndroidTestCase;

import com.gsma.rcs.cms.Constants;

import junit.framework.Assert;

public class TextCpimBodyTest extends AndroidTestCase{

    public void testFromPayload(){

        StringBuilder payload = new StringBuilder();
        payload.append("Content-Type: text/plain").append(Constants.CRLFCRLF);
        payload.append("myContent");

        TextCpimBody textCpimBody = new TextCpimBody();
        textCpimBody.parseBody(payload.toString());
        Assert.assertEquals("text/plain", textCpimBody.getContentType());
        Assert.assertEquals("myContent", textCpimBody.getContent());
    }

    public void test(){

        StringBuilder expected = new StringBuilder();
        expected.append("Content-Type: myContentType").append(Constants.CRLFCRLF);
        expected.append("myContent");

        TextCpimBody textCpimBody = new TextCpimBody("myContentType", "myContent");
        String payload = textCpimBody.toPayload();
        Assert.assertEquals(expected.toString(), payload);
    }

}
