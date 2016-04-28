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

import java.util.List;

public class FetchHeaderCmdHandlerTest extends AndroidTestCase {

    public void test() {
        String expectedCmd = String.format(FetchHeaderCmdHandler.sCommand, "1", "8");
        String line1 = "* 1 FETCH (UID 1 FLAGS (\\Seen) MODSEQ (6) BODY[HEADER] {297}";
        String line2 = "* 7 FETCH (UID 7 FLAGS (\\Deleted) MODSEQ (5) BODY[HEADER] {297}";
        String payload = "From: +33642575779" + Constants.CRLF + "To: tel:+33640332859"
                + Constants.CRLF + "Date: mar., 29 09 2015 11:09:20.826 +0200" + Constants.CRLF
                + "Conversation-ID: 1443517760826" + Constants.CRLF
                + "Contribution-ID: 1443517760826" + Constants.CRLF
                + "IMDN-Message-ID: 1443517760826" + Constants.CRLF + "Message-Direction: received"
                + Constants.CRLF + "Message-Correlator: 1" + Constants.CRLF
                + "Message-Context: \"pager-message\"" + Constants.CRLF
                + "Content-Type: message/cpim" + Constants.CRLF + Constants.CRLF + ")";

        FetchHeaderCmdHandler handler = new FetchHeaderCmdHandler();
        String cmd = handler.buildCommand("1", "8");
        Assert.assertEquals(expectedCmd, cmd);

        handler.handleLine(line1);
        Assert.assertEquals("1", handler.mData.get(1).get(Constants.METADATA_UID));
        Assert.assertEquals("\\Seen", handler.mData.get(1).get(Constants.METADATA_FLAGS));
        Assert.assertEquals("6", handler.mData.get(1).get(Constants.METADATA_MODSEQ));

        Part part = new Part();
        part.fromPayload(payload);
        handler.handlePart(part);

        handler.handleLine(line2);
        Assert.assertEquals("7", handler.mData.get(7).get(Constants.METADATA_UID));
        Assert.assertEquals("\\Deleted", handler.mData.get(7).get(Constants.METADATA_FLAGS));
        Assert.assertEquals("5", handler.mData.get(7).get(Constants.METADATA_MODSEQ));

        handler.handlePart(part);

        List<ImapMessage> messages = handler.getResult();
        Assert.assertEquals(2, messages.size());

        Assert.assertEquals(7, messages.get(0).getUid());
        Assert.assertTrue(messages.get(0).getMetadata().getFlags().contains(Flag.Deleted));

        Assert.assertEquals(1, messages.get(1).getUid());
        Assert.assertTrue(messages.get(1).getMetadata().getFlags().contains(Flag.Seen));
    }

}
