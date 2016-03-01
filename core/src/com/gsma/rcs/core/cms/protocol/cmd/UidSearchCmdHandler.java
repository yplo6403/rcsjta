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

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated class for handling UID SEARCH IMAP command
 */
public class UidSearchCmdHandler extends CmdHandler {

    static final String sCommand = Constants.CMD_UID_SEARCH;
    private static final String sPattern = "^\\* SEARCH (.*)$";

    private static final int sExpectedValues = 1;

    private List<Integer> mUids;

    /**
     * Constructor
     */
    public UidSearchCmdHandler() {
        mUids = new ArrayList<>();
    }

    @Override
    public String buildCommand(Object... params) {
        return String.format(sCommand, params);
    }

    @Override
    public boolean handleLine(String oneLine) {
        String[] values = extractCounterValuesFromLine(sPattern, oneLine);
        if (values == null || values.length != sExpectedValues) {
            return false;
        }
        String[] uids = values[0].trim().split(" ");
        for (String uid : uids) {
            mUids.add(Integer.parseInt(uid));
        }
        return true;
    }

    @Override
    public void handleLines(List<String> lines) {
    }

    @Override
    public void handlePart(Part part) {
    }

    @Override
    public List<Integer> getResult() {
        return mUids;
    }

}
