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
import com.gsma.rcs.core.cms.protocol.cmd.CmdHandler.CommandType;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;

public class CmdHandlerTest extends AndroidTestCase {

    public void test() {
        List<String> capabilities = new ArrayList<String>();
        capabilities.add(Constants.CAPA_CONDSTORE);
        CmdHandler handler;

        handler = CmdHandler.getHandler(CommandType.SELECT_CONDSTORE, capabilities);
        assertTrue(handler instanceof SelectCondstoreCmdHandler);

        handler = CmdHandler.getHandler(CommandType.LIST_STATUS, capabilities);
        assertTrue(handler instanceof ListStatusCmdHandler);

        handler = CmdHandler.getHandler(CommandType.FETCH_FLAGS, capabilities, "myFolder");
        assertTrue(handler instanceof FetchFlagCmdHandler);

        handler = CmdHandler.getHandler(CommandType.FETCH_HEADERS, capabilities);
        assertTrue(handler instanceof FetchHeaderCmdHandler);

        handler = CmdHandler.getHandler(CommandType.FETCH_MESSAGES_BODY, capabilities);
        assertTrue(handler instanceof FetchMessageCmdHandler);

    }

}
