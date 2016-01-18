package com.gsma.rcs.cms.imap.message;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.event.exception.CmsSyncException;
import com.gsma.rcs.cms.utils.DateUtils;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;
import com.sonymobile.rcs.imap.Part;

import junit.framework.Assert;

public class ImapChatMessageTest extends AndroidTestCase {

    private ContactId mExpectedContact;
    private long mDate;
    private String mImapDate;
    private String mCpimDate;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        init();
    }

    public void init() throws Exception{
        mExpectedContact = ContactUtil.getInstance(getContext()).formatContact("+33642575779");
        mDate = System.currentTimeMillis();
        mImapDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_IMAP_DATE_FORMAT);
        mCpimDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_CPIM_DATE_FORMAT);
    }

    @SmallTest
    public void testOneToOneChatMessage(){

        try {

            String folderName = "myFolder";
            String headerFrom = "+33642575779";
            String headerTo = "+33640332859";
            String direction = Constants.DIRECTION_RECEIVED;
            String contributionId = "1443517760826";

            Integer uid = 12;
            Part part = new Part();
            part.fromPayload(getPayload(true, headerFrom, headerTo, contributionId, direction));
            ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
            metadata.getFlags().add(Flag.Seen);
            ImapMessage imapMessage = new ImapMessage(uid, metadata, part);
            imapMessage.setFolderPath(folderName);

            ImapChatMessage imapChatMessage = new ImapChatMessage(imapMessage);
            Assert.assertEquals(folderName, imapChatMessage.getFolder());
            Assert.assertEquals(uid, imapChatMessage.getUid());
            Assert.assertTrue(imapChatMessage.isSeen());
            Assert.assertFalse(imapChatMessage.isDeleted());

            Assert.assertEquals(headerFrom, imapChatMessage.getHeader(Constants.HEADER_FROM));
            Assert.assertEquals(headerTo, imapChatMessage.getHeader(Constants.HEADER_TO));
            Assert.assertEquals(mImapDate, imapChatMessage.getHeader(Constants.HEADER_DATE));
            Assert.assertEquals(contributionId, imapChatMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
            Assert.assertEquals(contributionId, imapChatMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
            Assert.assertEquals(contributionId, imapChatMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
            Assert.assertEquals(direction, imapChatMessage.getHeader(Constants.HEADER_DIRECTION));
            Assert.assertEquals("Message/CPIM", imapChatMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

            Assert.assertEquals(ImapChatMessage.ANONYMOUS, imapChatMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
            Assert.assertEquals(ImapChatMessage.ANONYMOUS, imapChatMessage.getCpimMessage().getHeader(Constants.HEADER_TO));

            Assert.assertTrue(imapChatMessage.isOneToOne());
            Assert.assertEquals(mExpectedContact, imapChatMessage.getContact());
            Assert.assertEquals(Direction.INCOMING, imapChatMessage.getDirection());
            Assert.assertEquals(contributionId, imapChatMessage.getChatId());
            Assert.assertEquals("Hello", imapChatMessage.getText());

        } catch (CmsSyncException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @SmallTest
    public void testGroupChatMessage(){

        try {
            String folderName = "myFolder";
            String headerFrom = "+33642575779";
            String headerTo = "+33640332859";
            String direction = Constants.DIRECTION_RECEIVED;
            String contributionId = "1443517760826";

            Integer uid = 12;
            Part part = new Part();
            part.fromPayload(getPayload(false, headerFrom, headerTo, contributionId, direction));
            ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
            metadata.getFlags().add(Flag.Seen);
            ImapMessage imapMessage = new ImapMessage(uid, metadata, part);
            imapMessage.setFolderPath(folderName);

            ImapChatMessage imapChatMessage = new ImapChatMessage(imapMessage);
            Assert.assertEquals(folderName, imapChatMessage.getFolder());
            Assert.assertEquals(uid, imapChatMessage.getUid());
            Assert.assertTrue(imapChatMessage.isSeen());
            Assert.assertFalse(imapChatMessage.isDeleted());

            Assert.assertEquals(headerFrom, imapChatMessage.getHeader(Constants.HEADER_FROM));
            Assert.assertEquals(headerTo, imapChatMessage.getHeader(Constants.HEADER_TO));
            Assert.assertEquals(mImapDate, imapChatMessage.getHeader(Constants.HEADER_DATE));
            Assert.assertEquals(contributionId, imapChatMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
            Assert.assertEquals(contributionId, imapChatMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
            Assert.assertEquals(contributionId, imapChatMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
            Assert.assertEquals(direction, imapChatMessage.getHeader(Constants.HEADER_DIRECTION));
            Assert.assertEquals("Message/CPIM", imapChatMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

            Assert.assertEquals(headerFrom, imapChatMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
            Assert.assertEquals(ImapChatMessage.ANONYMOUS, imapChatMessage.getCpimMessage().getHeader(Constants.HEADER_TO));

            Assert.assertFalse(imapChatMessage.isOneToOne());
            Assert.assertEquals(mExpectedContact, imapChatMessage.getContact());
            Assert.assertEquals(Direction.INCOMING, imapChatMessage.getDirection());
            Assert.assertEquals(contributionId, imapChatMessage.getChatId());
            Assert.assertEquals("Hello", imapChatMessage.getText());
        } catch (CmsSyncException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    public String getPayload(boolean isOneToOne,  String headerFrom, String headerTo, String contributionId, String direction){

        String headerFromCpim = isOneToOne ? ImapChatMessage.ANONYMOUS : headerFrom;

        return  new StringBuilder()
                .append("From: ").append(headerFrom).append(Constants.CRLF)
                .append("To: ").append(headerTo).append(Constants.CRLF)
                .append("Date: ").append(mImapDate).append(Constants.CRLF)
                .append("Conversation-ID: ").append(contributionId).append(Constants.CRLF)
                .append("Contribution-ID: ").append(contributionId).append(Constants.CRLF)
                .append("IMDN-Message-ID: ").append(contributionId).append(Constants.CRLF)
                .append("Message-Direction: ").append(direction).append(Constants.CRLF)
                .append("Content-Type: Message/CPIM").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("From: " + headerFromCpim ).append(Constants.CRLF)
                .append("To: <sip:anonymous@anonymous.invalid>").append(Constants.CRLF)
                .append("NS: imdn <urn:ietf:params:imdn>").append(Constants.CRLF)
                .append("NS: rcs <http://www.gsma.com>").append(Constants.CRLF)
                .append("imdn.Message-ID: ").append(contributionId).append(Constants.CRLF)
                .append("DateTime: ").append(mCpimDate).append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("Content-Type: text/plain; charset=utf-8").append(Constants.CRLF)
                .append("Content-Length: 5").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("Hello").toString();
    }
}
