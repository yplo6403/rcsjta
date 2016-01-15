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

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.sync.strategy.FlagChange;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.Part;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dedicated class for handling FETCH Flag IMAP command
 */
public class FetchFlagCmdHandler extends CmdHandler {

    // TODO why not private
    static final String sCommand = Constants.CMD_FETCH_FLAGS;
    private static final String sPattern = "\\(UID ([0-9]+) FLAGS \\((.*)\\) MODSEQ \\(([0-9]+)\\)\\)$";

    private static final int sExpectedValues = 3;

    /* Map of UIDs with their associated command (map of parameter/value) */
    // TODO Give more explicit name
    final Map<Integer, Map<String, String>> mData;

    private final String mFolderName;

    /**
     * Constructor
     * 
     * @param folderName The folder
     */
    public FetchFlagCmdHandler(String folderName) {
        mFolderName = folderName;
        mData = new HashMap<>();
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
        Map<String, String> data = new HashMap<>();
        Integer uid = Integer.valueOf(values[0]);
        data.put(Constants.METADATA_UID, values[0]);
        data.put(Constants.METADATA_FLAGS, values[1]);
        data.put(Constants.METADATA_MODSEQ, values[2]);
        mData.put(uid, data);
        return true;
    }

    @Override
    public void handleLines(List<String> lines) {
    }

    @Override
    public void handlePart(Part part) {
    }

    @Override
    public List<FlagChange> getResult() {
        Set<Integer> readUids = new HashSet<>();
        Set<Integer> deletedUids = new HashSet<>();
        for (Map.Entry<Integer, Map<String, String>> entry : mData.entrySet()) {
            Integer uid = entry.getKey();
            Map<String, String> cmdArgs = entry.getValue();
            Set<Flag> flags = CmdUtils.parseFlags(cmdArgs.get(Constants.METADATA_FLAGS));
            if (flags.contains(Flag.Seen)) {
                readUids.add(uid);
            }
            if (flags.contains(Flag.Deleted)) {
                deletedUids.add(uid);
            }
        }
        List<FlagChange> flagChanges = new ArrayList<>();
        if (!deletedUids.isEmpty()) {
            flagChanges.add(new FlagChange(mFolderName, deletedUids, Flag.Deleted));
        }
        if (!readUids.isEmpty()) {
            flagChanges.add(new FlagChange(mFolderName, readUids, Flag.Seen));
        }
        return flagChanges;
    }

}
