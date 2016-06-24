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
 *
 ******************************************************************************/

package com.gsma.rcs.core.cms.protocol.cmd;

import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.imaplib.imap.Part;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CmdHandler {

    /**
     * IMAP command definition
     */
    public enum CommandType {
        LIST, LIST_STATUS, SELECT_CONDSTORE, FETCH_FLAGS, FETCH_HEADERS, FETCH_MESSAGES_BODY, UID_SEARCH
    }

    /**
     * Return a IMAP command handler
     *
     * @param command the command type
     * @param capabilities the list of capabilities
     * @param params variable string parameters
     * @return the IMAP command handler
     */
    public static CmdHandler getHandler(CommandType command, List<String> capabilities,
            Object... params) throws ImapException {
        CmdHandler handler = null;
        switch (command) {
            case LIST:
                handler = new ListCmdHandler();
                break;
            case LIST_STATUS:
                handler = new ListStatusCmdHandler();
                break;
            case SELECT_CONDSTORE:
                handler = new SelectCondstoreCmdHandler();
                break;
            case FETCH_FLAGS:
                handler = new FetchFlagCmdHandler((String) params[0]);
                break;
            case FETCH_HEADERS:
                handler = new FetchHeaderCmdHandler();
                break;
            case FETCH_MESSAGES_BODY:
                handler = new FetchMessageCmdHandler();
                break;
            case UID_SEARCH:
                handler = new UidSearchCmdHandler();
                break;
        }
        if (handler.checkCapabilities(capabilities)) {
            return handler;
        }
        throw new ImapException("Command " + command + " failed: no capabilities "
                + Arrays.toString(capabilities.toArray()));
    }

    /**
     * Build an IMAP command
     * 
     * @param params the arguments used for the method call
     * @return the command
     */
    public abstract String buildCommand(Object... params);

    /**
     * Handle one line
     * 
     * @param oneLine the line
     * @return boolean true if handling is successful
     */
    public abstract boolean handleLine(String oneLine);

    /**
     * handle multiple lines
     * 
     * @param lines the list of lines
     */
    public abstract void handleLines(List<String> lines);

    /**
     * Handles part
     * 
     * @param part the part
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
