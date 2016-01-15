package com.gsma.rcs.cms.imap.message;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.event.exception.CmsSyncException;
import com.gsma.rcs.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.cms.imap.message.cpim.multipart.MultipartCpimBody;
import com.gsma.rcs.cms.imap.message.cpim.text.TextCpimBody;
import com.gsma.rcs.cms.utils.DateUtils;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;
import com.sonymobile.rcs.imap.Part;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class ImapMmsMessageTest extends AndroidTestCase {

    private String mPayload;
    private String mBoundary;
    private long mDate;
    private String mImapDate;
    private String mCpimDate;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        com.gsma.services.rcs.contact.ContactUtil.getInstance(getContext());

        mBoundary = "boundary_1446218793256";
        mDate = System.currentTimeMillis();
        mImapDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_IMAP_DATE_FORMAT);
        mCpimDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_CPIM_DATE_FORMAT);

        mPayload = new StringBuilder()
                .append("From: +33642575779").append(Constants.CRLF)
                .append("To: +33640332859").append(Constants.CRLF)
                .append("Date: ").append(mImapDate).append(Constants.CRLF)
                .append("Conversation-ID: 1443517760826").append(Constants.CRLF)
                .append("Contribution-ID: 1443517760826").append(Constants.CRLF)
                .append("Message-ID: myMmsId").append(Constants.CRLF)
                .append("IMDN-Message-ID: 1443517760826").append(Constants.CRLF)
                .append("Message-Direction: received").append(Constants.CRLF)
                .append("Message-Context: multimedia-message").append(Constants.CRLF)
                .append("Content-Type: Message/CPIM").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("From: +33642575779").append(Constants.CRLF)
                .append("To: +33640332859").append(Constants.CRLF)
                .append("DateTime: ").append(mCpimDate).append(Constants.CRLF)
                .append("NS: imdn <urn:ietf:params:imdn>").append(Constants.CRLF)
                .append("NS: rcs <http://www.gsma.com>").append(Constants.CRLF)
                .append("imdn.Message-ID: 1443517760826").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("Content-Type: Multipart/Related;boundary=\"boundary_1446218793256\";").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("--boundary_1446218793256").append(Constants.CRLF)
                .append("Content-Type: text/plain").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("myContent").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("--boundary_1446218793256").append(Constants.CRLF)
                .append("Content-Type: text/plain; charset=utf-8").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("1").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("--boundary_1446218793256--").toString();

    }

    @SmallTest
    public void testFromPayload(){

        try {
            ImapMessage rawMessage = new ImapMessage();
            rawMessage.fromPayload(mPayload);
            ImapMmsMessage imapMmsMessage = new ImapMmsMessage(rawMessage);
            Assert.assertEquals("+33642575779", imapMmsMessage.getHeader(Constants.HEADER_FROM));
            Assert.assertEquals("+33640332859", imapMmsMessage.getHeader(Constants.HEADER_TO));
            Assert.assertEquals(mImapDate, imapMmsMessage.getHeader(Constants.HEADER_DATE));
            Assert.assertEquals("1443517760826", imapMmsMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
            Assert.assertEquals("1443517760826", imapMmsMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
            Assert.assertEquals("myMmsId", imapMmsMessage.getHeader(Constants.HEADER_MESSAGE_ID));
            Assert.assertEquals("1443517760826", imapMmsMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
            Assert.assertEquals("received", imapMmsMessage.getHeader(Constants.HEADER_DIRECTION));
            Assert.assertEquals("multimedia-message", imapMmsMessage.getHeader(Constants.HEADER_MESSAGE_CONTEXT));
            Assert.assertEquals("Message/CPIM", imapMmsMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

            Assert.assertEquals("+33642575779", imapMmsMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
            Assert.assertEquals("+33640332859", imapMmsMessage.getCpimMessage().getHeader(Constants.HEADER_TO));
            Assert.assertEquals("1443517760826", imapMmsMessage.getCpimMessage().getHeader("imdn.Message-ID"));
            Assert.assertEquals(mCpimDate, imapMmsMessage.getCpimMessage().getHeader("DateTime"));

            MultipartCpimBody cpimBody = (MultipartCpimBody)imapMmsMessage.getCpimMessage().getBody();

            Assert.assertEquals("Multipart/Related;boundary=\"boundary_1446218793256\";", cpimBody.getContentType());
            Assert.assertEquals(2, cpimBody.getParts().size());

            MultipartCpimBody.Part part;

            part = cpimBody.getParts().get(0);
            Assert.assertEquals("text/plain", part.getHeader(Constants.HEADER_CONTENT_TYPE));
            Assert.assertEquals("myContent", part.getContent());

            part = cpimBody.getParts().get(1);
            Assert.assertEquals("text/plain; charset=utf-8", part.getHeader(Constants.HEADER_CONTENT_TYPE));
            Assert.assertEquals("1", part.getContent());

        } catch (CmsSyncException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }

    @SmallTest
    public void testToPayload(){

        List<MmsPart> parts = new ArrayList<>();
        parts.add(new MmsPart("myMmsId", ContactUtil.createContactIdFromTrustedData("+33642575779"), "text/plain", "myContent"));
        parts.add(new MmsPart("myMmsId", ContactUtil.createContactIdFromTrustedData("+33642575779"), "text/plain; charset=utf-8", "1"));
        ImapMmsMessage imapMmsMessage = new ImapMmsMessage(
                getContext(),
                "+33642575779",
                "+33640332859",
                "received",
                mDate,
                null,
                "1443517760826",
                "1443517760826",
                "1443517760826",
                "myMmsId",
                parts);

        ((MultipartCpimBody)imapMmsMessage.getCpimMessage().getBody()).setBoundary(mBoundary);

        Assert.assertEquals(mPayload, imapMmsMessage.toPayload());

    }
}
