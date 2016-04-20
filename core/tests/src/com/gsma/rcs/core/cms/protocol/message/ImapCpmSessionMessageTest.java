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
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.imaplib.imap.ImapMessageMetadata;
import com.gsma.rcs.imaplib.imap.Part;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;

import java.util.List;

public class ImapCpmSessionMessageTest extends AndroidTestCase {

    private ContactUtil mContactUtil;
    private RcsSettings mSettings;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        init();
    }

    public void init() throws Exception {
        mContactUtil = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        mSettings = RcsSettingsMock.getMockSettings(mContext);
    }

    @SmallTest
    public void testCpmSessionMessage() throws CmsSyncException, RcsPermissionDeniedException {
        String folderName = "myFolder";
        Integer uid = 12;
        Part part = new Part();
        part.fromPayload(getPayload());
        ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
        metadata.getFlags().add(Flag.Seen);
        ImapMessage imapMessage = new ImapMessage(uid, metadata, part);
        imapMessage.setFolderPath(folderName);

        ImapCpmSessionMessage imapCpmSessionMessage = new ImapCpmSessionMessage(mSettings,
                imapMessage);
        Assert.assertEquals(folderName, imapCpmSessionMessage.getFolder());
        Assert.assertEquals(uid, imapCpmSessionMessage.getUid());
        Assert.assertTrue(imapCpmSessionMessage.isSeen());
        Assert.assertFalse(imapCpmSessionMessage.isDeleted());

        Assert.assertEquals("tel:+33643209850",
                imapCpmSessionMessage.getHeader(Constants.HEADER_FROM));
        Assert.assertEquals("sip:Conference-Factory@volteofr.com",
                imapCpmSessionMessage.getHeader(Constants.HEADER_TO));
        Assert.assertEquals("Thu, 11 Feb 2016 14:00:49 +0100",
                imapCpmSessionMessage.getHeader(Constants.HEADER_DATE));
        Assert.assertEquals("927d83c9902c362b08f2f2d731bdddb7",
                imapCpmSessionMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
        Assert.assertEquals("927d83c9902c362b08f2f2d731bdddb7",
                imapCpmSessionMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
        Assert.assertEquals("UFoF32nXQSy5l3d4cVGwZXn4f8YQ8rq6",
                imapCpmSessionMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
        Assert.assertEquals("sent", imapCpmSessionMessage.getHeader(Constants.HEADER_DIRECTION));
        Assert.assertEquals(Constants.APPLICATION_CPM_SESSION,
                imapCpmSessionMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

        Assert.assertEquals("927d83c9902c362b08f2f2d731bdddb7", imapCpmSessionMessage.getChatId());
        List<ContactId> contacts = imapCpmSessionMessage.getParticipants();
        Assert.assertEquals(2, contacts.size());
        Assert.assertEquals(mContactUtil.formatContact("+33642639381"), contacts.get(0));
    }

    public String getPayload() {
        return "Date: Thu, 11 Feb 2016 14:00:49 +0100" + Constants.CRLF + "From: tel:+33643209850"
                + Constants.CRLF + "To: sip:Conference-Factory@volteofr.com" + Constants.CRLF
                + "Message-ID: <881999583.1171.1455195649122@RCS5frontox1>" + Constants.CRLF
                + "Subject: cfff" + Constants.CRLF + "MIME-Version: 1.0" + Constants.CRLF
                + "Content-Type: Application/X-CPM-Session" + Constants.CRLF
                + "Content-Transfer-Encoding: 8bit" + Constants.CRLF
                + "Conversation-ID: 927d83c9902c362b08f2f2d731bdddb7" + Constants.CRLF
                + "Contribution-ID: 927d83c9902c362b08f2f2d731bdddb7" + Constants.CRLF
                + "IMDN-Message-ID: UFoF32nXQSy5l3d4cVGwZXn4f8YQ8rq6" + Constants.CRLF
                + "Message-Direction: sent" + Constants.CRLF + Constants.CRLF
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + Constants.CRLF + "<session>"
                + "<session-type>Group</session-type>"
                + "<sdp>o=- 3664184448 3664184448 IN IP4 sip.imsnsn.fr</sdp>"
                + "<invited-participants>tel:+33642639381;tel:+33643209850</invited-participants>"
                + "</session>";
    }
}
