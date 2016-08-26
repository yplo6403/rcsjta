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

import java.util.Map;

public class ImapFolder {

    private String mName;
    /**
     * The map of counter name (like UIDNEXT, MODSEQ or UIDVALIDITY) associated with its value.
     */
    private Map<String, String> mCounters;

    /**
     * Default constructor
     * 
     * @param name the name
     * @param counters the counters
     */
    public ImapFolder(String name, Map<String, String> counters) {
        super();
        mName = name;
        mCounters = counters;
    }

    /**
     * @return name
     */
    public String getName() {
        return mName;
    }

    /**
     * @return uidvalidity
     */
    public Integer getUidValidity() {
        return getValueAsInteger(Constants.METADATA_UIDVALIDITY);
    }

    /**
     * @return HighestModseq
     */
    public Integer getHighestModseq() {
        return getValueAsInteger(Constants.METADATA_HIGHESTMODSEQ);
    }

    /**
     * @return UidNext
     */
    public Integer getUidNext() {
        return getValueAsInteger(Constants.METADATA_UIDNEXT);
    }

    /**
     * Gets the number of messages
     * 
     * @return the number of messages
     */
    public Integer getMessageCount() {
        return getValueAsInteger(Constants.METADATA_MESSAGES);
    }

    /**
     * Checks if the folder is empty
     * 
     * @return true if the folder is empty
     */
    public boolean isEmpty() {
        Integer numberOfMessages = getValueAsInteger(Constants.METADATA_MESSAGES);
        return numberOfMessages == null || numberOfMessages == 0;
    }

    @Override
    public String toString() {
        return "ImapFolder [mName=" + mName + ", " + Constants.METADATA_MESSAGES + "="
                + getMessageCount() + ", " + Constants.METADATA_UIDVALIDITY + "="
                + getUidValidity() + ", " + Constants.METADATA_HIGHESTMODSEQ + "="
                + getHighestModseq() + ", " + Constants.METADATA_UIDNEXT + "=" + getUidNext()
                + ", " + "]";
    }

    private Integer getValueAsInteger(String key) {
        String val = mCounters.get(key);
        return (val == null) ? null : Integer.valueOf(val);
    }
}
