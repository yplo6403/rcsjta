/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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
 *
 ******************************************************************************/

package com.gsma.rcs.cms.imap.cmd;

import com.sonymobile.rcs.imap.Part;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CmdHandler {

    /**
     * IMAP command definition
     */
    public enum CommandType {
        LIST_STATUS, SELECT_CONDSTORE, FETCH_FLAGS, FETCH_HEADERS, FETCH_MESSAGES_BODY
    }

    /**
     * Return a IMAP command handler
     *
     * @param command
     * @param capabilities
     * @param params
     * @return
     */
    public static CmdHandler getHandler(CommandType command, List<String> capabilities,
            Object... params) {
        CmdHandler handler = null;
        switch (command) {
            case LIST_STATUS:
                handler = new ListStatusCmdHandler();
                break;
            case SELECT_CONDSTORE:
                handler = new SelectCondstoreCmdHandler();
                break;
            case FETCH_FLAGS:
                handler = new FetchFlagCmdHandler((String)params[0]);
                break;
            case FETCH_HEADERS:
                handler = new FetchHeaderCmdHandler();
                break;
            case FETCH_MESSAGES_BODY:
                handler = new FetchMessageCmdHandler();
                break;
        }
        return handler.checkCapabilities(capabilities) ? handler : null;
    }

    /**
     * Build an IMAP command
     * @param params
     * @return
     */
    public abstract String buildCommand(Object... params);

    /**
     * @param oneLine
     * @return boolean
     */
    public abstract boolean handleLine(String oneLine);

    /**
     * @param lines
     */
    public abstract void handleLines(List<String> lines);

    /**
     * @param part
     */
    public abstract void handlePart(Part part);

    /**
     * @return result
     */
    public abstract Object getResult();

    protected boolean checkCapabilities(List<String> capabilities) {
        return true;
    }

    protected String[] extractCounterValuesFromLine(String pattern, String line) {

        Matcher matcher = Pattern.compile(pattern).matcher(line);
        if (!matcher.find()) {
            return null;
        }

        String[] values = new String[matcher.groupCount()];
        for (int i = 0; i < matcher.groupCount(); i++) {
            values[i] = matcher.group(i + 1);
        }
        return values;
    }
}
