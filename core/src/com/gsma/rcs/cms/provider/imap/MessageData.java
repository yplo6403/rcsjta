/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.cms.provider.imap;

import android.net.Uri;

/**
 * CMS IMAP data constants
 */
public final class MessageData {

    /**
     * Database URI
     */
    /* package private */static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.rcs.cms.imap/message");

    /**
     * Mailbox name
     */
    /* package private */static final String KEY_FOLDER_NAME = "foldername";

    /**
     * HIGHESTMODESEQ IMAP counter
     */
    /* package private */static final String KEY_MODESQ = "modeseq";

    /**
     * UID IMAP counter
     */
    /* package private */static final String KEY_UID = "uid";

    /**
     * IMAP Flag Seen
     */
    /* package private */static final String KEY_FLAG_SEEN = "seen";

    /**
     * IMAP Flag Deleted
     */
    /* package private */static final String KEY_FLAG_DELETED = "deleted";

    /**
     * RCS Message Type
     */
    /* package private */static final String KEY_MESSAGE_TYPE = "messageType";

    /**
     * RCS Message Id
     */
    /* package private */static final String KEY_MESSAGE_ID = "messageId";

    public static enum MessageType {
        SMS, MMS, ONETOONE, GC
    }

    private String mFolder;
    private Integer mModseq;
    private Integer mUid;
    private Boolean mSeen;
    private Boolean mDeleted;
    private MessageType mMessageType;
    private String mMessageId;

    /**
     * @param folder
     * @param modseq
     * @param uid
     * @param seen
     * @param deleted
     * @param messageType
     * @param messageId
     */
    public MessageData(String folder, Integer modseq, Integer uid, boolean seen, boolean deleted,
            MessageType messageType, String messageId) {
        super();
        this.mFolder = folder;
        this.mModseq = modseq;
        this.mUid = uid;
        this.mSeen = seen;
        this.mDeleted = deleted;
        this.mMessageType = messageType;
        this.mMessageId = messageId;
    }

    public String getFolder() {
        return mFolder;
    }

    public Integer getModseq() {
        return mModseq;
    }

    public Integer getUid() {
        return mUid;
    }

    public Boolean getSeen() {
        return mSeen;
    }

    public void setSeen(boolean seen) {
        mSeen = seen;
    }

    public Boolean getDeleted() {
        return mDeleted;
    }

    public void setDeleted(boolean deleted) {
        mDeleted = deleted;
    }

    public MessageType getMessageType() {
        return mMessageType;
    }

    public String getMessageId() {
        return mMessageId;
    }

    @Override
    public String toString() {
        return "MessageData [mFolder=" + mFolder + ", mModseq=" + mModseq + ", mUid=" + mUid
                + ", mSeen=" + mSeen + ", mDeleted=" + mDeleted + ", mMessageType=" + mMessageType
                + ", mMessageId=" + mMessageId + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mDeleted == null) ? 0 : mDeleted.hashCode());
        result = prime * result + ((mFolder == null) ? 0 : mFolder.hashCode());
        result = prime * result + ((mMessageId == null) ? 0 : mMessageId.hashCode());
        result = prime * result + ((mMessageType == null) ? 0 : mMessageType.hashCode());
        result = prime * result + ((mModseq == null) ? 0 : mModseq.hashCode());
        result = prime * result + ((mSeen == null) ? 0 : mSeen.hashCode());
        result = prime * result + ((mUid == null) ? 0 : mUid.hashCode());
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
        MessageData other = (MessageData) obj;
        if (mDeleted == null) {
            if (other.mDeleted != null)
                return false;
        } else if (!mDeleted.equals(other.mDeleted))
            return false;
        if (mFolder == null) {
            if (other.mFolder != null)
                return false;
        } else if (!mFolder.equals(other.mFolder))
            return false;
        if (mMessageId == null) {
            if (other.mMessageId != null)
                return false;
        } else if (!mMessageId.equals(other.mMessageId))
            return false;
        if (mMessageType != other.mMessageType)
            return false;
        if (mModseq == null) {
            if (other.mModseq != null)
                return false;
        } else if (!mModseq.equals(other.mModseq))
            return false;
        if (mSeen == null) {
            if (other.mSeen != null)
                return false;
        } else if (!mSeen.equals(other.mSeen))
            return false;
        if (mUid == null) {
            if (other.mUid != null)
                return false;
        } else if (!mUid.equals(other.mUid))
            return false;
        return true;
    }

}
