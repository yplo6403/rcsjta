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
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncXmlFormatException;
import com.gsma.rcs.core.cms.utils.DateUtils;
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

public class ImapChatMessageTest extends AndroidTestCase {

    private final static String ANONYMOUS = "<sip:anonymous@anonymous.invalid>";

    private ContactId mExpectedContact;
    private String mImapDate;
    private String mCpimDate;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        init();
    }

    public void init() throws Exception {
        ContactUtil contactUtil = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        mExpectedContact = contactUtil.formatContact("+33642575779");
        long date = NtpTrustedTime.currentTimeMillis();
        mImapDate = DateUtils.getDateAsString(date, DateUtils.CMS_IMAP_DATE_FORMAT);
        mCpimDate = DateUtils.getDateAsString(date, DateUtils.CMS_CPIM_DATE_FORMAT);
    }

    @SmallTest
    public void testOneToOneChatMessage() throws CmsSyncMissingHeaderException,
            CmsSyncHeaderFormatException, CmsSyncXmlFormatException {
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

        ImapChatMessage imapChatMessage = new ImapChatMessage(imapMessage, mExpectedContact);
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
        Assert.assertEquals("message/cpim",
                imapChatMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

        Assert.assertEquals(ANONYMOUS,
                imapChatMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
        Assert.assertEquals(ANONYMOUS,
                imapChatMessage.getCpimMessage().getHeader(Constants.HEADER_TO));

        Assert.assertEquals(mExpectedContact, imapChatMessage.getContact());
        Assert.assertEquals(Direction.INCOMING, imapChatMessage.getDirection());
        Assert.assertEquals(contributionId, imapChatMessage.getChatId());
        Assert.assertEquals("Hello", imapChatMessage.getText());
    }

    @SmallTest
    public void testGroupChatMessage() throws CmsSyncMissingHeaderException,
            CmsSyncHeaderFormatException, CmsSyncXmlFormatException {
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

        ImapChatMessage imapChatMessage = new ImapChatMessage(imapMessage, mExpectedContact);
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
        Assert.assertEquals("message/cpim",
                imapChatMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

        Assert.assertEquals("<" + headerFrom + ">",
                imapChatMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
        Assert.assertEquals(ANONYMOUS,
                imapChatMessage.getCpimMessage().getHeader(Constants.HEADER_TO));

        Assert.assertEquals(mExpectedContact, imapChatMessage.getContact());
        Assert.assertEquals(Direction.INCOMING, imapChatMessage.getDirection());
        Assert.assertEquals(contributionId, imapChatMessage.getChatId());
        Assert.assertEquals("Hello", imapChatMessage.getText());
    }

    public String getPayload(boolean isOneToOne, String headerFrom, String headerTo,
            String contributionId, String direction) {
        String headerFromCpim = isOneToOne ? ANONYMOUS : "<" + headerFrom + ">";
        return "From: " + headerFrom + Constants.CRLF + "To: " + headerTo + Constants.CRLF
                + "Date: " + mImapDate + Constants.CRLF + "Conversation-ID: " + contributionId
                + Constants.CRLF + "Contribution-ID: " + contributionId + Constants.CRLF
                + "IMDN-Message-ID: " + contributionId + Constants.CRLF + "Message-Direction: "
                + direction + Constants.CRLF + "Content-Type: message/cpim" + Constants.CRLF
                + Constants.CRLF + "From: " + headerFromCpim + Constants.CRLF
                + "To: <sip:anonymous@anonymous.invalid>" + Constants.CRLF
                + "NS: imdn <urn:ietf:params:imdn>" + Constants.CRLF
                + "NS: rcs <http://www.gsma.com>" + Constants.CRLF + "imdn.Message-ID: "
                + contributionId + Constants.CRLF + "DateTime: " + mCpimDate + Constants.CRLF
                + Constants.CRLF + "Content-Type: text/plain; charset=utf-8" + Constants.CRLF
                + "Content-Length: 5" + Constants.CRLF + Constants.CRLF + "Hello";
    }
}
