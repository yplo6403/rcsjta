package com.gsma.rcs.cms.imap.message;

import android.test.AndroidTestCase;

import com.gsma.rcs.cms.Constants;

public class HeaderPartTest extends AndroidTestCase {

    public static StringBuilder headerContent = new StringBuilder();
    static{
        headerContent.append("From: +33643202148").append(Constants.CRLF);
        headerContent.append("To: +33643209850").append(Constants.CRLF);
        headerContent.append("Date: ven., 30 10 2015 16:26:16.0 +0100").append(Constants.CRLF);
        headerContent.append("Conversation-ID: 1446218776000").append(Constants.CRLF);
        headerContent.append("Contribution-ID: 1446218776000").append(Constants.CRLF);
        headerContent.append("Message-ID: 103015261610014200000").append(Constants.CRLF);
        headerContent.append("IMDN-Message-ID: 1446218776000").append(Constants.CRLF);
        headerContent.append("Message-Direction: received").append(Constants.CRLF);
        headerContent.append("Message-Context: \"multimedia-message\"").append(Constants.CRLF);
        headerContent.append("Content-Type: Message/CPIM").append(Constants.CRLF);
    }

    public void test(){
        HeaderPart headers = new HeaderPart();
        headers.addHeader(Constants.HEADER_FROM, "+33643202148");
        headers.addHeader(Constants.HEADER_TO, "+33643209850");
        headers.addHeader(Constants.HEADER_DATE, "ven., 30 10 2015 16:26:16.0 +0100");
        headers.addHeader(Constants.HEADER_CONVERSATION_ID, "1446218776000");
        headers.addHeader(Constants.HEADER_CONTRIBUTION_ID, "1446218776000");
        headers.addHeader(Constants.HEADER_MESSAGE_ID, "103015261610014200000");
        headers.addHeader(headers.new Header(Constants.HEADER_IMDN_MESSAGE_ID, "1446218776000"));
        headers.addHeader(headers.new Header(Constants.HEADER_DIRECTION, "received"));
        headers.addHeader(Constants.HEADER_MESSAGE_CONTEXT, "\"multimedia-message\"");
        headers.addHeader(Constants.HEADER_CONTENT_TYPE, "Message/CPIM");
        assertEquals(headerContent.toString(), headers.toString());
    }
}
