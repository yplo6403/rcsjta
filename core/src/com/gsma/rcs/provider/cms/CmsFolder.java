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

package com.gsma.rcs.provider.cms;

import android.net.Uri;

/**
 * CMS IMAP data constants
 */
public final class CmsFolder {

    /**
     * Database URI
     */
    /* package private */static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.rcs.cms.imap/folder");

    /**
     * Mailbox name
     */
    /* package private */static final String KEY_NAME = "name";

    /**
     * NextUid counter
     */
    /* package private */static final String KEY_NEXT_UID = "nextuid";

    /**
     * HIGHESTMODSEQ IMAP counter
     */
    /* package private */static final String KEY_HIGHESTMODSEQ = "highestmodseq";

    /**
     * UID Validity IMAP counter
     */
    /* package private */static final String KEY_UID_VALIDITY = "uidValidity";

    private String mName;
    private Integer mNextUid;
    private Integer mModseq;
    private Integer mUidValidity;
    private Integer mMaxUid;

    /**
     * @param name
     */
    public CmsFolder(String name) {
        super();
        this.mName = name;
        resetCounters();
    }

    /**
     * @param name
     * @param nextUid
     * @param modseq
     * @param uidValidity
     */
    public CmsFolder(String name, Integer nextUid, Integer modseq, Integer uidValidity) {
        super();
        mName = name;
        mNextUid = nextUid;
        mModseq = modseq;
        mUidValidity = uidValidity;
    }

    public void resetCounters(){
        mNextUid = mModseq = mUidValidity = mMaxUid = 0;
    }

    /**
     * @return boolean
     */
    public boolean isNewFolder() {
        return mNextUid.equals(0);
    }

    /**
     * @return boolean
     */
    public boolean hasMessages() {
        return !mMaxUid.equals(0);
    }

    /**
     * @return mName
     */
    public String getName() {
        return mName;
    }

    /**
     * @return mNextUid
     */
    public Integer getNextUid() {
        return mNextUid;
    }

    /**
     * @return mModseq
     */
    public Integer getModseq() {
        return mModseq;
    }

    /**
     * @return mUidValidity
     */
    public Integer getUidValidity() {
        return mUidValidity;
    }

    /**
     * @return mMaxUid
     */
    public Integer getMaxUid() {
        return mMaxUid;
    }

    /**
     * @param maxUid
     */
    public void setMaxUid(Integer maxUid) {
        mMaxUid = maxUid;
    }

    @Override
    public String toString() {
        return "CmsFolder [mName=" + mName + ", mNextUid=" + mNextUid + ", mModseq=" + mModseq
                + ", mUidValidity=" + mUidValidity + ", mMaxUid=" + mMaxUid + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mMaxUid == null) ? 0 : mMaxUid.hashCode());
        result = prime * result + ((mModseq == null) ? 0 : mModseq.hashCode());
        result = prime * result + ((mName == null) ? 0 : mName.hashCode());
        result = prime * result + ((mNextUid == null) ? 0 : mNextUid.hashCode());
        result = prime * result + ((mUidValidity == null) ? 0 : mUidValidity.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CmsFolder other = (CmsFolder) obj;
        if (mMaxUid == null) {
            if (other.mMaxUid != null)
                return false;
        } else if (!mMaxUid.equals(other.mMaxUid))
            return false;
        if (mModseq == null) {
            if (other.mModseq != null)
                return false;
        } else if (!mModseq.equals(other.mModseq))
            return false;
        if (mName == null) {
            if (other.mName != null)
                return false;
        } else if (!mName.equals(other.mName))
            return false;
        if (mNextUid == null) {
            if (other.mNextUid != null)
                return false;
        } else if (!mNextUid.equals(other.mNextUid))
            return false;
        if (mUidValidity == null) {
            if (other.mUidValidity != null)
                return false;
        } else if (!mUidValidity.equals(other.mUidValidity))
            return false;
        return true;
    }

}
