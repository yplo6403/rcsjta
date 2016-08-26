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

import com.gsma.rcs.RcsSettingsMock;
import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncXmlFormatException;
import com.gsma.rcs.core.cms.protocol.message.cpim.multipart.MultipartCpimBody;
import com.gsma.rcs.core.cms.utils.DateUtils;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

public class ImapMmsMessageTest extends AndroidTestCase {

    // @formatter:off
    private static final String FORMAT_PAYLOAD = "From: +33642575779\r\n" +
            "To: +33640332859\r\n" +
            "Date: %1$s\r\n" +
            "Conversation-ID: 1443517760826\r\n" +
            "Contribution-ID: 1443517760826\r\n" +
            "Message-Correlator: myMmsId\r\n" +
            "IMDN-Message-ID: 1443517760826\r\n" +
            "Message-Direction: received\r\n" +
            "Message-Context: multimedia-message\r\n" +
            "Content-Type: message/cpim\r\n\r\n" +
            "From: +33642575779\r\n" +
            "To: +33640332859\r\n" +
            "DateTime: %2$s\r\n" +
            "NS: imdn <urn:ietf:params:imdn>\r\n" +
            "NS: rcs <http://www.gsma.com>\r\n" +
            "imdn.Message-ID: 1443517760826\r\n\r\n" +
            "Content-Type: multipart/related;boundary=\"boundary_1446218793256\";\r\n\r\n" +
            "--boundary_1446218793256\r\n" +
            "Content-Type: text/plain\r\n\r\n" +
            "myContent\r\n\r\n" +
            "--boundary_1446218793256\r\n" +
           "Content-Type: text/plain; charset=utf-8\r\n\r\n" +
            "1\r\n\r\n" +
            "--boundary_1446218793256--";
    // @formatter:on

    private static final String MULTIPART_BOUNDARY = "boundary_1446218793256";

    private String mPayload;
    private long mDate;
    private String mImapDate;
    private String mCpimDate;
    private ContactId mRemote;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        RcsSettingsMock.getMockSettings(getContext());
        mDate = NtpTrustedTime.currentTimeMillis();
        mImapDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_IMAP_DATE_FORMAT);
        mCpimDate = DateUtils.getDateAsString(mDate, DateUtils.CMS_CPIM_DATE_FORMAT);
        mRemote = ContactUtil.createContactIdFromTrustedData("+33642575779");
        Formatter formatter = new Formatter(new StringBuilder(), Locale.US);
        mPayload = formatter.format(FORMAT_PAYLOAD, mImapDate, mCpimDate).toString();
    }

    @SmallTest
    public void testFromPayload() throws CmsSyncMissingHeaderException,
            CmsSyncHeaderFormatException, CmsSyncXmlFormatException {
        ImapMessage rawMessage = new ImapMessage();
        rawMessage.fromPayload(mPayload);
        ImapMmsMessage imapMmsMessage = new ImapMmsMessage(rawMessage, mRemote);
        imapMmsMessage.parseBody();
        Assert.assertEquals("+33642575779", imapMmsMessage.getHeader(Constants.HEADER_FROM));
        Assert.assertEquals("+33640332859", imapMmsMessage.getHeader(Constants.HEADER_TO));
        Assert.assertEquals(mImapDate, imapMmsMessage.getHeader(Constants.HEADER_DATE));
        Assert.assertEquals("1443517760826",
                imapMmsMessage.getHeader(Constants.HEADER_CONVERSATION_ID));
        Assert.assertEquals("1443517760826",
                imapMmsMessage.getHeader(Constants.HEADER_CONTRIBUTION_ID));
        Assert.assertEquals("myMmsId",
                imapMmsMessage.getHeader(Constants.HEADER_MESSAGE_CORRELATOR));
        Assert.assertEquals("1443517760826",
                imapMmsMessage.getHeader(Constants.HEADER_IMDN_MESSAGE_ID));
        Assert.assertEquals("received", imapMmsMessage.getHeader(Constants.HEADER_DIRECTION));
        Assert.assertEquals("multimedia-message",
                imapMmsMessage.getHeader(Constants.HEADER_MESSAGE_CONTEXT));
        Assert.assertEquals("message/cpim", imapMmsMessage.getHeader(Constants.HEADER_CONTENT_TYPE));

        Assert.assertEquals("+33642575779",
                imapMmsMessage.getCpimMessage().getHeader(Constants.HEADER_FROM));
        Assert.assertEquals("+33640332859",
                imapMmsMessage.getCpimMessage().getHeader(Constants.HEADER_TO));
        Assert.assertEquals("1443517760826",
                imapMmsMessage.getCpimMessage().getHeader("imdn.Message-ID"));
        Assert.assertEquals(mCpimDate, imapMmsMessage.getCpimMessage().getHeader("DateTime"));

        MultipartCpimBody cpimBody = (MultipartCpimBody) imapMmsMessage.getCpimMessage().getBody();

        Assert.assertEquals("multipart/related;boundary=\"boundary_1446218793256\";",
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
    }

    @SmallTest
    public void testToPayload() {
        List<MmsPart> parts = new ArrayList<>();
        parts.add(new MmsPart("myMmsId", "text/plain", "myContent"));
        parts.add(new MmsPart("myMmsId", "text/plain; charset=utf-8", "1"));
        ImapMmsMessage imapMmsMessage = new ImapMmsMessage(getContext(), mRemote, "+33642575779",
                "+33640332859", "received", mDate, null, "1443517760826", "1443517760826",
                "1443517760826", "myMmsId", parts);

        ((MultipartCpimBody) imapMmsMessage.getCpimMessage().getBody())
                .setBoundary(MULTIPART_BOUNDARY);

        Assert.assertEquals(mPayload, imapMmsMessage.toPayload());

    }

}
