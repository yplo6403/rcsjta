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
import com.gsma.rcs.core.cms.protocol.message.cpim.text.TextCpimBody;
import com.gsma.rcs.core.cms.utils.DateUtils;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.imaplib.imap.ImapMessageMetadata;
import com.gsma.rcs.imaplib.imap.Part;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;

public class ImapSmsMessageTest extends AndroidTestCase {

    private String mPayload;
    private long mDate;
    private String mImapDate;
    private String mCpimDate;
    private ContactId mRemote;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDate = NtpTrustedTime.currentTimeMillis();
        mImapDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_IMAP_DATE_FORMAT);
        mCpimDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_CPIM_DATE_FORMAT);
        mRemote = ContactUtil.createContactIdFromTrustedData("+33642575779");
        mPayload = "From: +33642575779" + Constants.CRLF + "To: +33640332859" + Constants.CRLF
                + "Date: " + mImapDate + Constants.CRLF + "Conversation-ID: 1443517760826"
                + Constants.CRLF + "Contribution-ID: 1443517760826" + Constants.CRLF
                + "IMDN-Message-ID: 1443517760826" + Constants.CRLF + "Message-Direction: received"
                + Constants.CRLF + "Message-Correlator: 1" + Constants.CRLF
                + "Message-Context: pager-message" + Constants.CRLF + "Content-Type: Message/CPIM"
                + Constants.CRLF + Constants.CRLF + "From: +33642575779" + Constants.CRLF
                + "To: +33640332859" + Constants.CRLF + "NS: imdn <urn:ietf:params:imdn>"
                + Constants.CRLF + "NS: rcs <http://www.gsma.com>" + Constants.CRLF
                + "imdn.Message-ID: 1443517760826" + Constants.CRLF + "DateTime: " + mCpimDate
                + Constants.CRLF + Constants.CRLF + "Content-Type: text/plain; charset=utf-8"
                + Constants.CRLF + Constants.CRLF + "1";
    }

    @SmallTest
    public void testFromPayload() throws CmsSyncMissingHeaderException,
            CmsSyncHeaderFormatException {
        String folderName = "myFolder";
        Integer uid = 12;
        Part part = new Part();
        part.fromPayload(mPayload);
        ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
        metadata.getFlags().add(Flag.Seen);
        ImapMessage imapMessage = new ImapMessage(uid, metadata, part);
        imapMessage.setFolderPath(folderName);

        ImapSmsMessage imapSmsMessage = new ImapSmsMessage(imapMessage);
        Assert.assertEquals(folderName, imapSmsMessage.getFolder());
        Assert.assertEquals(uid, imapSmsMessage.getUid());
        Assert.assertTrue(imapSmsMessage.isSeen());
        Assert.assertFalse(imapSmsMessage.isDeleted());

        Assert.assertEquals("+33642575779", imapSmsMessage.getHeader(Constants.HEADER_FROM));
        Assert.assertEquals("+33640332859", imapSmsMessage.getHeader(Constants.HEADER_TO));
        Assert.assertEquals(mImapDate, imapSmsMessage.getHeader(Constants.HEADER_DATE));
        Assert.assertEquals("1443517760826",
                imapSmsMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
        Assert.assertEquals("1443517760826",
                imapSmsMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
        Assert.assertEquals("1443517760826",
                imapSmsMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
        Assert.assertEquals("received", imapSmsMessage.getHeader(Constants.HEADER_DIRECTION));
        Assert.assertEquals("1", imapSmsMessage.getHeader(Constants.HEADER_MESSAGE_CORRELATOR));
        Assert.assertEquals("pager-message",
                imapSmsMessage.getHeader(Constants.HEADER_MESSAGE_CONTEXT));
        Assert.assertEquals("Message/CPIM", imapSmsMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

        Assert.assertEquals("+33642575779",
                imapSmsMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
        Assert.assertEquals("+33640332859",
                imapSmsMessage.getCpimMessage().getHeader(Constants.HEADER_TO));
        Assert.assertEquals("1443517760826",
                imapSmsMessage.getCpimMessage().getHeader("imdn.Message-ID"));
        Assert.assertEquals(mCpimDate, imapSmsMessage.getCpimMessage().getHeader("DateTime"));

        Assert.assertEquals("1",
                ((TextCpimBody) imapSmsMessage.getCpimMessage().getBody()).getContent());
    }

    @SmallTest
    public void testToPayload() {
        ImapSmsMessage imapSmsMessage = new ImapSmsMessage(mRemote, "+33642575779", "+33640332859",
                "received", mDate, "1", "1443517760826", "1443517760826", "1443517760826");
        Assert.assertEquals(mPayload, imapSmsMessage.toPayload());
    }

}
