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
import com.gsma.rcs.core.cms.integration.RcsSettingsMock;
import com.gsma.rcs.core.cms.utils.DateUtils;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.imaplib.imap.ImapMessageMetadata;
import com.gsma.rcs.imaplib.imap.Part;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;

import java.util.List;

public class ImapGroupStateMessageTest extends AndroidTestCase {

    private ContactUtil mContactUtil;
    private String mImapDate;
    private RcsSettings mSettings;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        init();
    }

    public void init() throws Exception {
        mContactUtil = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        mImapDate = DateUtils.getDateAsString(NtpTrustedTime.currentTimeMillis(),
                DateUtils.CMS_IMAP_DATE_FORMAT);
        mSettings = RcsSettingsMock.getMockSettings(mContext);
    }

    @SmallTest
    public void testGroupStateMessage() throws RcsPermissionDeniedException, CmsSyncException {
        String folderName = "myFolder";
        Integer uid = 12;
        Part part = new Part();
        part.fromPayload(getPayload());
        ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
        metadata.getFlags().add(Flag.Seen);
        ImapMessage imapMessage = new ImapMessage(uid, metadata, part);
        imapMessage.setFolderPath(folderName);

        ImapGroupStateMessage imapGroupStateMessage = new ImapGroupStateMessage(mSettings,
                imapMessage);
        Assert.assertEquals(folderName, imapGroupStateMessage.getFolder());
        Assert.assertEquals(uid, imapGroupStateMessage.getUid());
        Assert.assertTrue(imapGroupStateMessage.isSeen());
        Assert.assertFalse(imapGroupStateMessage.isDeleted());

        Assert.assertEquals("+33642575779", imapGroupStateMessage.getHeader(Constants.HEADER_FROM));
        Assert.assertEquals("+33640332859", imapGroupStateMessage.getHeader(Constants.HEADER_TO));
        Assert.assertEquals(mImapDate, imapGroupStateMessage.getHeader(Constants.HEADER_DATE));
        Assert.assertEquals("1443517760826",
                imapGroupStateMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
        Assert.assertEquals("1443517760826",
                imapGroupStateMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
        Assert.assertEquals("1443517760826",
                imapGroupStateMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
        Assert.assertEquals("sent", imapGroupStateMessage.getHeader(Constants.HEADER_DIRECTION));
        Assert.assertEquals(Constants.APPLICATION_GROUP_STATE,
                imapGroupStateMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

        Assert.assertEquals("1443517760826", imapGroupStateMessage.getChatId());
        Assert.assertEquals("sip:da9274453@company.com", imapGroupStateMessage.getRejoinId());
        List<ContactId> contacts = imapGroupStateMessage.getParticipants();
        Assert.assertEquals(3, contacts.size());
        Assert.assertEquals(mContactUtil.formatContact("+16135551210"), contacts.get(0));
        Assert.assertEquals(mContactUtil.formatContact("+16135551211"), contacts.get(1));
        Assert.assertEquals(mContactUtil.formatContact("+16135551212"), contacts.get(2));
    }

    public String getPayload() {
        return "From: +33642575779" + Constants.CRLF + "To: +33640332859" + Constants.CRLF
                + "Date: " + mImapDate + Constants.CRLF + "Subject: mySubject" + Constants.CRLF
                + "Conversation-ID: 1443517760826" + Constants.CRLF
                + "Contribution-ID: 1443517760826" + Constants.CRLF
                + "IMDN-Message-ID: 1443517760826" + Constants.CRLF + "Message-Direction: sent"
                + Constants.CRLF + "Content-Type: application/group-state-object+xml"
                + Constants.CRLF + Constants.CRLF + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + Constants.CRLF + "<groupstate" + Constants.CRLF
                + "timestamp=\"2012-06-13T16:39:57-05:00\"" + Constants.CRLF
                + "lastfocussessionid=\"sip:da9274453@company.com\"" + Constants.CRLF
                + "group-type=\"Closed\">" + Constants.CRLF
                + "<participant name=\"bob\" comm-addr=\"tel:+16135551210\"/>" + Constants.CRLF
                + "<participant name=\"alice\" comm-addr=\"tel:+16135551211\"/>" + Constants.CRLF
                + "<participant name=\"donald\" comm-addr=\"tel:+16135551212\"/>" + Constants.CRLF
                + "</groupstate>" + Constants.CRLF;
    }
}
