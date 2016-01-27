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
 ******************************************************************************/

package com.gsma.rcs.cms.provider.imap;

import com.gsma.rcs.cms.provider.imap.MessageData.DeleteStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;

import java.util.HashMap;
import java.util.Map;

public class ImapLogEnvIntegration {

    private static final String SORT_BY_UID_ASC = new StringBuilder(MessageData.KEY_UID).append(
            " ASC").toString();

    protected final LocalContentResolver mLocalContentResolver;

    /**
     * Current instance
     */
    private static volatile ImapLogEnvIntegration sInstance;

    private ImapLogEnvIntegration(Context context) {
        mLocalContentResolver = new LocalContentResolver(context.getContentResolver());
    }

    public static ImapLogEnvIntegration getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (ImapLogEnvIntegration.class) {
            if (sInstance == null) {
                sInstance = new ImapLogEnvIntegration(context);
            }
        }
        return sInstance;
    }

    /**
     * Get messages by folderName
     *
     * @param folderName
     * @return MessageData
     */
    public Map<Integer, MessageData> getMessages(String folderName) {
        Cursor cursor = null;
        Map<Integer, MessageData> messages = new HashMap<Integer, MessageData>();
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                    ImapLog.Message.SELECTION_FOLDER_NAME, new String[] {
                        folderName
                    }, SORT_BY_UID_ASC);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);

            int uidIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_UID);
            int folderIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_FOLDER_NAME);
            int seenIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_READ_STATUS);
            int deletedIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_DELETE_STATUS);
            int pushIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_PUSH_STATUS);
            int messageTypeIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_TYPE);
            int messageIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
            int nativeProviderIdIdx = cursor
                    .getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID);

            while (cursor.moveToNext()) {
                Integer uid = cursor.isNull(uidIdx) ? null : cursor.getInt(uidIdx);
                messages.put(
                        uid,
                        new MessageData(cursor.getString(folderIdx), uid, MessageData.ReadStatus
                                .valueOf(cursor.getInt(seenIdx)), DeleteStatus.valueOf(cursor
                                .getInt(deletedIdx)), PushStatus.valueOf(cursor.getInt(pushIdx)),
                                MessageType.valueOf(cursor.getString(messageTypeIdx)), cursor
                                        .getString(messageIdIdx), cursor
                                        .isNull(nativeProviderIdIdx) ? null : cursor
                                        .getLong(nativeProviderIdIdx)));
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get folder by name
     *
     * @param folderName
     * @return FolderData
     */
    public FolderData getFolder(String folderName) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(FolderData.CONTENT_URI, null,
                    ImapLog.Folder.SELECTION_NAME, new String[] {
                        folderName
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, FolderData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }

            return new FolderData(folderName, cursor.getInt(cursor
                    .getColumnIndexOrThrow(FolderData.KEY_NEXT_UID)), cursor.getInt(cursor
                    .getColumnIndexOrThrow(FolderData.KEY_HIGHESTMODSEQ)), cursor.getInt(cursor
                    .getColumnIndexOrThrow(FolderData.KEY_UID_VALIDITY)));
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get message by folderName and messageId
     *
     * @param folderName
     * @param messageId
     * @return MessageData
     */
    public MessageData getMessage(String folderName, String messageId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                    ImapLog.Message.SELECTION_FOLDER_NAME_MESSAGEID, new String[] {
                            folderName, messageId
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            int uidIxd = cursor.getColumnIndexOrThrow(MessageData.KEY_UID);
            return new MessageData(
                    folderName,
                    cursor.isNull(uidIxd) ? null : cursor.getInt(uidIxd),
                    ReadStatus.valueOf(cursor.getInt(cursor
                            .getColumnIndexOrThrow(MessageData.KEY_READ_STATUS))),
                    DeleteStatus.valueOf(cursor.getInt(cursor
                            .getColumnIndexOrThrow(MessageData.KEY_DELETE_STATUS))),
                    PushStatus.valueOf(cursor.getInt(cursor
                            .getColumnIndexOrThrow(MessageData.KEY_PUSH_STATUS))),
                    MessageType.valueOf(cursor.getString(cursor
                            .getColumnIndexOrThrow(MessageData.KEY_MESSAGE_TYPE))),
                    cursor.getString(cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID)),
                    cursor.isNull(cursor.getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID)) ? null
                            : cursor.getLong(cursor
                                    .getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID)));
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get messages
     *
     * @return MessageData
     */
    public Map<Integer, MessageData> getMessages() {
        Cursor cursor = null;
        Map<Integer, MessageData> messages = new HashMap<>();
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);

            int idIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            int uidIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_UID);
            int folderIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_FOLDER_NAME);
            int seenIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_READ_STATUS);
            int deletedIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_DELETE_STATUS);
            int pushIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_PUSH_STATUS);
            int messageTypeIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_TYPE);
            int messageIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
            int nativeProviderIdIdx = cursor
                    .getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                messages.put(
                        cursor.getInt(idIdx),
                        new MessageData(cursor.getString(folderIdx), cursor.isNull(uidIdx) ? null
                                : cursor.getInt(uidIdx),
                                ReadStatus.valueOf(cursor.getInt(seenIdx)), DeleteStatus
                                        .valueOf(cursor.getInt(deletedIdx)), PushStatus
                                        .valueOf(cursor.getInt(pushIdx)), MessageType
                                        .valueOf(cursor.getString(messageTypeIdx)), cursor
                                        .getString(messageIdIdx), cursor
                                        .isNull(nativeProviderIdIdx) ? null : cursor
                                        .getLong(nativeProviderIdIdx)));

            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Remove a message
     *
     * @param folderName
     * @param uid
     * @return int
     */
    public int removeMessage(String folderName, Integer uid) {
        return mLocalContentResolver.delete(MessageData.CONTENT_URI,
                ImapLog.Message.SELECTION_FOLDER_NAME_UID, new String[] {
                        folderName, String.valueOf(uid)
                });
    }
}
