/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.cms;

import android.net.Uri;
import android.util.SparseArray;

/**
 * CMS IMAP data log constants
 */
public abstract class CmsData {

    /**
     * Database URI
     */
    /* package private */static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.rcs.cms.imap/message");

    /**
     * Mailbox name
     */
    /* package private */static final String KEY_FOLDER = "folder";

    /**
     * UID IMAP counter
     */
    /* package private */static final String KEY_UID = "uid";

    /**
     * IMAP Flag Seen
     */
    /* package private */static final String KEY_READ_STATUS = "readStatus";

    /**
     * IMAP Flag Deleted
     */
    /* package private */static final String KEY_DEL_STATUS = "delStatus";

    /**
     * Push status
     */
    /* package private */static final String KEY_PUSH_STATUS = "pushStatus";

    /**
     * Message Type
     */
    /* package private */static final String KEY_MSG_TYPE = "msgType";

    /**
     * Message Id
     */
    /* package private */static final String KEY_MSG_ID = "msgId";

    /**
     * Native provider Id
     */
    /* package private */static final String KEY_NATIVE_ID = "nativeId";

    /**
     * Chat Id
     */
    /* package private */static final String KEY_CHAT_ID = "chatId";

    public enum MessageType {
        SMS, MMS, MESSAGE_CPIM, CHAT_MESSAGE, IMDN, CPM_SESSION, GROUP_STATE, FILE_TRANSFER
    }

    /**
     * Read status of the message
     */
    public enum ReadStatus {
        /**
         * The message has not yet been displayed in the UI.
         */
        UNREAD(0), /**
         * The message has been displayed in the UI and not synchronized with the CMS
         * server
         */
        READ_REPORT_REQUESTED(1), /**
         * The message has been displayed in the UI and reported to CMS
         * server but CMS server has not acknowledged the report processing.
         */
        READ_REPORTED(2), /**
         * The message has been displayed in the UI and synchronized with the CMS
         * server
         */
        READ(3);

        private final int mValue;

        private static SparseArray<ReadStatus> mValueToEnum = new SparseArray<>();

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
         * @param value the valure representing the read status
         * @return instance
         */
        public static ReadStatus valueOf(int value) {
            ReadStatus entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class " + ReadStatus.class.getName()
                    + "." + value + "!");
        }
    }

    /**
     * Read status of the message
     */
    public enum DeleteStatus {
        /**
         * The message has not yet been deleted from the UI.
         */
        NOT_DELETED(0), /**
         * The message has been deleted from the UI but not synchronized with the
         * CMS server
         */
        DELETED_REPORT_REQUESTED(1), /**
         * The message has been deleted in the UI and reported to CMS
         * server but CMS server has not acknowledged the report processing.
         */
        DELETED_REPORTED(2), /**
         * The message has been deleted from the UI and synchronized with the
         * CMS server
         */
        DELETED(3);

        private final int mValue;

        private static SparseArray<DeleteStatus> mValueToEnum = new SparseArray<>();

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
         * @param value the valure representing the delete status
         * @return instance
         */
        public static DeleteStatus valueOf(int value) {
            DeleteStatus entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + DeleteStatus.class.getName() + "." + value + "!");
        }
    }

    /**
     * Push status of the message
     */
    public enum PushStatus {
        /**
         * The message should be pushed on CMS
         */
        PUSH_REQUESTED(0), /**
         * The message has been pushed on CMS
         */
        PUSHED(1);

        private final int mValue;

        private static SparseArray<PushStatus> mValueToEnum = new SparseArray<>();

        static {
            for (PushStatus entry : PushStatus.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        PushStatus(int value) {
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
         * @param value the valure representing the push status
         * @return instance
         */
        public static PushStatus valueOf(int value) {
            PushStatus entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class " + PushStatus.class.getName()
                    + "." + value + "!");
        }
    }

}
