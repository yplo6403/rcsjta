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

package com.gsma.rcs.cms.imap.cmd;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.ImapFolder;

import com.sonymobile.rcs.imap.Part;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Dedicated class for handling LIST STATUS IMAP command
 */
public class ListStatusCmdHandler extends CmdHandler {

    static final String sCommand = Constants.CMD_LIST_STATUS;
    private static final String sPattern = "^STATUS (.*) \\(MESSAGES ([0-9]+) UIDNEXT ([0-9]+) UIDVALIDITY ([0-9]+) HIGHESTMODSEQ ([0-9]+)\\)$";

    private static final int sExpectedValues = 5;

    final Map<String, Map<String, String>> mData = new HashMap<String, Map<String, String>>();

    @Override
    public String buildCommand(Object... params) {
        return sCommand;
    }

    @Override
    public boolean handleLine(String oneLine) {
        return false;
    }

    @Override
    public void handleLines(List<String> lines) {

        for (String line : lines) {
            line = line.substring(2).trim();

            String[] values = extractCounterValuesFromLine(sPattern, line);

            if (values == null || values.length != sExpectedValues) {
                continue;
            }

            Map<String, String> data = new HashMap<String, String>();
            data.put(Constants.METADATA_MESSAGES, values[1]);
            data.put(Constants.METADATA_UIDNEXT, values[2]);
            data.put(Constants.METADATA_UIDVALIDITY, values[3]);
            data.put(Constants.METADATA_HIGHESTMODSEQ, values[4]);
            mData.put(values[0], data);
        }
    }

    @Override
    public void handlePart(Part part) {
    }

    @Override
    public List<ImapFolder> getResult() {
        List<ImapFolder> folders = new ArrayList<ImapFolder>();
        Iterator<Entry<String, Map<String, String>>> iter1 = mData.entrySet().iterator();
        while (iter1.hasNext()) {
            Entry<String, Map<String, String>> entry1 = iter1.next();
            String folderName = entry1.getKey();
            Map<String, String> counters = entry1.getValue();
            folders.add(new ImapFolder(folderName, counters));
        }
        return folders;
    }
}
