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

import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;
import com.sonymobile.rcs.imap.Part;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Dedicated class for handling FETCH Headers IMAP command
 */
public class FetchHeaderCmdHandler extends CmdHandler {

    static final String sCommand = Constants.CMD_FETCH_HEADERS;
    private static final String sFlagsPattern = "^\\* [0-9]+ FETCH \\(UID ([0-9]+) FLAGS \\((.*)\\) MODSEQ \\(([0-9]+)\\).*$";

    private static final int sExpectedValuesForFlag = 3;

    private Integer mUid;
    final Map<Integer, Map<String, String>> mData = new TreeMap<Integer, Map<String, String>>();
    protected Map<Integer, Part> mPart = new HashMap<Integer, Part>();

    @Override
    public String buildCommand(Object... params) {
        return String.format(sCommand, params);
    }

    @Override
    public boolean handleLine(String oneLine) {

        String[] values = extractCounterValuesFromLine(sFlagsPattern, oneLine);
        if (values != null && values.length == sExpectedValuesForFlag) {
            Map<String, String> data = new HashMap<String, String>();
            mUid = Integer.parseInt(values[0]);
            data.put(Constants.METADATA_UID, values[0]);
            data.put(Constants.METADATA_FLAGS, values[1]);
            data.put(Constants.METADATA_MODSEQ, values[2]);
            mData.put(mUid, data);
            return true;
        }
        return false;
    }

    @Override
    public void handleLines(List<String> lines) {
    }

    @Override
    public void handlePart(Part part) {
        mPart.put(mUid, part);
    }

    @Override
    public List<ImapMessage> getResult() {

        List<ImapMessage> messages = new ArrayList<ImapMessage>();
        Iterator<Entry<Integer, Map<String, String>>> iter = mData.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<Integer, Map<String, String>> entry = iter.next();
            Integer uid = entry.getKey();
            ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
            CmdUtils.fillFlags(metadata.getFlags(), entry.getValue().get(Constants.METADATA_FLAGS));
            messages.add(new ImapMessage(uid, metadata, mPart.get(uid)));
        }
        Collections.reverse(messages); // highest uids first
        return messages;
    }
}
