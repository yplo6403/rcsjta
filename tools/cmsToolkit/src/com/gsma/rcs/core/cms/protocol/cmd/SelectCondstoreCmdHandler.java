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

import com.gsma.rcs.core.cms.Constants;

import com.gsma.rcs.imaplib.imap.Part;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated class for handling SELECT CONDSTORE IMAP command
 */
public class SelectCondstoreCmdHandler extends CmdHandler {

    static final String sCommand = Constants.CMD_SELECT_CONDSTORE;
    private static final String sPattern = "OK \\[(UIDVALIDITY|UIDNEXT|HIGHESTMODSEQ) ([0-9]+)\\]";

    private static final int sExpectedValues = 2;

    private String mFolderName;

    final Map<String, Map<String, String>> mData = new HashMap<String, Map<String, String>>();

    @Override
    public String buildCommand(Object... params) {
        mFolderName = String.valueOf(params[0]);
        return String.format(sCommand, params);
    }

    @Override
    public boolean handleLine(String oneLine) {
        return false;
    }

    @Override
    public void handleLines(List<String> lines) {

        Map<String, String> data = new HashMap<String, String>();
        for (String line : lines) {
            line = line.substring(2).trim();

            String[] values = extractCounterValuesFromLine(sPattern, line);

            if (values == null || values.length != sExpectedValues) {
                continue;
            }
            data.put(values[0], values[1]);
        }
        mData.put(mFolderName, data);
    }

    @Override
    public boolean checkCapabilities(List<String> capabilities) {
        return capabilities.contains(Constants.CAPA_CONDSTORE);
    }

    @Override
    public void handlePart(Part part) {
    }

    @Override
    public Object getResult() {
        return null;
    }
}
