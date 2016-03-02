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

import android.test.AndroidTestCase;

import java.util.List;

import junit.framework.Assert;

public class UidSearchCmdHandlerTest extends AndroidTestCase {

    public void test() {

        String headerName = "IMDN-Message-ID";
        String headerValue = "41dc8c6cb9d643ca9a1b789b1ead65a8";
        String expectedCmd = String.format(UidSearchCmdHandler.sCommand, "HEADER " + headerName
                + " " + headerValue);
        String[] lines = new String[] {
                "* SEARCH 10",
                "* 11 EXISTS",
                "a4 OK Search completed (0.004 + 0.000 secs)."
        };

        UidSearchCmdHandler handler = new UidSearchCmdHandler();
        String cmd = handler.buildCommand("HEADER " + headerName + " " + headerValue);
        Assert.assertEquals(expectedCmd, cmd);
        for (String line : lines) {
            handler.handleLine(line);
        }

        List<Integer> result = handler.getResult();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(new Integer(10), result.get(0));
    }
}
