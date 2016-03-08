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
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.core.cms.utils.DateUtils;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.imaplib.imap.ImapMessageMetadata;
import com.gsma.rcs.imaplib.imap.Part;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.List;

import junit.framework.Assert;

public class ImapGroupStateMessageTest extends AndroidTestCase {

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

    // @formatter:off
    @SmallTest
    public void testGroupStateMessage() {

        try {
            String folderName = "myFolder";

            Integer uid = 12;
            Part part = new Part();
            part.fromPayload(getPayload());
            ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
            metadata.getFlags().add(Flag.Seen);
            ImapMessage imapMessage = new ImapMessage(uid, metadata, part);
            imapMessage.setFolderPath(folderName);

            ImapGroupStateMessage imapGroupStateMessage = new ImapGroupStateMessage(imapMessage);
            Assert.assertEquals(folderName, imapGroupStateMessage.getFolder());
            Assert.assertEquals(uid, imapGroupStateMessage.getUid());
            Assert.assertTrue(imapGroupStateMessage.isSeen());
            Assert.assertFalse(imapGroupStateMessage.isDeleted());

            Assert.assertEquals("+33642575779", imapGroupStateMessage.getHeader(Constants.HEADER_FROM));
            Assert.assertEquals("+33640332859", imapGroupStateMessage.getHeader(Constants.HEADER_TO));
            Assert.assertEquals(mImapDate, imapGroupStateMessage.getHeader(Constants.HEADER_DATE));
            Assert.assertEquals("1443517760826", imapGroupStateMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
            Assert.assertEquals("1443517760826", imapGroupStateMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
            Assert.assertEquals("1443517760826", imapGroupStateMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
            Assert.assertEquals("sent", imapGroupStateMessage.getHeader(Constants.HEADER_DIRECTION));
            Assert.assertEquals(Constants.APPLICATION_GROUP_STATE, imapGroupStateMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

            Assert.assertEquals("1443517760826", imapGroupStateMessage.getChatId());
            Assert.assertEquals("sip:da9274453@company.com", imapGroupStateMessage.getRejoinId());
            List<ContactId> contacts = imapGroupStateMessage.getParticipants();
            Assert.assertEquals(3, contacts.size());
            Assert.assertEquals(ContactUtil.getInstance(getContext()).formatContact("+16135551210"), contacts.get(0));
            Assert.assertEquals(ContactUtil.getInstance(getContext()).formatContact("+16135551211"), contacts.get(1));
            Assert.assertEquals(ContactUtil.getInstance(getContext()).formatContact("+16135551212"), contacts.get(2));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }


    public String getPayload() {

        return new StringBuilder()
                .append("From: +33642575779").append(Constants.CRLF)
                .append("To: +33640332859").append(Constants.CRLF)
                .append("Date: ").append(mImapDate).append(Constants.CRLF)
                .append("Subject: mySubject").append(Constants.CRLF)
                .append("Conversation-ID: 1443517760826").append(Constants.CRLF)
                .append("Contribution-ID: 1443517760826").append(Constants.CRLF)
                .append("IMDN-Message-ID: 1443517760826").append(Constants.CRLF)
                .append("Message-Direction: sent").append(Constants.CRLF)
                .append("Content-Type: Application/group-state-object+xml").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(Constants.CRLF)
                .append("<groupstate").append(Constants.CRLF)
                .append("timestamp=\"2012-06-13T16:39:57-05:00\"").append(Constants.CRLF)
                .append("lastfocussessionid=\"sip:da9274453@company.com\"").append(Constants.CRLF)
                .append("group-type=\"Closed\">").append(Constants.CRLF)
                .append("<participant name=\"bob\" comm-addr=\"tel:+16135551210\"/>").append(Constants.CRLF)
                .append("<participant name=\"alice\" comm-addr=\"tel:+16135551211\"/>").append(Constants.CRLF)
                .append("<participant name=\"donald\" comm-addr=\"tel:+16135551212\"/>").append(Constants.CRLF)
                .append("</groupstate>").append(Constants.CRLF).toString();
    }
    // @formatter:on
}
