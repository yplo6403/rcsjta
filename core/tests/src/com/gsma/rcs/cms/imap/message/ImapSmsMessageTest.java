package com.gsma.rcs.cms.imap.message;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.cpim.text.TextCpimBody;
import com.gsma.rcs.cms.utils.DateUtils;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;
import com.sonymobile.rcs.imap.Part;

import junit.framework.Assert;

public class ImapSmsMessageTest extends AndroidTestCase {

    private String mPayload;
    private long mDate;
    private String mImapDate;
    private String mCpimDate;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mDate = System.currentTimeMillis();
        mImapDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_IMAP_DATE_FORMAT);
        mCpimDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_CPIM_DATE_FORMAT);

        mPayload= new StringBuilder()
                .append("From: +33642575779").append(Constants.CRLF)
                .append("To: +33640332859").append(Constants.CRLF)
                .append("Date: ").append(mImapDate).append(Constants.CRLF)
                .append("Conversation-ID: 1443517760826").append(Constants.CRLF)
                .append("Contribution-ID: 1443517760826").append(Constants.CRLF)
                .append("IMDN-Message-ID: 1443517760826").append(Constants.CRLF)
                .append("Message-Direction: received").append(Constants.CRLF)
                .append("Message-Correlator: 1").append(Constants.CRLF)
                .append("Message-Context: pager-message").append(Constants.CRLF)
                .append("Content-Type: Message/CPIM").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("From: +33642575779").append(Constants.CRLF)
                .append("To: +33640332859").append(Constants.CRLF)
                .append("NS: imdn <urn:ietf:params:imdn>").append(Constants.CRLF)
                .append("NS: rcs <http://www.gsma.com>").append(Constants.CRLF)
                .append("imdn.Message-ID: 1443517760826").append(Constants.CRLF)
                .append("DateTime: ").append(mCpimDate).append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("Content-Type: text/plain; charset=utf-8").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("1").toString();

    }

    @SmallTest
    public void testFromPayload(){
        
        String folderName = "myFolder";

        Integer uid = 12;                
        Part part = new Part();
        part.fromPayload(mPayload);
        ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
        metadata.getFlags().add(Flag.Seen);
        ImapMessage imapMessage =  new ImapMessage(uid, metadata, part);
        imapMessage.setFolderPath(folderName);    
                
        ImapSmsMessage imapSmsMessage = new ImapSmsMessage(imapMessage);
        Assert.assertEquals(folderName,imapSmsMessage.getFolder());
        Assert.assertEquals(uid, imapSmsMessage.getUid());
        Assert.assertTrue(imapSmsMessage.isSeen());
        Assert.assertFalse(imapSmsMessage.isDeleted());

        Assert.assertEquals("+33642575779", imapSmsMessage.getHeader(Constants.HEADER_FROM));
        Assert.assertEquals("+33640332859", imapSmsMessage.getHeader(Constants.HEADER_TO));
        Assert.assertEquals(mImapDate, imapSmsMessage.getHeader(Constants.HEADER_DATE));
        Assert.assertEquals("1443517760826", imapSmsMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
        Assert.assertEquals("1443517760826", imapSmsMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
        Assert.assertEquals("1443517760826", imapSmsMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
        Assert.assertEquals("received", imapSmsMessage.getHeader(Constants.HEADER_DIRECTION));
        Assert.assertEquals("1", imapSmsMessage.getHeader(Constants.HEADER_MESSAGE_CORRELATOR));
        Assert.assertEquals("pager-message", imapSmsMessage.getHeader(Constants.HEADER_MESSAGE_CONTEXT));
        Assert.assertEquals("Message/CPIM", imapSmsMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

        Assert.assertEquals("+33642575779", imapSmsMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
        Assert.assertEquals("+33640332859", imapSmsMessage.getCpimMessage().getHeader(Constants.HEADER_TO));
        Assert.assertEquals("1443517760826", imapSmsMessage.getCpimMessage().getHeader("imdn.Message-ID"));
        Assert.assertEquals(mCpimDate, imapSmsMessage.getCpimMessage().getHeader("DateTime"));

        Assert.assertEquals("1", ((TextCpimBody) imapSmsMessage.getCpimMessage().getBody()).getContent());
    }

    @SmallTest
    public void testToPayload(){

        ImapSmsMessage imapSmsMessage = new ImapSmsMessage(
                "+33642575779",
                "+33640332859",
                "received",
                mDate,
                "1",
                "1443517760826",
                "1443517760826",
                "1443517760826"
        );

        Assert.assertEquals(mPayload, imapSmsMessage.toPayload());
    }

}
