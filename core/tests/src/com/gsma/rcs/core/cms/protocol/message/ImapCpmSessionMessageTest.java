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

public class ImapCpmSessionMessageTest extends AndroidTestCase {

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
    public void testCpmSessionMessage() {

        try {
            String folderName = "myFolder";

            Integer uid = 12;
            Part part = new Part();
            part.fromPayload(getPayload());
            ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
            metadata.getFlags().add(Flag.Seen);
            ImapMessage imapMessage = new ImapMessage(uid, metadata, part);
            imapMessage.setFolderPath(folderName);

            ImapCpmSessionMessage imapCpmSessionMessage = new ImapCpmSessionMessage(imapMessage);
            Assert.assertEquals(folderName, imapCpmSessionMessage.getFolder());
            Assert.assertEquals(uid, imapCpmSessionMessage.getUid());
            Assert.assertTrue(imapCpmSessionMessage.isSeen());
            Assert.assertFalse(imapCpmSessionMessage.isDeleted());

            Assert.assertEquals("tel:+33643209850", imapCpmSessionMessage.getHeader(Constants.HEADER_FROM));
            Assert.assertEquals("sip:Conference-Factory@volteofr.com", imapCpmSessionMessage.getHeader(Constants.HEADER_TO));
            Assert.assertEquals("Thu, 11 Feb 2016 14:00:49 +0100", imapCpmSessionMessage.getHeader(Constants.HEADER_DATE));
            Assert.assertEquals("927d83c9902c362b08f2f2d731bdddb7", imapCpmSessionMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
            Assert.assertEquals("927d83c9902c362b08f2f2d731bdddb7", imapCpmSessionMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
            Assert.assertEquals("UFoF32nXQSy5l3d4cVGwZXn4f8YQ8rq6", imapCpmSessionMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
            Assert.assertEquals("sent", imapCpmSessionMessage.getHeader(Constants.HEADER_DIRECTION));
            Assert.assertEquals(Constants.APPLICATION_CPM_SESSION, imapCpmSessionMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

            Assert.assertEquals("927d83c9902c362b08f2f2d731bdddb7", imapCpmSessionMessage.getChatId());
            List<ContactId> contacts = imapCpmSessionMessage.getParticipants();
            Assert.assertEquals(1, contacts.size());
            Assert.assertEquals(ContactUtil.getInstance(getContext()).formatContact("+33642639381"), contacts.get(0));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }


    public String getPayload() {

        return new StringBuilder()
                .append("Date: Thu, 11 Feb 2016 14:00:49 +0100").append(Constants.CRLF)
                .append("From: tel:+33643209850").append(Constants.CRLF)
                .append("To: sip:Conference-Factory@volteofr.com").append(Constants.CRLF)
                .append("Message-ID: <881999583.1171.1455195649122@RCS5frontox1>").append(Constants.CRLF)
                .append("Subject: cfff").append(Constants.CRLF)
                .append("MIME-Version: 1.0").append(Constants.CRLF)
                .append("Content-Type: Application/X-CPM-Session").append(Constants.CRLF)
                .append("Content-Transfer-Encoding: 8bit").append(Constants.CRLF)
                .append("Conversation-ID: 927d83c9902c362b08f2f2d731bdddb7").append(Constants.CRLF)
                .append("Contribution-ID: 927d83c9902c362b08f2f2d731bdddb7").append(Constants.CRLF)
                .append("IMDN-Message-ID: UFoF32nXQSy5l3d4cVGwZXn4f8YQ8rq6").append(Constants.CRLF)
                .append("Message-Direction: sent").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(Constants.CRLF)
                .append("<session>")
                .append("<session-type>Group</session-type>")
                .append("<sdp>o=- 3664184448 3664184448 IN IP4 sip.imsnsn.fr</sdp>")
                .append("<invited-participants>tel:+33642639381;tel:+33643209850</invited-participants>")
                .append("</session>").toString();
    }
    // @formatter:on
}
