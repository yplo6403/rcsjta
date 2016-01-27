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

package com.gsma.rcs.cms.imap;

import com.gsma.rcs.cms.Constants;

import java.util.Map;

public class ImapFolder {

    private String mName;
    private Map<String, String> mCounters;

    /**
     * Default constructor
     * 
     * @param name
     * @param counters
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
     * @return messages
     */
    public Integer getMessages() {
        return getValueAsInteger(Constants.METADATA_MESSAGES);
    }

    public boolean isEmpty() {
        return 0 == getValueAsInteger(Constants.METADATA_MESSAGES);
    }

    @Override
    public String toString() {
        return new StringBuilder("ImapFolder [mName=").append(mName).append(", ")
                .append(Constants.METADATA_MESSAGES).append("=").append(getMessages()).append(", ")
                .append(Constants.METADATA_UIDVALIDITY).append("=").append(getUidValidity())
                .append(", ").append(Constants.METADATA_HIGHESTMODSEQ).append("=")
                .append(getHighestModseq()).append(", ").append(Constants.METADATA_UIDNEXT)
                .append("=").append(getUidNext()).append(", ").append("]").toString();

    }

    private Integer getValueAsInteger(String key) {
        String val = mCounters.get(key);
        return (val == null) ? null : Integer.valueOf(val);
    }
}
