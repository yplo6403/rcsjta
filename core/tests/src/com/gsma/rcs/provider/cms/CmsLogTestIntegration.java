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

package com.gsma.rcs.provider.cms;

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;

import java.util.HashMap;
import java.util.Map;

public class CmsLogTestIntegration {

    private static final String SORT_BY_UID_ASC = CmsData.KEY_UID + " ASC";

    protected final LocalContentResolver mLocalContentResolver;

    /**
     * Current instance
     */
    private static volatile CmsLogTestIntegration sInstance;

    private CmsLogTestIntegration(Context context) {
        mLocalContentResolver = new LocalContentResolver(context.getContentResolver());
    }

    public static CmsLogTestIntegration getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (CmsLogTestIntegration.class) {
            if (sInstance == null) {
                sInstance = new CmsLogTestIntegration(context);
            }
        }
        return sInstance;
    }

    /**
     * Get messages by folderName
     *
     * @param folderName the folder
     * @return CmsObject
     */
    public Map<Integer, CmsObject> getMessages(String folderName) {
        Cursor cursor = null;
        Map<Integer, CmsObject> messages = new HashMap<>();
        try {
            cursor = mLocalContentResolver.query(CmsData.CONTENT_URI, null,
                    CmsLog.Message.SEL_FOLDER_NAME, new String[] {
                        folderName
                    }, SORT_BY_UID_ASC);
            CursorUtil.assertCursorIsNotNull(cursor, CmsData.CONTENT_URI);
            int uidIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_UID);
            int folderIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_FOLDER);
            int seenIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_READ_STATUS);
            int delIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_DEL_STATUS);
            int pushIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_PUSH_STATUS);
            int msgTypeIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_MSG_TYPE);
            int msgIdIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_MSG_ID);
            int chatIdIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_CHAT_ID);
            int nativeIdIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_NATIVE_ID);
            while (cursor.moveToNext()) {
                Integer uid = cursor.isNull(uidIdx) ? null : cursor.getInt(uidIdx);
                String folder = cursor.getString(folderIdx);
                CmsData.ReadStatus readStatus = CmsData.ReadStatus.valueOf(cursor.getInt(seenIdx));
                DeleteStatus delStatus = DeleteStatus.valueOf(cursor.getInt(delIdx));
                PushStatus pushStatus = PushStatus.valueOf(cursor.getInt(pushIdx));
                MessageType msgType = MessageType.valueOf(cursor.getString(msgTypeIdx));
                String msgId = cursor.getString(msgIdIdx);
                String chatId = cursor.getString(chatIdIdx);
                Long nativeId = cursor.isNull(nativeIdIdx) ? null : cursor.getLong(nativeIdIdx);
                if (CmsObject.isXmsData(msgType)) {
                    messages.put(uid, new CmsXmsObject(msgType, folder, msgId, uid, pushStatus,
                            readStatus, delStatus, nativeId));
                } else {
                    messages.put(uid, new CmsRcsObject(msgType, folder, msgId, uid, pushStatus,
                            readStatus, delStatus, chatId));
                }
            }
            return messages;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get folder by name
     *
     * @param folderName the folder
     * @return CmsFolder
     */
    public CmsFolder getFolder(String folderName) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CmsFolder.CONTENT_URI, null,
                    CmsLog.Folder.SELECTION_NAME, new String[] {
                        folderName
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsFolder.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new CmsFolder(folderName, cursor.getInt(cursor
                    .getColumnIndexOrThrow(CmsFolder.KEY_NEXT_UID)), cursor.getInt(cursor
                    .getColumnIndexOrThrow(CmsFolder.KEY_HIGHESTMODSEQ)), cursor.getInt(cursor
                    .getColumnIndexOrThrow(CmsFolder.KEY_UID_VALIDITY)));
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get message by folderName and messageId
     *
     * @param folder the folder
     * @param messageId the message ID
     * @return CmsObject
     */
    public CmsObject getMessage(String folder, String messageId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CmsData.CONTENT_URI, null,
                    CmsLog.Message.SEL_FOLDER_MSGID, new String[] {
                            folder, messageId
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            int uidIxd = cursor.getColumnIndexOrThrow(CmsData.KEY_UID);
            Integer uid = cursor.isNull(uidIxd) ? null : cursor.getInt(uidIxd);
            int seenIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_READ_STATUS);
            CmsData.ReadStatus readStatus = CmsData.ReadStatus.valueOf(cursor.getInt(seenIdx));
            int delIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_DEL_STATUS);
            DeleteStatus delStatus = DeleteStatus.valueOf(cursor.getInt(delIdx));
            int pushIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_PUSH_STATUS);
            PushStatus pushStatus = PushStatus.valueOf(cursor.getInt(pushIdx));
            int msgTypeIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_MSG_TYPE);
            MessageType msgType = MessageType.valueOf(cursor.getString(msgTypeIdx));
            int msgIdIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_MSG_ID);
            String msgId = cursor.getString(msgIdIdx);
            int chatIdIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_CHAT_ID);
            String chatId = cursor.getString(chatIdIdx);
            int nativeIdIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_NATIVE_ID);
            Long nativeId = cursor.isNull(nativeIdIdx) ? null : cursor.getLong(nativeIdIdx);
            if (CmsObject.isXmsData(msgType)) {
                return new CmsXmsObject(msgType, folder, msgId, uid, pushStatus, readStatus,
                        delStatus, nativeId);
            } else {
                return new CmsRcsObject(msgType, folder, msgId, uid, pushStatus, readStatus,
                        delStatus, chatId);
            }
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get messages
     *
     * @return CmsObject
     */
    public Map<Integer, CmsObject> getMessages() {
        Cursor cursor = null;
        Map<Integer, CmsObject> messages = new HashMap<>();
        try {
            cursor = mLocalContentResolver.query(CmsData.CONTENT_URI, null, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsData.CONTENT_URI);
            int idIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            int uidIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_UID);
            int folderIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_FOLDER);
            int seenIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_READ_STATUS);
            int delIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_DEL_STATUS);
            int pushIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_PUSH_STATUS);
            int msgTypeIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_MSG_TYPE);
            int msgIdIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_MSG_ID);
            int chatIdIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_CHAT_ID);
            int nativeIdIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_NATIVE_ID);
            while (cursor.moveToNext()) {
                String folder = cursor.getString(folderIdx);
                Integer uid = cursor.isNull(uidIdx) ? null : cursor.getInt(uidIdx);
                CmsData.ReadStatus readStatus = CmsData.ReadStatus.valueOf(cursor.getInt(seenIdx));
                DeleteStatus delStatus = DeleteStatus.valueOf(cursor.getInt(delIdx));
                PushStatus pushStatus = PushStatus.valueOf(cursor.getInt(pushIdx));
                MessageType msgType = MessageType.valueOf(cursor.getString(msgTypeIdx));
                String msgId = cursor.getString(msgIdIdx);
                String chatId = cursor.getString(chatIdIdx);
                Long nativeId = cursor.isNull(nativeIdIdx) ? null : cursor.getLong(nativeIdIdx);
                if (CmsObject.isXmsData(msgType)) {
                    messages.put(cursor.getInt(idIdx), new CmsXmsObject(msgType, folder, msgId,
                            uid, pushStatus, readStatus, delStatus, nativeId));
                } else {
                    messages.put(cursor.getInt(idIdx), new CmsRcsObject(msgType, folder, msgId,
                            uid, pushStatus, readStatus, delStatus, chatId));
                }
            }
            return messages;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Remove a message
     *
     * @param folderName the folder
     * @param uid the message UID
     * @return int
     */
    public int removeMessage(String folderName, Integer uid) {
        return mLocalContentResolver.delete(CmsData.CONTENT_URI,
                CmsLog.Message.SEL_FOLDER_NAME_UID, new String[] {
                        folderName, String.valueOf(uid)
                });
    }

}
