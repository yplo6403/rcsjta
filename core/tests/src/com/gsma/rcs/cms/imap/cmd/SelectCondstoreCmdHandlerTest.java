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

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.Arrays;

public class SelectCondstoreCmdHandlerTest extends AndroidTestCase {

    public void test() {

        String expectedCmd = String.format(SelectCondstoreCmdHandler.sCommand, "myFolder");
        String[] lines = new String[] {
                "* OK [CLOSED] Previous mailbox closed.",
                "* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)",
                "* OK [PERMANENTFLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft \\*)] Flags permitted.",
                "* 3 EXISTS", "* 0 RECENT", "* OK [UIDVALIDITY 1437039675] UIDs valid",
                "* OK [UIDNEXT 4] Predicted next UID", "* OK [HIGHESTMODSEQ 7] Highest",
                "a6 OK [READ-WRITE] Select completed."
        };

        String[] capabilities = new String[] {
            Constants.CAPA_CONDSTORE
        };

        SelectCondstoreCmdHandler handler = new SelectCondstoreCmdHandler();
        Assert.assertTrue(handler.checkCapabilities(Arrays.asList(capabilities)));

        String cmd = handler.buildCommand("myFolder");
        Assert.assertEquals(expectedCmd, cmd);

        handler.handleLines(Arrays.asList(lines));

        Assert.assertEquals("1437039675",
                handler.mData.get("myFolder").get(Constants.METADATA_UIDVALIDITY));
        Assert.assertEquals("4", handler.mData.get("myFolder").get(Constants.METADATA_UIDNEXT));
        Assert.assertEquals("7", handler.mData.get("myFolder")
                .get(Constants.METADATA_HIGHESTMODSEQ));
    }

}
