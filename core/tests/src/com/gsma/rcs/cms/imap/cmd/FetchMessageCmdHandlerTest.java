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

package com.gsma.rcs.cms.imap.cmd;

import com.gsma.rcs.cms.Constants;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.Part;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class FetchMessageCmdHandlerTest extends AndroidTestCase {

    public void test() {

        String expectedCmd = String.format(FetchMessageCmdHandler.sCommand, "1");

        String line = "* 1 FETCH (UID 1 RFC822.SIZE 460 FLAGS (\\Seen) MODSEQ (6) BODY[] {460}";
        String payload = new StringBuilder().append("From: +33642575779").append(Constants.CRLF)
                .append("To: tel:+33640332859").append(Constants.CRLF)
                .append("Date: mar., 29 09 2015 11:09:20.826 +0200").append(Constants.CRLF)
                .append("Conversation-ID: 1443517760826").append(Constants.CRLF)
                .append("Contribution-ID: 1443517760826").append(Constants.CRLF)
                .append("IMDN-Message-ID: 1443517760826").append(Constants.CRLF)
                .append("Message-Direction: received").append(Constants.CRLF)
                .append("Message-Correlator: 1").append(Constants.CRLF)
                .append("Message-Context: \"pager-message\"").append(Constants.CRLF)
                .append("Content-Type: Message/CPIM").append(Constants.CRLF).append(Constants.CRLF)
                .append("From: +33642575779").append(Constants.CRLF).append("To: +33640332859")
                .append(Constants.CRLF).append("imdn.Message-ID: 1443517760826")
                .append(Constants.CRLF).append("DateTime: mar., 29 09 2015 11:09:20.826 +0200")
                .append(Constants.CRLF).append(Constants.CRLF)
                .append("Content-Type: text/plain; charset=utf-8").append(Constants.CRLF)
                .append(Constants.CRLF).append("1)").append(Constants.CRLF).toString();

        FetchMessageCmdHandler handler = new FetchMessageCmdHandler();
        String cmd = handler.buildCommand("1");
        Assert.assertEquals(expectedCmd, cmd);

        handler.handleLine(line);
        Assert.assertEquals("1", handler.mData.get(Constants.METADATA_UID));
        Assert.assertEquals("\\Seen", handler.mData.get(Constants.METADATA_FLAGS));
        Assert.assertEquals("6", handler.mData.get(Constants.METADATA_MODSEQ));
        Assert.assertEquals("460", handler.mData.get(Constants.METADATA_SIZE));

        Part part = new Part();
        part.fromPayload(payload);
        handler.handlePart(part);

        ImapMessage message = handler.getResult();
        Assert.assertEquals(1, message.getUid());
        Assert.assertTrue(message.getMetadata().getFlags().contains(Flag.Seen));
        Assert.assertFalse(message.getMetadata().getFlags().contains(Flag.Deleted));

    }
}
