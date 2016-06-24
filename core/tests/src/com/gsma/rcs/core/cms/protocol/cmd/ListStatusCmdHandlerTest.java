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

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.Arrays;
import java.util.List;

public class ListStatusCmdHandlerTest extends AndroidTestCase {

    public void test() {
        String[] lines = new String[] {
                "* LIST () \".\" \"+33642575779\"",
                "* STATUS +33642575779 (MESSAGES 1 UIDNEXT 4 UIDVALIDITY 1437039675 HIGHESTMODSEQ 7)",
                "* LIST () \".\" \"INBOX\"",
                "* STATUS INBOX (MESSAGES 2 UIDNEXT 1 UIDVALIDITY 1437039422 HIGHESTMODSEQ 1)",
                "a3 OK List completed."
        };

        ListStatusCmdHandler handler = new ListStatusCmdHandler();
        String cmd = handler.buildCommand();
        Assert.assertEquals(ListStatusCmdHandler.sCommand, cmd);

        handler.handleLines(Arrays.asList(lines));

        Assert.assertEquals("1", handler.mData.get("+33642575779").get(Constants.METADATA_MESSAGES));
        Assert.assertEquals("4", handler.mData.get("+33642575779").get(Constants.METADATA_UIDNEXT));
        Assert.assertEquals("1437039675",
                handler.mData.get("+33642575779").get(Constants.METADATA_UIDVALIDITY));
        Assert.assertEquals("7",
                handler.mData.get("+33642575779").get(Constants.METADATA_HIGHESTMODSEQ));

        Assert.assertEquals("2", handler.mData.get("INBOX").get(Constants.METADATA_MESSAGES));
        Assert.assertEquals("1", handler.mData.get("INBOX").get(Constants.METADATA_UIDNEXT));
        Assert.assertEquals("1437039422",
                handler.mData.get("INBOX").get(Constants.METADATA_UIDVALIDITY));
        Assert.assertEquals("1", handler.mData.get("INBOX").get(Constants.METADATA_HIGHESTMODSEQ));

        List<ImapFolder> folders = handler.getResult();
        Assert.assertEquals(2, folders.size());

        for (ImapFolder imapFolder : folders) {
            if ("+33642575779".equals(imapFolder.getName())) {
                Assert.assertEquals(Integer.valueOf(1), imapFolder.getMessages());
                Assert.assertEquals(Integer.valueOf(4), imapFolder.getUidNext());
                Assert.assertEquals(Integer.valueOf(7), imapFolder.getHighestModseq());
                Assert.assertEquals(Integer.valueOf(1437039675), imapFolder.getUidValidity());

            } else if ("INBOX".equals(imapFolder.getName())) {
                Assert.assertEquals(Integer.valueOf(2), imapFolder.getMessages());
                Assert.assertEquals(Integer.valueOf(1), imapFolder.getUidNext());
                Assert.assertEquals(Integer.valueOf(1), imapFolder.getHighestModseq());
                Assert.assertEquals(Integer.valueOf(1437039422), imapFolder.getUidValidity());
            }
        }

    }
}
