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
import com.gsma.rcs.core.cms.sync.process.FlagChangeOperation;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.List;

public class FetchFlagCmdHandlerTest extends AndroidTestCase {

    public void test() {

        String expectedCmd = String.format(FetchFlagCmdHandler.sCommand, "1", "2");
        String line = "(UID 19 FLAGS (\\Seen \\Deleted) MODSEQ (92566))";

        FetchFlagCmdHandler handler = new FetchFlagCmdHandler("myFolder");
        String cmd = handler.buildCommand("1", "2");
        Assert.assertEquals(expectedCmd, cmd);

        handler.handleLine(line);

        Assert.assertEquals("19", handler.mData.get(19).get(Constants.METADATA_UID));
        Assert.assertEquals("\\Seen \\Deleted", handler.mData.get(19).get(Constants.METADATA_FLAGS));
        Assert.assertEquals("92566", handler.mData.get(19).get(Constants.METADATA_MODSEQ));

        List<FlagChangeOperation> flagChanges = handler.getResult();
        Assert.assertEquals(2, flagChanges.size());

        // Delete flagchange first
        FlagChangeOperation fg = flagChanges.get(0);
        Assert.assertEquals("myFolder", fg.getFolder());
        Assert.assertTrue(!fg.getUids().isEmpty());
        Assert.assertTrue(fg.getUids().iterator().next() == 19);
        Assert.assertTrue(fg.isDeleted());

        // Read flagchange in second
        fg = flagChanges.get(1);
        Assert.assertEquals("myFolder", fg.getFolder());
        Assert.assertTrue(!fg.getUids().isEmpty());
        Assert.assertTrue(fg.getUids().iterator().next() == 19);
        Assert.assertTrue(fg.isSeen());

    }
}
