/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.cms.protocol.message;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncImdnFormatException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.utils.DateUtils;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.imaplib.imap.ImapMessageMetadata;
import com.gsma.rcs.imaplib.imap.Part;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;

public class ImapImdnMessageTest extends AndroidTestCase {

    private ContactId mExpectedContact;
    private String mImapDate;
    private String mCpimDate;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ContactUtil contactUtil = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        mExpectedContact = contactUtil.formatContact("+33642575779");
        long date = NtpTrustedTime.currentTimeMillis();
        mImapDate = DateUtils.getDateAsString(date, DateUtils.CMS_IMAP_DATE_FORMAT);
        mCpimDate = DateUtils.getDateAsString(date, DateUtils.CMS_CPIM_DATE_FORMAT);
    }

    @SmallTest
    public void testOneToOneImdn() throws CmsSyncMissingHeaderException,
            CmsSyncHeaderFormatException, CmsSyncImdnFormatException {
        String folderName = "myFolder";
        String from = "+33642575779";
        String to = "+33640332859";
        String direction = Constants.DIRECTION_RECEIVED;
        String messageId = "MsgfkYflzUUKA";
        Integer uid = 12;
        Part part = new Part();
        part.fromPayload(getPayload(true, from, to, direction, messageId,
                ImdnDocument.DeliveryStatus.DELIVERED));
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
        Assert.assertEquals("1443517760826",
                imapImdnMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
        Assert.assertEquals("1443517760826",
                imapImdnMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
        Assert.assertEquals("1443517760826",
                imapImdnMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
        Assert.assertEquals(direction, imapImdnMessage.getHeader(Constants.HEADER_DIRECTION));
        Assert.assertEquals("Message/CPIM",
                imapImdnMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

        Assert.assertEquals(ImapChatMessage.ANONYMOUS,
                imapImdnMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
        Assert.assertEquals(ImapChatMessage.ANONYMOUS,
                imapImdnMessage.getCpimMessage().getHeader(Constants.HEADER_TO));

        Assert.assertTrue(imapImdnMessage.isOneToOne());
        Assert.assertEquals(mExpectedContact, imapImdnMessage.getContact());
        Assert.assertEquals(Direction.INCOMING, imapImdnMessage.getDirection());
        Assert.assertEquals("1443517760826", imapImdnMessage.getImdnId());
        Assert.assertEquals(messageId, imapImdnMessage.getImdnDocument().getMsgId());
        Assert.assertEquals(ImdnDocument.DeliveryStatus.DELIVERED, imapImdnMessage
                .getImdnDocument().getStatus());

        part.fromPayload(getPayload(true, from, to, direction, messageId,
                ImdnDocument.DeliveryStatus.DISPLAYED));
        imapImdnMessage = new ImapImdnMessage(imapMessage);
        Assert.assertEquals(ImdnDocument.DeliveryStatus.DISPLAYED, imapImdnMessage
                .getImdnDocument().getStatus());
    }

    @SmallTest
    public void testGroupChatImdn() throws CmsSyncMissingHeaderException,
            CmsSyncHeaderFormatException, CmsSyncImdnFormatException {
        String folderName = "myFolder";
        String from = "+33642575779";
        String to = "+33640332859";
        String direction = Constants.DIRECTION_RECEIVED;
        String messageId = "MsgfkYflzUUKA";
        Integer uid = 12;
        Part part = new Part();
        part.fromPayload(getPayload(false, from, to, direction, messageId,
                ImdnDocument.DeliveryStatus.DELIVERED));
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
        Assert.assertEquals("1443517760826",
                imapImdnMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
        Assert.assertEquals("1443517760826",
                imapImdnMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
        Assert.assertEquals("1443517760826",
                imapImdnMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
        Assert.assertEquals(direction, imapImdnMessage.getHeader(Constants.HEADER_DIRECTION));
        Assert.assertEquals("Message/CPIM",
                imapImdnMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

        Assert.assertEquals("+33642575779",
                imapImdnMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
        Assert.assertEquals(ImapChatMessage.ANONYMOUS,
                imapImdnMessage.getCpimMessage().getHeader(Constants.HEADER_TO));

        Assert.assertFalse(imapImdnMessage.isOneToOne());
        Assert.assertEquals(mExpectedContact, imapImdnMessage.getContact());
        Assert.assertEquals(Direction.INCOMING, imapImdnMessage.getDirection());
        Assert.assertEquals("1443517760826", imapImdnMessage.getImdnId());
        Assert.assertEquals(messageId, imapImdnMessage.getImdnDocument().getMsgId());
        Assert.assertEquals(ImdnDocument.DeliveryStatus.DELIVERED, imapImdnMessage
                .getImdnDocument().getStatus());

        part.fromPayload(getPayload(false, from, to, direction, messageId,
                ImdnDocument.DeliveryStatus.DISPLAYED));
        imapImdnMessage = new ImapImdnMessage(imapMessage);
        Assert.assertEquals(ImdnDocument.DeliveryStatus.DISPLAYED, imapImdnMessage
                .getImdnDocument().getStatus());
    }

    public String getPayload(boolean isOneToOne, String headerFrom, String headerTo,
            String direction, String messageId, ImdnDocument.DeliveryStatus status) {
        String headerFromCpim = isOneToOne ? ImapImdnMessage.ANONYMOUS : headerFrom;
        return "From: " + headerFrom + Constants.CRLF + "To: " + headerTo + Constants.CRLF
                + "Date: " + mImapDate + Constants.CRLF + "Conversation-ID: 1443517760826"
                + Constants.CRLF + "Contribution-ID: 1443517760826" + Constants.CRLF
                + "IMDN-Message-ID: 1443517760826" + Constants.CRLF + "Message-Direction: "
                + direction + Constants.CRLF + "Content-Type: Message/CPIM" + Constants.CRLF
                + Constants.CRLF + "From: " + headerFromCpim + Constants.CRLF
                + "To: <sip:anonymous@anonymous.invalid>" + Constants.CRLF
                + "NS: imdn <urn:ietf:params:imdn>" + Constants.CRLF
                + "NS: rcs <http://www.gsma.com>" + Constants.CRLF
                + "imdn.Message-ID: 1443517760826" + Constants.CRLF + "DateTime: " + mCpimDate
                + Constants.CRLF + Constants.CRLF + "Content-Type: message/imdn+xml"
                + Constants.CRLF + "Content-Disposition: notifiaction" + Constants.CRLF
                + "Content-Length: 257" + Constants.CRLF + Constants.CRLF
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + Constants.CRLF
                + "<imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">" + Constants.CRLF + "<message-id>"
                + messageId + "</message-id>" + Constants.CRLF
                + "<datetime>2015-04-23T08:55:52.000Z</datetime>" + Constants.CRLF
                + "<delivery-notification><status><" + status
                + "/></status></delivery-notification>" + Constants.CRLF + "</imdn>";
    }
}
