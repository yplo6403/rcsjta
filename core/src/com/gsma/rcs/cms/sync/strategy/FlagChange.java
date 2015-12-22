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

package com.gsma.rcs.cms.sync.strategy;

import com.gsma.rcs.cms.utils.ListUtils;

import com.sonymobile.rcs.imap.Flag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("javadoc")
public class FlagChange {

    public enum Operation {
        ADD_FLAG, REMOVE_FLAG
    };

    private String mFolder;
    private List<Integer> mUids;
    private Set<Flag> mFlags;
    private Operation mOperation = Operation.ADD_FLAG;

    /**
     * @param folder
     * @param uids
     * @param flags
     */
    public FlagChange(String folder, List<Integer> uids, Set<Flag> flags) {
        super();
        mFolder = folder;
        mUids = uids;
        mFlags = flags;
    }

    /**
     * @param folder
     * @param uids
     * @param flag
     */
    public FlagChange(String folder, List<Integer> uids, Flag flag) {
        super();
        mFolder = folder;
        mUids = uids;
        mFlags = new HashSet<>(Arrays.asList(flag));
    }

    public FlagChange(String folder, Integer uid, Flag flag, Operation operation) {
        super();
        mFolder = folder;
        mUids = Arrays.asList(new Integer[] {
            uid
        });
        mFlags = new HashSet<>(Arrays.asList(flag));
        mOperation = operation;
    }

    public String getJoinedUids() {
        return ListUtils.join(mUids, ",");
    }

    public List<Integer> getUids() {
        return mUids;
    }

    public Boolean addSeenFlag() {
        return mOperation == Operation.ADD_FLAG && mFlags.contains(Flag.Seen);
    }

    public Boolean addDeletedFlag() {
        return mOperation == Operation.ADD_FLAG && mFlags.contains(Flag.Deleted);
    }

    public String getFolder() {
        return mFolder;
    }

    public Flag[] getFlags() {
        return mFlags.toArray(new Flag[mFlags.size()]);
    }

    public Operation getOperation() {
        return mOperation;
    }

}
