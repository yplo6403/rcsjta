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
import com.gsma.rcs.core.cms.event.exception.CmsSyncException;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.core.cms.utils.DateUtils;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.imaplib.imap.ImapMessageMetadata;
import com.gsma.rcs.imaplib.imap.Part;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

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

    public void init() throws Exception {
        mExpectedContact = ContactUtil.getInstance(getContext()).formatContact("+33642575779");
        mDate = NtpTrustedTime.currentTimeMillis();
        mImapDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_IMAP_DATE_FORMAT);
        mCpimDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_CPIM_DATE_FORMAT);
    }

    @SmallTest
    public void testOneToOneChatMessage() {

        try {

            String folderName = "myFolder";
            String headerFrom = "tel:+33642575779";
            String headerTo = "tel:+33640332859";
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
            Assert.assertEquals(contributionId,
                    imapChatMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
            Assert.assertEquals(contributionId,
                    imapChatMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
            Assert.assertEquals(contributionId,
                    imapChatMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
            Assert.assertEquals(direction, imapChatMessage.getHeader(Constants.HEADER_DIRECTION));
            Assert.assertEquals("Message/CPIM",
                    imapChatMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

            Assert.assertEquals(ImapChatMessage.ANONYMOUS, imapChatMessage.getCpimMessage()
                    .getHeader(Constants.HEADER_FROM));
            Assert.assertEquals(ImapChatMessage.ANONYMOUS, imapChatMessage.getCpimMessage()
                    .getHeader(Constants.HEADER_TO));

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
    public void testGroupChatMessage() {

        try {
            String folderName = "myFolder";
            String headerFrom = "tel:+33642575779";
            String headerTo = "tel:+33640332859";
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
            Assert.assertEquals(contributionId,
                    imapChatMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
            Assert.assertEquals(contributionId,
                    imapChatMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
            Assert.assertEquals(contributionId,
                    imapChatMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
            Assert.assertEquals(direction, imapChatMessage.getHeader(Constants.HEADER_DIRECTION));
            Assert.assertEquals("Message/CPIM",
                    imapChatMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

            Assert.assertEquals("<" + headerFrom + ">",
                    imapChatMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
            Assert.assertEquals(ImapChatMessage.ANONYMOUS, imapChatMessage.getCpimMessage()
                    .getHeader(Constants.HEADER_TO));

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

    public String getPayload(boolean isOneToOne, String headerFrom, String headerTo,
            String contributionId, String direction) {

        String headerFromCpim = isOneToOne ? ImapChatMessage.ANONYMOUS : "<" + headerFrom + ">";

        return new StringBuilder().append("From: ").append(headerFrom).append(Constants.CRLF)
                .append("To: ").append(headerTo).append(Constants.CRLF).append("Date: ")
                .append(mImapDate).append(Constants.CRLF).append("Conversation-ID: ")
                .append(contributionId).append(Constants.CRLF).append("Contribution-ID: ")
                .append(contributionId).append(Constants.CRLF).append("IMDN-Message-ID: ")
                .append(contributionId).append(Constants.CRLF).append("Message-Direction: ")
                .append(direction).append(Constants.CRLF).append("Content-Type: Message/CPIM")
                .append(Constants.CRLF).append(Constants.CRLF).append("From: " + headerFromCpim)
                .append(Constants.CRLF).append("To: <sip:anonymous@anonymous.invalid>")
                .append(Constants.CRLF).append("NS: imdn <urn:ietf:params:imdn>")
                .append(Constants.CRLF).append("NS: rcs <http://www.gsma.com>")
                .append(Constants.CRLF).append("imdn.Message-ID: ").append(contributionId)
                .append(Constants.CRLF).append("DateTime: ").append(mCpimDate)
                .append(Constants.CRLF).append(Constants.CRLF)
                .append("Content-Type: text/plain; charset=utf-8").append(Constants.CRLF)
                .append("Content-Length: 5").append(Constants.CRLF).append(Constants.CRLF)
                .append("Hello").toString();
    }
}
