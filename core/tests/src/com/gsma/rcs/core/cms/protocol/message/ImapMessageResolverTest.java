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
import com.gsma.rcs.core.cms.event.exception.CmsSyncMessageNotSupportedException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.integration.RcsSettingsMock;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.imaplib.imap.ImapMessageMetadata;
import com.gsma.rcs.imaplib.imap.Part;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.test.AndroidTestCase;

import junit.framework.Assert;

public class ImapMessageResolverTest extends AndroidTestCase {

    private RcsSettings mSettings;

    protected void setUp() throws Exception {
        super.setUp();
        mSettings = RcsSettingsMock.getMockSettings(mContext);
        AndroidFactory.setApplicationContext(mContext, mSettings);
    }

    public void testSms() throws CmsSyncException {
        String payload = "From: +33642575779" + Constants.CRLF + "To: tel:+33640332859"
                + Constants.CRLF + "Date: mar., 29 09 2015 11:09:20.826 +0200" + Constants.CRLF
                + "Conversation-ID: 1443517760826" + Constants.CRLF
                + "Contribution-ID: 1443517760826" + Constants.CRLF
                + "IMDN-Message-ID: 1443517760826" + Constants.CRLF + "Message-Direction: received"
                + Constants.CRLF + "Message-Correlator: 1" + Constants.CRLF
                + "Message-Context: pager-message" + Constants.CRLF + "Content-Type: Message/CPIM"
                + Constants.CRLF + Constants.CRLF + "From: +33642575779" + Constants.CRLF
                + "To: +33640332859" + Constants.CRLF + "imdn.Message-ID: 1443517760826"
                + Constants.CRLF + "DateTime: mar., 29 09 2015 11:09:20.826 +0200" + Constants.CRLF
                + Constants.CRLF + "Content-Type: text/plain; charset=utf-8" + Constants.CRLF
                + Constants.CRLF + "1)" + Constants.CRLF;

        Integer uid = 12;
        Part part = new Part();
        part.fromPayload(payload);
        ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
        metadata.getFlags().add(Flag.Seen);
        ImapMessage imapMessage = new ImapMessage(uid, metadata, part);

        ImapMessageResolver imapMessageResolver = new ImapMessageResolver(mSettings);
        MessageType type = imapMessageResolver.resolveType(imapMessage);
        Assert.assertEquals(MessageType.SMS, type);

        Assert.assertTrue(imapMessageResolver.resolveMessage(type, imapMessage) instanceof ImapSmsMessage);
    }

    public void testMms() throws CmsSyncMissingHeaderException, CmsSyncMessageNotSupportedException {
        String payload = "From: +33642575779" + Constants.CRLF + "To: tel:+33640332859"
                + Constants.CRLF + "Date: mar., 29 09 2015 11:09:20.826 +0200" + Constants.CRLF
                + "Conversation-ID: 1443517760826" + Constants.CRLF
                + "Contribution-ID: 1443517760826" + Constants.CRLF
                + "IMDN-Message-ID: 1443517760826" + Constants.CRLF + "Message-Direction: received"
                + Constants.CRLF + "Message-Correlator: 1" + Constants.CRLF
                + "Message-Context: multimedia-message" + Constants.CRLF
                + "Content-Type: Message/CPIM" + Constants.CRLF + Constants.CRLF
                + "From: +33642575779" + Constants.CRLF + "To: +33640332859" + Constants.CRLF
                + "imdn.Message-ID: 1443517760826" + Constants.CRLF
                + "DateTime: mar., 29 09 2015 11:09:20.826 +0200" + Constants.CRLF + Constants.CRLF
                + "Content-Type: text/plain; charset=utf-8" + Constants.CRLF + Constants.CRLF
                + "1)" + Constants.CRLF;

        Integer uid = 12;
        Part part = new Part();
        part.fromPayload(payload);
        ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
        metadata.getFlags().add(Flag.Seen);
        ImapMessage imapMessage = new ImapMessage(uid, metadata, part);

        ImapMessageResolver imapMessageResolver = new ImapMessageResolver(mSettings);
        MessageType type = imapMessageResolver.resolveType(imapMessage);
        Assert.assertEquals(MessageType.MMS, type);
        // TODO
        // Assert.assertTrue(imapMessageResolver.resolveMessage(type, imapMessage) instanceof
        // ImapMmsMessage);
    }
}
