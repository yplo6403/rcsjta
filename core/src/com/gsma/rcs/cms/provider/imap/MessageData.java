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
import android.util.SparseArray;

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
     * UID IMAP counter
     */
    /* package private */static final String KEY_UID = "uid";

    /**
     * IMAP Flag Seen
     */
    /* package private */static final String KEY_READ_STATUS = "read_status";

    /**
     * IMAP Flag Deleted
     */
    /* package private */static final String KEY_DELETE_STATUS = "delete_status";

    /**
     * Push status
     */
    /* package private */static final String KEY_PUSH_STATUS = "push_status";

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

    /**
     * Read status of the message
     */
    public enum ReadStatus {
        /**
         * The message has not yet been displayed in the UI.
         */
        UNREAD(0),
        /**
         * The message has been displayed in the UI and not synchronized with the CMS server
         */
        READ_REPORT_REQUESTED(1),
        /**
         * The message has been displayed in the UI and synchronized with the CMS server
         */
        READ(2);


        private final int mValue;

        private static SparseArray<ReadStatus> mValueToEnum = new SparseArray<ReadStatus>();
        static {
            for (ReadStatus entry : ReadStatus.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private ReadStatus(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to ReadStatus instance
         *
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a ReadStatus instance for the specified integer value.
         *
         * @param value
         * @return instance
         */
        public final static ReadStatus valueOf(int value) {
            ReadStatus entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(ReadStatus.class.getName()).append(".").append(value).append("!")
                    .toString());
        }
    }

    /**
     * Read status of the message
     */
    public enum DeleteStatus {
        /**
         * The message has not yet been deleted from the UI.
         */
        NOT_DELETED(0),
        /**
         * The message has been deleted from the UI but not synchronized with the CMS server
         */
        DELETED_REPORT_REQUESTED(1),
        /**
         * The message has been deleted from the UI and synchronized with the CMS server
         */
        DELETED(2);


        private final int mValue;

        private static SparseArray<DeleteStatus> mValueToEnum = new SparseArray<DeleteStatus>();
        static {
            for (DeleteStatus entry : DeleteStatus.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private DeleteStatus(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to ReadStatus instance
         *
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a ReadStatus instance for the specified integer value.
         *
         * @param value
         * @return instance
         */
        public final static DeleteStatus valueOf(int value) {
            DeleteStatus entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(DeleteStatus.class.getName()).append(".").append(value).append("!")
                    .toString());
        }
    }

    /**
     * Push status of the message
     */
    public enum PushStatus {
        /**
         * The message should be pushed on CMS
         */
        PUSH_REQUESTED(0),
        /**
         * The message has been pushed on CMS
         */
        PUSHED(1);


        private final int mValue;

        private static SparseArray<PushStatus> mValueToEnum = new SparseArray<PushStatus>();
        static {
            for (PushStatus entry : PushStatus.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private PushStatus(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to ReadStatus instance
         *
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a ReadStatus instance for the specified integer value.
         *
         * @param value
         * @return instance
         */
        public final static PushStatus valueOf(int value) {
            PushStatus entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(PushStatus.class.getName()).append(".").append(value).append("!")
                    .toString());
        }
    }

    public static final Integer INVALID_UID = 0;
    private String mFolder;
    private Integer mUid;
    private ReadStatus mReadStatus;
    private DeleteStatus mDeleteStatus;
    private PushStatus mPushStatus = PushStatus.PUSHED;
    private MessageType mMessageType;
    private String mMessageId;

    /**
     * @param readStatus
     * @param deleteStatus
     * @param messageType
     * @param messageId
     */
    public MessageData(String folder, ReadStatus readStatus, DeleteStatus deleteStatus, PushStatus pushStatus,
                       MessageType messageType, String messageId) {
        super();
        mFolder = folder;
        mReadStatus = readStatus;
        mDeleteStatus = deleteStatus;
        mPushStatus = pushStatus;
        mMessageType = messageType;
        mMessageId = messageId;
    }

    /**
     * @param folder
     * @param uid
     * @param readStatus
     * @param deleteStatus
     * @param messageType
     * @param messageId
     */
    public MessageData(String folder, Integer uid, ReadStatus readStatus, DeleteStatus deleteStatus, PushStatus pushStatus,
            MessageType messageType, String messageId) {
        super();
        mFolder = folder;
        mUid = uid;
        mReadStatus = readStatus;
        mDeleteStatus = deleteStatus;
        mPushStatus = pushStatus;
        mMessageType = messageType;
        mMessageId = messageId;
    }

    public String getFolder() {
        return mFolder;
    }

    public Integer getUid() {
        return mUid;
    }

    public ReadStatus getReadStatus() {
        return mReadStatus;
    }

    public void setReadStatus(ReadStatus readStatus) {
        mReadStatus = readStatus;
    }

    public DeleteStatus getDeleteStatus() {
        return mDeleteStatus;
    }

    public void setDeleteStatus(DeleteStatus deleteStatus) {
        mDeleteStatus = deleteStatus;
    }

    public MessageType getMessageType() {
        return mMessageType;
    }

    public String getMessageId() {
        return mMessageId;
    }

    public PushStatus getPushStatus() {
        return mPushStatus;
    }

    public void setPushStatus(PushStatus mPushStatus) {
        this.mPushStatus = mPushStatus;
    }


    @Override
    public String toString() {
        return "MessageData [mFolder=" + mFolder + ", mUid=" + mUid
                + ", mSeen=" + mReadStatus + ", mDeleted=" + mDeleteStatus + ", mMessageType=" + mMessageType
                + ", mMessageId=" + mMessageId + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mDeleteStatus == null) ? 0 : mDeleteStatus.hashCode());
        result = prime * result + ((mFolder == null) ? 0 : mFolder.hashCode());
        result = prime * result + ((mMessageId == null) ? 0 : mMessageId.hashCode());
        result = prime * result + ((mMessageType == null) ? 0 : mMessageType.hashCode());
        result = prime * result + ((mReadStatus == null) ? 0 : mReadStatus.hashCode());
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
        if (mDeleteStatus == null) {
            if (other.mDeleteStatus != null)
                return false;
        } else if (!mDeleteStatus.equals(other.mDeleteStatus))
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
        if (mReadStatus == null) {
            if (other.mReadStatus != null)
                return false;
        } else if (!mReadStatus.equals(other.mReadStatus))
            return false;
        if (mUid == null) {
            if (other.mUid != null)
                return false;
        } else if (!mUid.equals(other.mUid))
            return false;
        return true;
    }

}
