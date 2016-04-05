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

package com.gsma.rcs.core.cms.sync.process;

import com.gsma.rcs.imaplib.imap.Flag;

import android.text.TextUtils;

import java.util.Set;

@SuppressWarnings("javadoc")
public class FlagChangeOperation {

    public enum Operation {
        ADD_FLAG, REMOVE_FLAG
    }

    private final String mFolder;
    /* UIDS are unique per folder */
    private final Set<Integer> mUids;
    private final Flag mFlag;
    private Operation mOperation;

    /**
     * Constructor
     * 
     * @param folder the folder
     * @param uids the set of UIDs
     * @param flag the flag
     */
    public FlagChangeOperation(String folder, Set<Integer> uids, Flag flag) {
        mFolder = folder;
        mUids = uids;
        mFlag = flag;
        mOperation = Operation.ADD_FLAG;
    }

     public String getJoinedUids() {
        return TextUtils.join(",", mUids);
    }

    public Set<Integer> getUids() {
        return mUids;
    }

    public Boolean isSeen() {
        return mOperation == Operation.ADD_FLAG && Flag.Seen == mFlag;
    }

    public Boolean isDeleted() {
        return mOperation == Operation.ADD_FLAG && Flag.Deleted == mFlag;
    }

    public String getFolder() {
        return mFolder;
    }

    public Flag getFlag() {
        return mFlag;
    }

    public Operation getOperation() {
        return mOperation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        FlagChangeOperation that = (FlagChangeOperation) o;

        return !(mFolder != null ? !mFolder.equals(that.mFolder) : that.mFolder != null)
                && !(mUids != null ? !mUids.equals(that.mUids) : that.mUids != null)
                && mFlag == that.mFlag && mOperation == that.mOperation;

    }

    @Override
    public int hashCode() {
        int result = mFolder != null ? mFolder.hashCode() : 0;
        result = 31 * result + (mUids != null ? mUids.hashCode() : 0);
        result = 31 * result + (mFlag != null ? mFlag.hashCode() : 0);
        result = 31 * result + (mOperation != null ? mOperation.hashCode() : 0);
        return result;
    }
}
