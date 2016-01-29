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

import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;

import java.util.HashMap;
import java.util.Map;

public class CmsLogTestIntegration {

    private static final String SORT_BY_UID_ASC = new StringBuilder(CmsObject.KEY_UID).append(
            " ASC").toString();

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
     * @param folderName
     * @return CmsObject
     */
    public Map<Integer, CmsObject> getMessages(String folderName) {
        Cursor cursor = null;
        Map<Integer, CmsObject> messages = new HashMap<Integer, CmsObject>();
        try {
            cursor = mLocalContentResolver.query(CmsObject.CONTENT_URI, null,
                    CmsLog.Message.SELECTION_FOLDER_NAME, new String[] {
                        folderName
                    }, SORT_BY_UID_ASC);
            CursorUtil.assertCursorIsNotNull(cursor, CmsObject.CONTENT_URI);

            int uidIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_UID);
            int folderIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_FOLDER_NAME);
            int seenIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_READ_STATUS);
            int deletedIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_DELETE_STATUS);
            int pushIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_PUSH_STATUS);
            int messageTypeIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_MESSAGE_TYPE);
            int messageIdIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_MESSAGE_ID);
            int nativeProviderIdIdx = cursor
                    .getColumnIndexOrThrow(CmsObject.KEY_NATIVE_PROVIDER_ID);

            while (cursor.moveToNext()) {
                Integer uid = cursor.isNull(uidIdx) ? null : cursor.getInt(uidIdx);
                messages.put(
                        uid,
                        new CmsObject(cursor.getString(folderIdx), uid, CmsObject.ReadStatus
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
     * @param folderName
     * @param messageId
     * @return CmsObject
     */
    public CmsObject getMessage(String folderName, String messageId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CmsObject.CONTENT_URI, null,
                    CmsLog.Message.SELECTION_FOLDER_NAME_MESSAGEID, new String[] {
                            folderName, messageId
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsObject.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            int uidIxd = cursor.getColumnIndexOrThrow(CmsObject.KEY_UID);
            return new CmsObject(
                    folderName,
                    cursor.isNull(uidIxd) ? null : cursor.getInt(uidIxd),
                    ReadStatus.valueOf(cursor.getInt(cursor
                            .getColumnIndexOrThrow(CmsObject.KEY_READ_STATUS))),
                    DeleteStatus.valueOf(cursor.getInt(cursor
                            .getColumnIndexOrThrow(CmsObject.KEY_DELETE_STATUS))),
                    PushStatus.valueOf(cursor.getInt(cursor
                            .getColumnIndexOrThrow(CmsObject.KEY_PUSH_STATUS))),
                    MessageType.valueOf(cursor.getString(cursor
                            .getColumnIndexOrThrow(CmsObject.KEY_MESSAGE_TYPE))),
                    cursor.getString(cursor.getColumnIndexOrThrow(CmsObject.KEY_MESSAGE_ID)),
                    cursor.isNull(cursor.getColumnIndexOrThrow(CmsObject.KEY_NATIVE_PROVIDER_ID)) ? null
                            : cursor.getLong(cursor
                                    .getColumnIndexOrThrow(CmsObject.KEY_NATIVE_PROVIDER_ID)));
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
            cursor = mLocalContentResolver.query(CmsObject.CONTENT_URI, null, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsObject.CONTENT_URI);

            int idIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            int uidIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_UID);
            int folderIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_FOLDER_NAME);
            int seenIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_READ_STATUS);
            int deletedIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_DELETE_STATUS);
            int pushIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_PUSH_STATUS);
            int messageTypeIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_MESSAGE_TYPE);
            int messageIdIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_MESSAGE_ID);
            int nativeProviderIdIdx = cursor
                    .getColumnIndexOrThrow(CmsObject.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                messages.put(
                        cursor.getInt(idIdx),
                        new CmsObject(cursor.getString(folderIdx), cursor.isNull(uidIdx) ? null
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
        return mLocalContentResolver.delete(CmsObject.CONTENT_URI,
                CmsLog.Message.SELECTION_FOLDER_NAME_UID, new String[] {
                        folderName, String.valueOf(uid)
                });
    }
}
