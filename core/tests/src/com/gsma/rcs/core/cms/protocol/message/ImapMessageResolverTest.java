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

import com.gsma.rcs.core.cms.event.CmsEventHandler;
import com.gsma.rcs.core.cms.integration.RcsSettingsMock;
import com.gsma.rcs.core.cms.integration.XmsLogEnvIntegration;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.service.CmsService;
import com.gsma.rcs.core.cms.sync.process.BasicSyncStrategy;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsLogTestIntegration;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.imaplib.imap.ImapMessageMetadata;
import com.gsma.rcs.imaplib.imap.Part;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.test.AndroidTestCase;

import junit.framework.Assert;

public class ImapMessageResolverTest extends AndroidTestCase {

    private RcsSettings mSettings;

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        ContactUtil.getInstance(getContext());
        mSettings = RcsSettingsMock.getMockSettings(context);
        AndroidFactory.setApplicationContext(context, mSettings);
    }
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

            ImapMessageResolver imapMessageResolver = new ImapMessageResolver(mSettings);
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

            ImapMessageResolver imapMessageResolver = new ImapMessageResolver(mSettings);
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
