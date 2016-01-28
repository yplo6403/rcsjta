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

    /**
     * RCS Message Id
     */
    /* package private */static final String KEY_NATIVE_PROVIDER_ID = "nativeProviderId";

    public enum MessageType {
        SMS, MMS, MESSAGE_CPIM, CHAT_MESSAGE, IMDN, SESSION_INFO, GROUP_STATE, FILE_TRANSFER
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

        ReadStatus(int value) {
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

        DeleteStatus(int value) {
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

    private String mFolder;
    private Integer mUid;
    private ReadStatus mReadStatus;
    private DeleteStatus mDeleteStatus;
    private PushStatus mPushStatus = PushStatus.PUSHED;
    private MessageType mMessageType;
    private String mMessageId;
    private Long mNativeProviderId;

    /**
     * @param readStatus
     * @param deleteStatus
     * @param messageType
     * @param messageId
     */
    public MessageData(String folder, ReadStatus readStatus, DeleteStatus deleteStatus,
            PushStatus pushStatus, MessageType messageType, String messageId, Long nativeProviderId) {
        super();
        mFolder = folder;
        mReadStatus = readStatus;
        mDeleteStatus = deleteStatus;
        mPushStatus = pushStatus;
        mMessageType = messageType;
        mMessageId = messageId;
        mNativeProviderId = nativeProviderId;
    }

    /**
     * @param folder
     * @param uid
     * @param readStatus
     * @param deleteStatus
     * @param messageType
     * @param messageId
     */
    public MessageData(String folder, Integer uid, ReadStatus readStatus,
            DeleteStatus deleteStatus, PushStatus pushStatus, MessageType messageType,
            String messageId, Long nativeProviderId) {
        super();
        mFolder = folder;
        mUid = uid;
        mReadStatus = readStatus;
        mDeleteStatus = deleteStatus;
        mPushStatus = pushStatus;
        mMessageType = messageType;
        mMessageId = messageId;
        mNativeProviderId = nativeProviderId;
    }

    public MessageData(Long nativeProviderId, ReadStatus readStatus, DeleteStatus deleteStatus) {
        super();
        mNativeProviderId = nativeProviderId;
        mReadStatus = readStatus;
        mDeleteStatus = deleteStatus;
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

    public Long getNativeProviderId() {
        return mNativeProviderId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MessageData{");
        sb.append("mFolder='").append(mFolder).append('\'');
        sb.append(", mUid=").append(mUid);
        sb.append(", mReadStatus=").append(mReadStatus);
        sb.append(", mDeleteStatus=").append(mDeleteStatus);
        sb.append(", mPushStatus=").append(mPushStatus);
        sb.append(", mMessageType=").append(mMessageType);
        sb.append(", mMessageId='").append(mMessageId).append('\'');
        sb.append(", mNativeProviderId=").append(mNativeProviderId);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        MessageData that = (MessageData) o;

        if (!mFolder.equals(that.mFolder))
            return false;
        if (mUid != null ? !mUid.equals(that.mUid) : that.mUid != null)
            return false;
        if (mReadStatus != that.mReadStatus)
            return false;
        if (mDeleteStatus != that.mDeleteStatus)
            return false;
        if (mPushStatus != that.mPushStatus)
            return false;
        if (mMessageType != that.mMessageType)
            return false;
        if (!mMessageId.equals(that.mMessageId))
            return false;
        return !(mNativeProviderId != null ? !mNativeProviderId.equals(that.mNativeProviderId)
                : that.mNativeProviderId != null);

    }

    @Override
    public int hashCode() {
        int result = mFolder.hashCode();
        result = 31 * result + (mUid != null ? mUid.hashCode() : 0);
        result = 31 * result + mReadStatus.hashCode();
        result = 31 * result + mDeleteStatus.hashCode();
        result = 31 * result + mPushStatus.hashCode();
        result = 31 * result + mMessageType.hashCode();
        result = 31 * result + mMessageId.hashCode();
        result = 31 * result + (mNativeProviderId != null ? mNativeProviderId.hashCode() : 0);
        return result;
    }
}
