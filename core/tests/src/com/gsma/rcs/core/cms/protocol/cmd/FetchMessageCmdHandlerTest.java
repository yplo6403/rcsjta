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

package com.gsma.rcs.core.cms.protocol.cmd;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.imaplib.imap.Part;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class FetchMessageCmdHandlerTest extends AndroidTestCase {

    public void test() {
        String expectedCmd = String.format(FetchMessageCmdHandler.sCommand, "1");

        String line = "* 1 FETCH (UID 1 RFC822.SIZE 460 FLAGS (\\Seen) MODSEQ (6) BODY[] {460}";
        String payload = "From: +33642575779" + Constants.CRLF + "To: tel:+33640332859"
                + Constants.CRLF + "Date: mar., 29 09 2015 11:09:20.826 +0200" + Constants.CRLF
                + "Conversation-ID: 1443517760826" + Constants.CRLF
                + "Contribution-ID: 1443517760826" + Constants.CRLF
                + "IMDN-Message-ID: 1443517760826" + Constants.CRLF + "Message-Direction: received"
                + Constants.CRLF + "Message-Correlator: 1" + Constants.CRLF
                + "Message-Context: \"pager-message\"" + Constants.CRLF
                + "Content-Type: message/cpim" + Constants.CRLF + Constants.CRLF
                + "From: +33642575779" + Constants.CRLF + "To: +33640332859" + Constants.CRLF
                + "imdn.Message-ID: 1443517760826" + Constants.CRLF
                + "DateTime: mar., 29 09 2015 11:09:20.826 +0200" + Constants.CRLF + Constants.CRLF
                + "Content-Type: text/plain; charset=utf-8" + Constants.CRLF + Constants.CRLF
                + "1)" + Constants.CRLF;

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
