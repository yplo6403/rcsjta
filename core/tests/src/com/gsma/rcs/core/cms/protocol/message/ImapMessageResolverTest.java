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

import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;
import com.sonymobile.rcs.imap.Part;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class ImapMessageResolverTest extends AndroidTestCase {

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
