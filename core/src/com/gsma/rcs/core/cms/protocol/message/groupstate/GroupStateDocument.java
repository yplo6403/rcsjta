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

package com.gsma.rcs.core.cms.protocol.message.groupstate;

import com.gsma.rcs.utils.DateUtils;
import com.gsma.services.rcs.contact.ContactId;

import java.util.List;

public class GroupStateDocument {

    public static final String GROUP_STATE_ELEMENT = "groupstate";
    public static final String TIMESTAMP_ATTR = "timestamp";
    public static final String LASTFOCUSSESSIONID_ATTR = "lastfocussessionid";

    public static final String PARTICIPANT_ELEMENT = "participant";
    public static final String COMM_ADDR_ATTR = "comm-addr";

    private final String mTimestamp;
    private final String mLastFocusSessionId;
    private final List<ContactId> mParticipants;

    public GroupStateDocument(String lastFocusSessionId, String timestamp,
            List<ContactId> participants) {
        mLastFocusSessionId = lastFocusSessionId;
        mTimestamp = timestamp;
        mParticipants = participants;
    }

    public String getLastFocusSessionId() {
        return mLastFocusSessionId;
    }

    public String getTimestamp() {
        return mTimestamp;
    }

    public List<ContactId> getParticipants() {
        return mParticipants;
    }

    public long getDate() {
        return DateUtils.decodeDate(mTimestamp);
    }
}
