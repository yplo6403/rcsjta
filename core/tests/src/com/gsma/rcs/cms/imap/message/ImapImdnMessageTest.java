package com.gsma.rcs.cms.imap.message;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.event.exception.CmsSyncException;
import com.gsma.rcs.cms.utils.DateUtils;
import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;
import com.sonymobile.rcs.imap.Part;

import junit.framework.Assert;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

public class ImapImdnMessageTest extends AndroidTestCase {

    private ContactId mExpectedContact;
    private long mDate;
    private String mImapDate;
    private String mCpimDate;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        init();

    }

    public void init() throws Exception {
        mExpectedContact = ContactUtil.getInstance(getContext()).formatContact("+33642575779");
        mDate = System.currentTimeMillis();
        mImapDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_IMAP_DATE_FORMAT);
        mCpimDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_CPIM_DATE_FORMAT);
    }

    @SmallTest
    public void testOneToOneImdn(){

        try {
            String folderName = "myFolder";
            String from = "+33642575779";
            String to = "+33640332859";
            String direction = Constants.DIRECTION_RECEIVED;
            String messageId = "MsgfkYflzUUKA";

            Integer uid = 12;
            Part part = new Part();
            part.fromPayload(getPayload(true, from, to, direction, messageId, ImdnDocument.DELIVERY_STATUS_DELIVERED));
            ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
            metadata.getFlags().add(Flag.Seen);
            ImapMessage imapMessage = new ImapMessage(uid, metadata, part);
            imapMessage.setFolderPath(folderName);

            ImapImdnMessage imapImdnMessage = new ImapImdnMessage(imapMessage);
            Assert.assertEquals(folderName, imapImdnMessage.getFolder());
            Assert.assertEquals(uid, imapImdnMessage.getUid());
            Assert.assertTrue(imapImdnMessage.isSeen());
            Assert.assertFalse(imapImdnMessage.isDeleted());
            
            Assert.assertEquals(from, imapImdnMessage.getHeader(Constants.HEADER_FROM));
            Assert.assertEquals(to, imapImdnMessage.getHeader(Constants.HEADER_TO));
            Assert.assertEquals(mImapDate, imapImdnMessage.getHeader(Constants.HEADER_DATE));
            Assert.assertEquals("1443517760826", imapImdnMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
            Assert.assertEquals("1443517760826", imapImdnMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
            Assert.assertEquals("1443517760826", imapImdnMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
            Assert.assertEquals(direction, imapImdnMessage.getHeader(Constants.HEADER_DIRECTION));
            Assert.assertEquals("Message/CPIM", imapImdnMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

            Assert.assertEquals(ImapChatMessage.ANONYMOUS, imapImdnMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
            Assert.assertEquals(ImapChatMessage.ANONYMOUS, imapImdnMessage.getCpimMessage().getHeader(Constants.HEADER_TO));

            Assert.assertTrue(imapImdnMessage.isOneToOne());
            Assert.assertEquals(mExpectedContact, imapImdnMessage.getContact());
            Assert.assertEquals(Direction.INCOMING, imapImdnMessage.getDirection());
            Assert.assertEquals("1443517760826",imapImdnMessage.getImdnId());
            Assert.assertEquals(messageId, imapImdnMessage.getImdnDocument().getMsgId());
            Assert.assertEquals(ImdnDocument.DELIVERY_STATUS_DELIVERED, imapImdnMessage.getImdnDocument().getStatus());

            part.fromPayload(getPayload(true, from, to, direction, messageId, ImdnDocument.DELIVERY_STATUS_DISPLAYED));
            imapImdnMessage = new ImapImdnMessage(imapMessage);
            Assert.assertEquals(ImdnDocument.DELIVERY_STATUS_DISPLAYED, imapImdnMessage.getImdnDocument().getStatus());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @SmallTest
    public void testGroupChatImdn(){

        try {
            String folderName = "myFolder";
            String from = "+33642575779";
            String to = "+33640332859";
            String direction = Constants.DIRECTION_RECEIVED;
            String messageId = "MsgfkYflzUUKA";
            Integer uid = 12;
            Part part = new Part();
            part.fromPayload(getPayload(false, from, to, direction, messageId, ImdnDocument.DELIVERY_STATUS_DELIVERED));
            ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
            metadata.getFlags().add(Flag.Seen);
            ImapMessage imapMessage = new ImapMessage(uid, metadata, part);
            imapMessage.setFolderPath(folderName);

            ImapImdnMessage imapImdnMessage = new ImapImdnMessage(imapMessage);
            Assert.assertEquals(folderName, imapImdnMessage.getFolder());
            Assert.assertEquals(uid, imapImdnMessage.getUid());
            Assert.assertTrue(imapImdnMessage.isSeen());
            Assert.assertFalse(imapImdnMessage.isDeleted());

            Assert.assertEquals(from, imapImdnMessage.getHeader(Constants.HEADER_FROM));
            Assert.assertEquals(to, imapImdnMessage.getHeader(Constants.HEADER_TO));
            Assert.assertEquals(mImapDate, imapImdnMessage.getHeader(Constants.HEADER_DATE));
            Assert.assertEquals("1443517760826", imapImdnMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
            Assert.assertEquals("1443517760826", imapImdnMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
            Assert.assertEquals("1443517760826", imapImdnMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
            Assert.assertEquals(direction, imapImdnMessage.getHeader(Constants.HEADER_DIRECTION));
            Assert.assertEquals("Message/CPIM", imapImdnMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

            Assert.assertEquals("+33642575779", imapImdnMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
            Assert.assertEquals(ImapChatMessage.ANONYMOUS, imapImdnMessage.getCpimMessage().getHeader(Constants.HEADER_TO));

            Assert.assertFalse(imapImdnMessage.isOneToOne());
            Assert.assertEquals(mExpectedContact, imapImdnMessage.getContact());
            Assert.assertEquals(Direction.INCOMING, imapImdnMessage.getDirection());
            Assert.assertEquals("1443517760826",imapImdnMessage.getImdnId());
            Assert.assertEquals(messageId, imapImdnMessage.getImdnDocument().getMsgId());
            Assert.assertEquals(ImdnDocument.DELIVERY_STATUS_DELIVERED, imapImdnMessage.getImdnDocument().getStatus());


            part.fromPayload(getPayload(false, from, to, direction, messageId, ImdnDocument.DELIVERY_STATUS_DISPLAYED));
            imapImdnMessage = new ImapImdnMessage(imapMessage);
            Assert.assertEquals(ImdnDocument.DELIVERY_STATUS_DISPLAYED, imapImdnMessage.getImdnDocument().getStatus());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    public String getPayload(boolean isOneToOne, String headerFrom, String headerTo, String direction, String messageId, String status){

        String headerFromCpim = isOneToOne ? ImapImdnMessage.ANONYMOUS : headerFrom;

        return new StringBuilder()
                .append("From: ").append(headerFrom).append(Constants.CRLF)
                .append("To: ").append(headerTo).append(Constants.CRLF)
                .append("Date: ").append(mImapDate).append(Constants.CRLF)
                .append("Conversation-ID: 1443517760826").append(Constants.CRLF)
                .append("Contribution-ID: 1443517760826").append(Constants.CRLF)
                .append("IMDN-Message-ID: 1443517760826").append(Constants.CRLF)
                .append("Message-Direction: ").append(direction).append(Constants.CRLF)
                .append("Content-Type: Message/CPIM").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("From: " + headerFromCpim ).append(Constants.CRLF)
                .append("To: <sip:anonymous@anonymous.invalid>").append(Constants.CRLF)
                .append("NS: imdn <urn:ietf:params:imdn>").append(Constants.CRLF)
                .append("NS: rcs <http://www.gsma.com>").append(Constants.CRLF)
                .append("imdn.Message-ID: 1443517760826").append(Constants.CRLF)
                .append("DateTime: ").append(mCpimDate).append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("Content-Type: message/imdn+xml").append(Constants.CRLF)
                .append("Content-Disposition: notifiaction").append(Constants.CRLF)
                .append("Content-Length: 257").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(Constants.CRLF)
                .append("<imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">").append(Constants.CRLF)
                .append("<message-id>").append(messageId).append("</message-id>").append(Constants.CRLF)
                .append("<datetime>2015-04-23T08:55:52.000Z</datetime>").append(Constants.CRLF)
                .append("<delivery-notification><status><" + status + "/></status></delivery-notification>").append(Constants.CRLF)
                .append("</imdn>").toString();
    }
}