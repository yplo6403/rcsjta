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
import com.gsma.rcs.core.cms.protocol.message.cpim.multipart.MultipartCpimBody;
import com.gsma.rcs.core.cms.utils.DateUtils;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.utils.ContactUtil;

import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.imaplib.imap.ImapMessageMetadata;
import com.gsma.rcs.imaplib.imap.Part;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class ImapMmsMessageTest extends AndroidTestCase {

    private String mPayload;
    private String mBoundary;
    private long mDate;
    private String mImapDate;
    private String mCpimDate;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        com.gsma.services.rcs.contact.ContactUtil.getInstance(getContext());

        mBoundary = "boundary_1446218793256";
        mDate = NtpTrustedTime.currentTimeMillis();
        mImapDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_IMAP_DATE_FORMAT);
        mCpimDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_CPIM_DATE_FORMAT);

        mPayload = new StringBuilder().append("From: +33642575779").append(Constants.CRLF)
                .append("To: +33640332859").append(Constants.CRLF).append("Date: ")
                .append(mImapDate).append(Constants.CRLF).append("Conversation-ID: 1443517760826")
                .append(Constants.CRLF).append("Contribution-ID: 1443517760826")
                .append(Constants.CRLF).append("Message-ID: myMmsId").append(Constants.CRLF)
                .append("IMDN-Message-ID: 1443517760826").append(Constants.CRLF)
                .append("Message-Direction: received").append(Constants.CRLF)
                .append("Message-Context: multimedia-message").append(Constants.CRLF)
                .append("Content-Type: Message/CPIM").append(Constants.CRLF).append(Constants.CRLF)
                .append("From: +33642575779").append(Constants.CRLF).append("To: +33640332859")
                .append(Constants.CRLF).append("DateTime: ").append(mCpimDate)
                .append(Constants.CRLF).append("NS: imdn <urn:ietf:params:imdn>")
                .append(Constants.CRLF).append("NS: rcs <http://www.gsma.com>")
                .append(Constants.CRLF).append("imdn.Message-ID: 1443517760826")
                .append(Constants.CRLF).append(Constants.CRLF)
                .append("Content-Type: Multipart/Related;boundary=\"boundary_1446218793256\";")
                .append(Constants.CRLF).append(Constants.CRLF).append("--boundary_1446218793256")
                .append(Constants.CRLF).append("Content-Type: text/plain").append(Constants.CRLF)
                .append(Constants.CRLF).append("myContent").append(Constants.CRLF)
                .append(Constants.CRLF).append("--boundary_1446218793256").append(Constants.CRLF)
                .append("Content-Type: text/plain; charset=utf-8").append(Constants.CRLF)
                .append(Constants.CRLF).append("1").append(Constants.CRLF).append(Constants.CRLF)
                .append("--boundary_1446218793256--").toString();

    }

    @SmallTest
    public void testFromPayload() {

        try {
            ImapMessage rawMessage = new ImapMessage();
            rawMessage.fromPayload(mPayload);
            ImapMmsMessage imapMmsMessage = new ImapMmsMessage(rawMessage);
            Assert.assertEquals("+33642575779", imapMmsMessage.getHeader(Constants.HEADER_FROM));
            Assert.assertEquals("+33640332859", imapMmsMessage.getHeader(Constants.HEADER_TO));
            Assert.assertEquals(mImapDate, imapMmsMessage.getHeader(Constants.HEADER_DATE));
            Assert.assertEquals("1443517760826",
                    imapMmsMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
            Assert.assertEquals("1443517760826",
                    imapMmsMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
            Assert.assertEquals("myMmsId", imapMmsMessage.getHeader(Constants.HEADER_MESSAGE_ID));
            Assert.assertEquals("1443517760826",
                    imapMmsMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
            Assert.assertEquals("received", imapMmsMessage.getHeader(Constants.HEADER_DIRECTION));
            Assert.assertEquals("multimedia-message",
                    imapMmsMessage.getHeader(Constants.HEADER_MESSAGE_CONTEXT));
            Assert.assertEquals("Message/CPIM",
                    imapMmsMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

            Assert.assertEquals("+33642575779",
                    imapMmsMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
            Assert.assertEquals("+33640332859",
                    imapMmsMessage.getCpimMessage().getHeader(Constants.HEADER_TO));
            Assert.assertEquals("1443517760826",
                    imapMmsMessage.getCpimMessage().getHeader("imdn.Message-ID"));
            Assert.assertEquals(mCpimDate, imapMmsMessage.getCpimMessage().getHeader("DateTime"));

            MultipartCpimBody cpimBody = (MultipartCpimBody) imapMmsMessage.getCpimMessage()
                    .getBody();

            Assert.assertEquals("Multipart/Related;boundary=\"boundary_1446218793256\";",
                    cpimBody.getContentType());
            Assert.assertEquals(2, cpimBody.getParts().size());

            MultipartCpimBody.Part part;

            part = cpimBody.getParts().get(0);
            Assert.assertEquals("text/plain", part.getHeader(Constants.HEADER_CONTENT_TYPE));
            Assert.assertEquals("myContent", part.getContent());

            part = cpimBody.getParts().get(1);
            Assert.assertEquals("text/plain; charset=utf-8",
                    part.getHeader(Constants.HEADER_CONTENT_TYPE));
            Assert.assertEquals("1", part.getContent());

        } catch (CmsSyncException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }

    @SmallTest
    public void testToPayload() {

        List<MmsPart> parts = new ArrayList<>();
        parts.add(new MmsPart("myMmsId",
                ContactUtil.createContactIdFromTrustedData("+33642575779"), "text/plain",
                "myContent"));
        parts.add(new MmsPart("myMmsId",
                ContactUtil.createContactIdFromTrustedData("+33642575779"),
                "text/plain; charset=utf-8", "1"));
        ImapMmsMessage imapMmsMessage = new ImapMmsMessage(getContext(), "+33642575779",
                "+33640332859", "received", mDate, null, "1443517760826", "1443517760826",
                "1443517760826", "myMmsId", parts);

        ((MultipartCpimBody) imapMmsMessage.getCpimMessage().getBody()).setBoundary(mBoundary);

        Assert.assertEquals(mPayload, imapMmsMessage.toPayload());

    }

    public static class ImapMessageResolverTest extends AndroidTestCase {

        public void testSms() {

            try {
                String payload = new StringBuilder().append("From: +33642575779")
                        .append(Constants.CRLF).append("To: tel:+33640332859").append(Constants.CRLF)
                        .append("Date: mar., 29 09 2015 11:09:20.826 +0200").append(Constants.CRLF)
                        .append("Conversation-ID: 1443517760826").append(Constants.CRLF)
                        .append("Contribution-ID: 1443517760826").append(Constants.CRLF)
                        .append("IMDN-Message-ID: 1443517760826").append(Constants.CRLF)
                        .append("Message-Direction: received").append(Constants.CRLF)
                        .append("Message-Correlator: 1").append(Constants.CRLF)
                        .append("Message-Context: pager-message").append(Constants.CRLF)
                        .append("Content-Type: Message/CPIM").append(Constants.CRLF)
                        .append(Constants.CRLF).append("From: +33642575779").append(Constants.CRLF)
                        .append("To: +33640332859").append(Constants.CRLF)
                        .append("imdn.Message-ID: 1443517760826").append(Constants.CRLF)
                        .append("DateTime: mar., 29 09 2015 11:09:20.826 +0200").append(Constants.CRLF)
                        .append(Constants.CRLF).append("Content-Type: text/plain; charset=utf-8")
                        .append(Constants.CRLF).append(Constants.CRLF).append("1)")
                        .append(Constants.CRLF).toString();

                Integer uid = 12;
                Part part = new Part();
                part.fromPayload(payload);
                ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
                metadata.getFlags().add(Flag.Seen);
                ImapMessage imapMessage = new ImapMessage(uid, metadata, part);

                ImapMessageResolver imapMessageResolver = new ImapMessageResolver();
                MessageType type = imapMessageResolver.resolveType(imapMessage);
                Assert.assertEquals(MessageType.SMS, type);

                Assert.assertTrue(imapMessageResolver.resolveMessage(type, imapMessage) instanceof ImapSmsMessage);
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail();
            }

        }

        public void testMms() {

            try {
                String payload = new StringBuilder().append("From: +33642575779")
                        .append(Constants.CRLF).append("To: tel:+33640332859").append(Constants.CRLF)
                        .append("Date: mar., 29 09 2015 11:09:20.826 +0200").append(Constants.CRLF)
                        .append("Conversation-ID: 1443517760826").append(Constants.CRLF)
                        .append("Contribution-ID: 1443517760826").append(Constants.CRLF)
                        .append("IMDN-Message-ID: 1443517760826").append(Constants.CRLF)
                        .append("Message-Direction: received").append(Constants.CRLF)
                        .append("Message-Correlator: 1").append(Constants.CRLF)
                        .append("Message-Context: multimedia-message").append(Constants.CRLF)
                        .append("Content-Type: Message/CPIM").append(Constants.CRLF)
                        .append(Constants.CRLF).append("From: +33642575779").append(Constants.CRLF)
                        .append("To: +33640332859").append(Constants.CRLF)
                        .append("imdn.Message-ID: 1443517760826").append(Constants.CRLF)
                        .append("DateTime: mar., 29 09 2015 11:09:20.826 +0200").append(Constants.CRLF)
                        .append(Constants.CRLF).append("Content-Type: text/plain; charset=utf-8")
                        .append(Constants.CRLF).append(Constants.CRLF).append("1)")
                        .append(Constants.CRLF).toString();

                Integer uid = 12;
                Part part = new Part();
                part.fromPayload(payload);
                ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
                metadata.getFlags().add(Flag.Seen);
                ImapMessage imapMessage = new ImapMessage(uid, metadata, part);

                ImapMessageResolver imapMessageResolver = new ImapMessageResolver();
                MessageType type = imapMessageResolver.resolveType(imapMessage);
                Assert.assertEquals(MessageType.MMS, type);
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail();
            }
            // TODO
            // Assert.assertTrue(imapMessageResolver.resolveMessage(type, imapMessage) instanceof
            // ImapMmsMessage);

        }
    }
}
