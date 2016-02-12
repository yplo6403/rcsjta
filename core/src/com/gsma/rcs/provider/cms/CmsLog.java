/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.cms;

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.utils.logger.Logger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class to access the CMS provider which is a local copy of the CMS database
 */
public class CmsLog {

    /**
     * Current instance
     */
    private static volatile CmsLog sInstance;

    private final LocalContentResolver mLocalContentResolver;

    private static final String[] PROJECTION_ID = new String[] {
        BaseColumns._ID
    };
    private static final String[] PROJECTION_UID = new String[] {
        CmsObject.KEY_UID
    };

    // Folder table
    static final class Folder {
        static final String SELECTION_NAME = CmsFolder.KEY_NAME + "=?";
    }

    // Message table
    static final class Message {

        private static final String PROJECTION_MAX_UID = "MAX(" + CmsObject.KEY_UID + ")";

        private static final String[] PROJECTION_NATIVE_PROVIDERID_READ_DELETE = new String[] {
                CmsObject.KEY_NATIVE_PROVIDER_ID, CmsObject.KEY_READ_STATUS,
                CmsObject.KEY_DELETE_STATUS
        };

        static final String SELECTION_FOLDER_NAME = CmsObject.KEY_FOLDER_NAME + "=?";
        private static final String SELECTION_UID = CmsObject.KEY_UID + "=?";
        static final String SELECTION_FOLDER_NAME_UID = SELECTION_FOLDER_NAME + " AND "
                + SELECTION_UID;
        private static final String SELECTION_UID_NOT_NULL = CmsObject.KEY_UID + " IS NOT NULL";
        private static final String SELECTION_MESSAGE_TYPE = CmsObject.KEY_MESSAGE_TYPE + "=?";
        private static final String SELECTION_MESSAGE_ID = CmsObject.KEY_MESSAGE_ID + "=?";
        static final String SELECTION_FOLDER_NAME_MESSAGEID = SELECTION_FOLDER_NAME + " AND "
                + SELECTION_MESSAGE_ID;
        private static final String SELECTION_PROVIDER_ID = CmsObject.KEY_NATIVE_PROVIDER_ID + "=?";
        private static final String SELECTION_PROVIDER_ID_NULL = CmsObject.KEY_NATIVE_PROVIDER_ID
                + " is null";
        private static final String SELECTION_PROVIDER_ID_NOT_NULL = CmsObject.KEY_NATIVE_PROVIDER_ID
                + " is not null";
        private static final String SELECTION_READ_STATUS = CmsObject.KEY_READ_STATUS + "=?";
        private static final String SELECTION_DELETE_STATUS = CmsObject.KEY_DELETE_STATUS + "=?";
        private static final String SELECTION_READ_STATUS_OR_DELETE_STATUS = "("
                + SELECTION_READ_STATUS + " OR " + SELECTION_DELETE_STATUS + ")";
        private static final String SELECTION_PUSH_STATUS = CmsObject.KEY_PUSH_STATUS + "=?";
        private static final String SELECTION_CHAT = CmsObject.KEY_MESSAGE_TYPE + "='"
                + MessageType.CHAT_MESSAGE + "'";
        private static final String SELECTION_IMDN = CmsObject.KEY_MESSAGE_TYPE + "='"
                + MessageType.IMDN + "'";
        private static final String SELECTION_GROUP_STATE = CmsObject.KEY_MESSAGE_TYPE + "='"
                + MessageType.GROUP_STATE + "'";
        private static final String SELECTION_SMS = CmsObject.KEY_MESSAGE_TYPE + "='"
                + MessageType.SMS + "'";
        private static final String SELECTION_MMS = CmsObject.KEY_MESSAGE_TYPE + "='"
                + MessageType.MMS + "'";
        private static final String SELECTION_XMS = "(" + SELECTION_SMS + " OR " + SELECTION_MMS
                + ")";
        private static final String SELECTION_CHAT_IMDN = "(" + SELECTION_CHAT + " OR "
                + SELECTION_IMDN + ")";
        private static final String SELECTION_XMS_MESSAGEID = SELECTION_XMS + " AND "
                + SELECTION_MESSAGE_ID;
        private static final String SELECTION_CHAT_IMDN_MESSAGEID = SELECTION_CHAT_IMDN + " AND "
                + SELECTION_MESSAGE_ID;
        private static final String SELECTION_GROUP_STATE_MESSAGEID = SELECTION_GROUP_STATE
                + " AND " + SELECTION_MESSAGE_ID;
        private static final String SELECTION_SMS_MESSAGEID = SELECTION_SMS + " AND "
                + SELECTION_MESSAGE_ID;
        private static final String SELECTION_MMS_MESSAGEID = SELECTION_MMS + " AND "
                + SELECTION_MESSAGE_ID;

        private static final String SELECTION_XMS_READ_STATUS_UID_NOT_NULL = SELECTION_XMS
                + " AND " + SELECTION_READ_STATUS + " AND " + SELECTION_UID_NOT_NULL;
        private static final String SELECTION_CHAT_READ_STATUS_UID_NOT_NULL = SELECTION_CHAT
                + " AND " + SELECTION_READ_STATUS + " AND " + SELECTION_UID_NOT_NULL;
        private static final String SELECTION_READ_STATUS_UID_NOT_NULL = SELECTION_READ_STATUS
                + " AND " + SELECTION_UID_NOT_NULL;

        private static final String SELECTION_XMS_DELETE_STATUS_UID_NOT_NULL = SELECTION_XMS
                + " AND " + SELECTION_DELETE_STATUS + " AND " + SELECTION_UID_NOT_NULL;
        private static final String SELECTION_CHAT_DELETE_STATUS_UID_NOT_NULL = SELECTION_CHAT
                + " AND " + SELECTION_DELETE_STATUS + " AND " + SELECTION_UID_NOT_NULL;
        private static final String SELECTION_DELETE_STATUS_UID_NOT_NULL = SELECTION_DELETE_STATUS
                + " AND " + SELECTION_UID_NOT_NULL;

        private static final String SELECTION_XMS_PUSH_STATUS_DELETE_STATUS = SELECTION_XMS
                + " AND " + SELECTION_PUSH_STATUS + " AND " + SELECTION_DELETE_STATUS;
        private static final String SELECTION_FOLDER_XMS_PUSH_STATUS_DELETE_STATUS = SELECTION_FOLDER_NAME
                + " AND " + SELECTION_XMS_PUSH_STATUS_DELETE_STATUS;

        private static final String SELECTION_MESSAGE_TYPE_MESSAGE_ID = SELECTION_MESSAGE_TYPE
                + " AND " + SELECTION_MESSAGE_ID;
        private static final String SELECTION_MESSAGE_TYPE_PROVIDER_ID = SELECTION_MESSAGE_TYPE
                + " AND " + SELECTION_PROVIDER_ID;
        private static final String SELECTION_FOLDER_NAME_READ_STATUS_DELETE_STATUS = SELECTION_FOLDER_NAME
                + " AND " + SELECTION_READ_STATUS_OR_DELETE_STATUS;

        private static final String SELECTION_DELETE_STATUS_PROVIDER_ID_NULL = SELECTION_DELETE_STATUS
                + " AND " + SELECTION_PROVIDER_ID_NULL;
        private static final String SELECTION_MESSAGE_TYPE_PROVIDER_ID_NOT_NULL = SELECTION_MESSAGE_TYPE
                + " AND " + SELECTION_PROVIDER_ID_NOT_NULL;
    }

    static final int INVALID_ID = -1;

    private static final Logger sLogger = Logger.getLogger(CmsLog.class.getSimpleName());

    /**
     * Gets the instance of SecurityLog singleton
     *
     * @param context The context
     * @return the instance of SecurityLog singleton
     */
    public static CmsLog getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (CmsLog.class) {
            if (sInstance == null) {
                sInstance = new CmsLog(context);
            }
        }
        return sInstance;
    }

    public static CmsLog getInstance() {
        return sInstance;
    }

    /**
     * Constructor
     *
     * @param ctx The context
     */
    private CmsLog(Context ctx) {
        mLocalContentResolver = new LocalContentResolver(ctx.getContentResolver());
    }

    /**
     * Adds mailboxData
     *
     * @param folder The folder
     */
    public void addFolder(CmsFolder folder) {
        boolean logActivated = sLogger.isActivated();

        ContentValues values = new ContentValues();
        String name = folder.getName();
        values.put(CmsFolder.KEY_NEXT_UID, folder.getNextUid());
        values.put(CmsFolder.KEY_HIGHESTMODSEQ, folder.getModseq());
        values.put(CmsFolder.KEY_UID_VALIDITY, folder.getUidValidity());
        values.put(CmsFolder.KEY_NAME, name);

        Integer id = getFolderId(name);
        if (INVALID_ID == id) {
            if (logActivated) {
                sLogger.debug("Add mailboxData : ".concat(folder.toString()));
            }
            mLocalContentResolver.insert(CmsFolder.CONTENT_URI, values);
            return;
        }
        if (logActivated) {
            sLogger.debug("Update  mailboxData :".concat(folder.toString()));
        }
        Uri uri = Uri.withAppendedPath(CmsFolder.CONTENT_URI, id.toString());
        mLocalContentResolver.update(uri, values, null, null);
    }

    /**
     * Gets folders data
     *
     * @return CmsFolder
     */
    public Map<String, CmsFolder> getFolders() {
        Cursor cursor = null;
        Map<String, CmsFolder> folders = new HashMap<>();
        try {
            cursor = mLocalContentResolver.query(CmsFolder.CONTENT_URI, null, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsFolder.CONTENT_URI);

            int nameColumnIdx = cursor.getColumnIndexOrThrow(CmsFolder.KEY_NAME);
            int nextUidColumnIdx = cursor.getColumnIndexOrThrow(CmsFolder.KEY_NEXT_UID);
            int highestmodseqColumnIdx = cursor.getColumnIndexOrThrow(CmsFolder.KEY_HIGHESTMODSEQ);
            int uidvalidityColumnIdx = cursor.getColumnIndexOrThrow(CmsFolder.KEY_UID_VALIDITY);
            while (cursor.moveToNext()) {
                String name = cursor.getString(nameColumnIdx);
                folders.put(
                        name,
                        new CmsFolder(name, cursor.getInt(nextUidColumnIdx), cursor
                                .getInt(highestmodseqColumnIdx), cursor
                                .getInt(uidvalidityColumnIdx)));
            }
            return folders;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get folderId for a folderName
     *
     * @param name the folder name
     * @return id the folder ID
     */
    public Integer getFolderId(String name) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CmsFolder.CONTENT_URI, PROJECTION_ID,
                    Folder.SELECTION_NAME, new String[] {
                        name
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsFolder.CONTENT_URI);
            if (cursor.moveToFirst()) {
                int idColumnIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
                return cursor.getInt(idColumnIdx);
            }
            return INVALID_ID;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Deletes a folder
     *
     * @param name the folder name
     * @param deleteMessages True if messages must be deleted
     * @return id
     */
    public int removeFolder(String name, boolean deleteMessages) {
        if (deleteMessages) {
            removeMessages(name);
        }
        return mLocalContentResolver.delete(CmsFolder.CONTENT_URI, Folder.SELECTION_NAME,
                new String[] {
                    name
                });
    }

    /**
     * Removes all folders
     *
     * @param deleteMessages True if messages must be deleted
     * @return int
     */
    public int removeFolders(boolean deleteMessages) {
        if (deleteMessages) {
            removeMessages();
        }
        return mLocalContentResolver.delete(CmsFolder.CONTENT_URI, null, null);
    }

    /**
     * Adds mailboxData
     *
     * @param message The message to add
     */
    public void addMessage(CmsObject message) {
        boolean logActivated = sLogger.isActivated();

        ContentValues values = new ContentValues();
        Integer uid = message.getUid();
        values.put(CmsObject.KEY_UID, uid);
        values.put(CmsObject.KEY_FOLDER_NAME, message.getFolder());
        values.put(CmsObject.KEY_MESSAGE_ID, message.getMessageId());
        values.put(CmsObject.KEY_READ_STATUS, message.getReadStatus().toInt());
        values.put(CmsObject.KEY_MESSAGE_TYPE, message.getMessageType().name());
        values.put(CmsObject.KEY_DELETE_STATUS, message.getDeleteStatus().toInt());
        values.put(CmsObject.KEY_PUSH_STATUS, message.getPushStatus().toInt());
        values.put(CmsObject.KEY_NATIVE_PROVIDER_ID, message.getNativeProviderId());

        Integer id = INVALID_ID;
        if (message.getFolder() != null && uid != null) {
            id = getMessageId(message.getFolder(), uid);
        }
        if (INVALID_ID == id) {
            if (logActivated) {
                sLogger.debug("Add messageData : ".concat(message.toString()));
            }
            mLocalContentResolver.insert(CmsObject.CONTENT_URI, values);
            return;
        }
        if (logActivated) {
            sLogger.debug("Update  messageData :".concat(message.toString()));
        }
        Uri uri = Uri.withAppendedPath(CmsObject.CONTENT_URI, id.toString());
        mLocalContentResolver.update(uri, values, null, null);
    }

    /**
     * Updates message
     * 
     * @param messageType the type
     * @param messageId the ID
     * @param folderName the folder name
     * @param uid the UID
     * @param seen the seen flag
     * @param deleted the deleted flag
     */
    public void updateMessage(MessageType messageType, String messageId, String folderName,
            Integer uid, Boolean seen, Boolean deleted) {
        boolean logActivated = sLogger.isActivated();

        ContentValues values = new ContentValues();
        values.put(CmsObject.KEY_UID, uid);
        values.put(CmsObject.KEY_FOLDER_NAME, folderName);
        if (seen) {
            values.put(CmsObject.KEY_READ_STATUS, ReadStatus.READ.toInt());
        }
        if (deleted) {
            values.put(CmsObject.KEY_DELETE_STATUS, DeleteStatus.DELETED.toInt());
        }
        if (logActivated) {
            sLogger.debug("Update messageData : " + messageType + "," + messageId + ","
                    + folderName + "," + uid);
        }
        mLocalContentResolver.update(CmsObject.CONTENT_URI, values,
                Message.SELECTION_MESSAGE_TYPE_MESSAGE_ID, new String[] {
                        messageType.toString(), messageId
                });
    }

    /**
     * Get message by folderName and uid
     *
     * @param folderName the folder name
     * @param uid the UID
     * @return CmsObject
     */
    public CmsObject getMessage(String folderName, Integer uid) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CmsObject.CONTENT_URI, null,
                    Message.SELECTION_FOLDER_NAME_UID, new String[] {
                            folderName, String.valueOf(uid)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsObject.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new CmsObject(
                    folderName,
                    uid,
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
     * Get sms data by messageId
     *
     * @param messageId the message ID
     * @return CmsObject
     */
    public CmsObject getSmsData(String messageId) {
        return getData(messageId, Message.SELECTION_SMS_MESSAGEID);
    }

    /**
     * Get mms data by messageId
     *
     * @param messageId the message ID
     * @return CmsObject
     */
    public CmsObject getMmsData(String messageId) {
        return getData(messageId, Message.SELECTION_MMS_MESSAGEID);
    }

    /**
     * Get chat or imdn data by messageId
     *
     * @param messageId the ID
     * @return CmsObject
     */
    public CmsObject getChatOrImdnData(String messageId) {
        return getData(messageId, Message.SELECTION_CHAT_IMDN_MESSAGEID);
    }

    /**
     * Get groupChatObject data by messageId
     *
     * @param messageId the message ID
     * @return CmsObject
     */
    public CmsObject getGroupChatObjectData(String messageId) {
        return getData(messageId, Message.SELECTION_GROUP_STATE_MESSAGEID);
    }

    private CmsObject getData(String messageId, String selection) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CmsObject.CONTENT_URI, null, selection,
                    new String[] {
                        messageId
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsObject.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new CmsObject(
                    cursor.getString(cursor.getColumnIndexOrThrow(CmsObject.KEY_FOLDER_NAME)),
                    cursor.isNull(cursor.getColumnIndexOrThrow(CmsObject.KEY_UID)) ? null : cursor
                            .getInt(cursor.getColumnIndexOrThrow(CmsObject.KEY_UID)),
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
     * Return the max uid for messages of a folder
     *
     * @param folderName the folder name
     * @return maxUid or null
     */
    public Integer getMaxUidForMessages(String folderName) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CmsObject.CONTENT_URI, new String[] {
                Message.PROJECTION_MAX_UID
            }, Message.SELECTION_FOLDER_NAME, new String[] {
                folderName
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsObject.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            return cursor.getInt(cursor.getColumnIndexOrThrow(Message.PROJECTION_MAX_UID));
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get messages by folderName
     *
     * @param folderName the folder name
     * @return CmsObject
     */
    public Set<CmsObject> getMessages(String folderName, ReadStatus readStatus,
            DeleteStatus deleteStatus) {
        Cursor cursor = null;
        Set<CmsObject> messages = new HashSet<>();
        try {
            cursor = mLocalContentResolver.query(
                    CmsObject.CONTENT_URI,
                    null,
                    Message.SELECTION_FOLDER_NAME_READ_STATUS_DELETE_STATUS,
                    new String[] {
                            folderName, String.valueOf(readStatus.toInt()),
                            String.valueOf(deleteStatus.toInt()),
                    }, null);
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
                messages.add(new CmsObject(cursor.getString(folderIdx),
                        cursor.isNull(uidIdx) ? null : cursor.getInt(uidIdx), ReadStatus
                                .valueOf(cursor.getInt(seenIdx)), DeleteStatus.valueOf(cursor
                                .getInt(deletedIdx)), PushStatus.valueOf(cursor.getInt(pushIdx)),
                        MessageType.valueOf(cursor.getString(messageTypeIdx)), cursor
                                .getString(messageIdIdx), cursor.isNull(nativeProviderIdIdx) ? null
                                : cursor.getLong(nativeProviderIdIdx)));
            }
            return messages;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Gets messages by readStatus
     *
     * @param readStatus the read status
     * @return CmsObject
     */
    public Set<CmsObject> getMessages(ReadStatus readStatus) {
        return getMessages(Message.SELECTION_READ_STATUS_UID_NOT_NULL, new String[] {
            String.valueOf(readStatus.toInt())
        });
    }

    /**
     * Gets xms messages by readStatus
     *
     * @param readStatus the read status
     * @return the set of CmsObjects
     */
    public Set<CmsObject> getXmsMessages(ReadStatus readStatus) {
        return getMessages(Message.SELECTION_XMS_READ_STATUS_UID_NOT_NULL, new String[] {
            String.valueOf(readStatus.toInt())
        });
    }

    /**
     * Gets chat messages by readStatus
     *
     * @param readStatus the read status
     * @return CmsObject
     */
    public Set<CmsObject> getChatMessages(ReadStatus readStatus) {
        return getMessages(Message.SELECTION_CHAT_READ_STATUS_UID_NOT_NULL, new String[] {
            String.valueOf(readStatus.toInt())
        });
    }

    /**
     * Gets messages by readStatus
     *
     * @param selection the selection
     * @return CmsObject
     */
    Set<CmsObject> getMessages(String selection, String[] params) {
        Cursor cursor = null;
        Set<CmsObject> messages = new HashSet<>();
        try {
            cursor = mLocalContentResolver.query(CmsObject.CONTENT_URI, null, selection, params,
                    null);
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
                messages.add(new CmsObject(cursor.getString(folderIdx),
                        cursor.isNull(uidIdx) ? null : cursor.getInt(uidIdx), ReadStatus
                                .valueOf(cursor.getInt(seenIdx)), DeleteStatus.valueOf(cursor
                                .getInt(deletedIdx)), PushStatus.valueOf(cursor.getInt(pushIdx)),
                        MessageType.valueOf(cursor.getString(messageTypeIdx)), cursor
                                .getString(messageIdIdx), cursor.isNull(nativeProviderIdIdx) ? null
                                : cursor.getLong(nativeProviderIdIdx)));

            }
            return messages;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Gets messages by deleteStatus
     *
     * @param deleteStatus the deleted status
     * @return the set of CmsObject instances
     */
    public Set<CmsObject> getMessages(DeleteStatus deleteStatus) {
        return getMessages(Message.SELECTION_DELETE_STATUS_UID_NOT_NULL, new String[] {
            String.valueOf(deleteStatus.toInt())
        });
    }

    /**
     * Gets xms messages by deleteStatus
     *
     * @param deleteStatus the deleted status
     * @return the set of CmsObjects
     */
    public Set<CmsObject> getXmsMessages(DeleteStatus deleteStatus) {
        return getMessages(Message.SELECTION_XMS_DELETE_STATUS_UID_NOT_NULL, new String[] {
            String.valueOf(deleteStatus.toInt())
        });
    }

    /**
     * Gets xms messages by deleteStatus
     *
     * @param deleteStatus the deleted status
     * @return the set of CmsObjects
     */
    public Set<CmsObject> getChatMessages(DeleteStatus deleteStatus) {
        return getMessages(Message.SELECTION_CHAT_DELETE_STATUS_UID_NOT_NULL, new String[] {
            String.valueOf(deleteStatus.toInt())
        });
    }

    /**
     * Gets messages by folder name and pushStatus Filter out messages marked as deleted
     *
     * @param folderName the folder name
     * @param pushStatus the push status
     * @return the set of CmsObjects
     */
    public Set<CmsObject> getXmsMessages(String folderName, PushStatus pushStatus) {
        Cursor cursor = null;
        Set<CmsObject> messages = new HashSet<>();
        try {
            cursor = mLocalContentResolver.query(
                    CmsObject.CONTENT_URI,
                    null,
                    Message.SELECTION_FOLDER_XMS_PUSH_STATUS_DELETE_STATUS,
                    new String[] {
                            folderName, String.valueOf(pushStatus.toInt()),
                            String.valueOf(DeleteStatus.NOT_DELETED.toInt())
                    }, null);
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
                messages.add(new CmsObject(cursor.getString(folderIdx),
                        cursor.isNull(uidIdx) ? null : cursor.getInt(uidIdx), ReadStatus
                                .valueOf(cursor.getInt(seenIdx)), DeleteStatus.valueOf(cursor
                                .getInt(deletedIdx)), PushStatus.valueOf(cursor.getInt(pushIdx)),
                        MessageType.valueOf(cursor.getString(messageTypeIdx)), cursor
                                .getString(messageIdIdx), cursor.isNull(nativeProviderIdIdx) ? null
                                : cursor.getLong(nativeProviderIdIdx)));

            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Gets messages by pushStatus Filter out messages marked as deleted
     * 
     * @param pushStatus the push status
     * @return the set CmsObjects
     */
    public Set<CmsObject> getXmsMessages(PushStatus pushStatus) {
        Cursor cursor = null;
        Set<CmsObject> messages = new HashSet<>();
        try {
            cursor = mLocalContentResolver.query(
                    CmsObject.CONTENT_URI,
                    null,
                    Message.SELECTION_XMS_PUSH_STATUS_DELETE_STATUS,
                    new String[] {
                            String.valueOf(pushStatus.toInt()),
                            String.valueOf(DeleteStatus.NOT_DELETED.toInt())
                    }, null);
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
                messages.add(new CmsObject(cursor.getString(folderIdx),
                        cursor.isNull(uidIdx) ? null : cursor.getInt(uidIdx), ReadStatus
                                .valueOf(cursor.getInt(seenIdx)), DeleteStatus.valueOf(cursor
                                .getInt(deletedIdx)), PushStatus.valueOf(cursor.getInt(pushIdx)),
                        MessageType.valueOf(cursor.getString(messageTypeIdx)), cursor
                                .getString(messageIdIdx), cursor.isNull(nativeProviderIdIdx) ? null
                                : cursor.getLong(nativeProviderIdIdx)));

            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Gets messageId for a folder name and uid message
     *
     * @param folderName the folder name
     * @param uid the UID
     * @return id the message ID
     */
    public Integer getMessageId(String folderName, Integer uid) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CmsObject.CONTENT_URI, PROJECTION_ID,
                    Message.SELECTION_FOLDER_NAME_UID, new String[] {
                            folderName, String.valueOf(uid)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsObject.CONTENT_URI);
            if (cursor.moveToFirst()) {
                int idColumnIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
                return cursor.getInt(idColumnIdx);
            }
            return INVALID_ID;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Gets uid by messageId
     * 
     * @param baseId the base ID
     * @return uid
     */
    public Integer getUidForXmsMessage(String baseId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CmsObject.CONTENT_URI, PROJECTION_UID,
                    Message.SELECTION_XMS_MESSAGEID, new String[] {
                        baseId
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsObject.CONTENT_URI);
            int uidIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_UID);
            if (cursor.moveToFirst()) {
                return cursor.isNull(uidIdx) ? null : cursor.getInt(uidIdx);
            }
            return null;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Updates delete status of all messages
     * 
     * @param deleteStatus the deleted status
     */
    public void updateDeleteStatus(DeleteStatus deleteStatus) {
        ContentValues values = new ContentValues();
        values.put(CmsObject.KEY_DELETE_STATUS, deleteStatus.toInt());
        mLocalContentResolver.update(CmsObject.CONTENT_URI, values, null, null);
    }

    /**
     * Updates delete status by folder and uid
     * 
     * @param folder the folder name
     * @param uid the UID
     * @param deleteStatus the deleted status
     */
    public void updateDeleteStatus(String folder, Integer uid, DeleteStatus deleteStatus) {
        ContentValues values = new ContentValues();
        values.put(CmsObject.KEY_DELETE_STATUS, deleteStatus.toInt());
        mLocalContentResolver.update(CmsObject.CONTENT_URI, values,
                Message.SELECTION_FOLDER_NAME_UID, new String[] {
                        folder, String.valueOf(uid)
                });
    }

    /**
     * Updates delete status by messageType and messageId
     * 
     * @param messageType the type
     * @param messageId the message ID
     * @param deleteStatus the deleted status
     */
    public void updateDeleteStatus(MessageType messageType, String messageId,
            DeleteStatus deleteStatus) {
        ContentValues values = new ContentValues();
        values.put(CmsObject.KEY_DELETE_STATUS, deleteStatus.toInt());
        mLocalContentResolver.update(CmsObject.CONTENT_URI, values,
                Message.SELECTION_MESSAGE_TYPE_MESSAGE_ID, new String[] {
                        messageType.toString(), messageId
                });
    }

    /**
     * Updates read status by folder and uid
     * 
     * @param folder the folder name
     * @param uid the UID
     * @param readStatus the read status
     */
    public void updateReadStatus(String folder, Integer uid, ReadStatus readStatus) {
        ContentValues values = new ContentValues();
        values.put(CmsObject.KEY_READ_STATUS, readStatus.toInt());
        mLocalContentResolver.update(CmsObject.CONTENT_URI, values,
                Message.SELECTION_FOLDER_NAME_UID, new String[] {
                        folder, String.valueOf(uid)
                });
    }

    /**
     * Updates read status by messageType and messageId
     * 
     * @param messageType the type
     * @param messageId the message ID
     * @param readStatus the read status
     */
    public void updateReadStatus(MessageType messageType, String messageId, ReadStatus readStatus) {
        ContentValues values = new ContentValues();
        values.put(CmsObject.KEY_READ_STATUS, readStatus.toInt());
        mLocalContentResolver.update(CmsObject.CONTENT_URI, values,
                Message.SELECTION_MESSAGE_TYPE_MESSAGE_ID, new String[] {
                        messageType.toString(), messageId
                });
    }

    /**
     * Updates push status and uid by messageId
     * 
     * @param uid the UID
     * @param messageId the message ID
     * @param pushStatus the push status
     */
    public void updateXmsPushStatus(Integer uid, String messageId, PushStatus pushStatus) {
        ContentValues values = new ContentValues();
        values.put(CmsObject.KEY_UID, uid);
        values.put(CmsObject.KEY_PUSH_STATUS, pushStatus.toInt());
        mLocalContentResolver.update(CmsObject.CONTENT_URI, values,
                Message.SELECTION_XMS_MESSAGEID, new String[] {
                    messageId
                });
    }

    /**
     * Updates push status by messageType
     * 
     * @param messageType the type
     * @param pushStatus the push status
     */
    public void updatePushStatus(MessageType messageType, PushStatus pushStatus) {
        ContentValues values = new ContentValues();
        values.put(CmsObject.KEY_PUSH_STATUS, pushStatus.toInt());
        mLocalContentResolver.update(CmsObject.CONTENT_URI, values, Message.SELECTION_MESSAGE_TYPE,
                new String[] {
                    messageType.toString()
                });
    }

    /**
     * Removes messages from a folder
     *
     * @param folderName the folder name
     * @return The number of rows affected.
     */
    public int removeMessages(String folderName) {
        return mLocalContentResolver.delete(CmsObject.CONTENT_URI, Message.SELECTION_FOLDER_NAME,
                new String[] {
                    folderName
                });
    }

    /**
     * Updates delete status by messageType and nativeProviderId
     * 
     * @param messageType the type
     * @param nativeProviderId the native provider ID
     * @param deleteStatus the deleted status
     */
    public void updateDeleteStatus(MessageType messageType, Long nativeProviderId,
            DeleteStatus deleteStatus) {
        ContentValues values = new ContentValues();
        values.put(CmsObject.KEY_DELETE_STATUS, deleteStatus.toInt());
        values.put(CmsObject.KEY_NATIVE_PROVIDER_ID, (String) null);
        mLocalContentResolver.update(CmsObject.CONTENT_URI, values,
                Message.SELECTION_MESSAGE_TYPE_PROVIDER_ID, new String[] {
                        messageType.toString(), String.valueOf(nativeProviderId)
                });
    }

    /**
     * Updates read status by messageType and nativeProviderId
     * 
     * @param messageType the type
     * @param nativeProviderId the native provider ID
     * @param readStatus the read status
     */
    public void updateReadStatus(MessageType messageType, Long nativeProviderId,
            ReadStatus readStatus) {
        ContentValues values = new ContentValues();
        values.put(CmsObject.KEY_READ_STATUS, readStatus.toInt());
        mLocalContentResolver.update(CmsObject.CONTENT_URI, values,
                Message.SELECTION_MESSAGE_TYPE_PROVIDER_ID, new String[] {
                        messageType.toString(), String.valueOf(nativeProviderId)
                });
    }

    /**
     * Purges messages
     * 
     * @return The number of rows affected.
     */
    public int purgeMessages() {
        return mLocalContentResolver.delete(CmsObject.CONTENT_URI,
                Message.SELECTION_DELETE_STATUS_PROVIDER_ID_NULL, new String[] {
                    String.valueOf(DeleteStatus.DELETED.toInt())
                });
    }

    /**
     * Removes all messages
     *
     * @return The number of rows affected.
     */
    public int removeMessages() {
        return mLocalContentResolver.delete(CmsObject.CONTENT_URI, null, null);
    }

    public Map<Long, CmsObject> getNativeMessages(MessageType messageType) {
        Cursor cursor = null;
        Map<Long, CmsObject> messages = new HashMap<>();
        try {
            cursor = mLocalContentResolver.query(CmsObject.CONTENT_URI,
                    Message.PROJECTION_NATIVE_PROVIDERID_READ_DELETE,
                    Message.SELECTION_MESSAGE_TYPE_PROVIDER_ID_NOT_NULL, new String[] {
                        messageType.toString()
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsObject.CONTENT_URI);
            int readIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_READ_STATUS);
            int deletedIdx = cursor.getColumnIndexOrThrow(CmsObject.KEY_DELETE_STATUS);
            int nativeProviderIdIdx = cursor
                    .getColumnIndexOrThrow(CmsObject.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(nativeProviderIdIdx);
                messages.put(id, new CmsObject(id, ReadStatus.valueOf(cursor.getInt(readIdx)),
                        DeleteStatus.valueOf(cursor.getInt(deletedIdx))));

            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

}
