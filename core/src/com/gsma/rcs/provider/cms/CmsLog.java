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

import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsData.ReadStatus;
import com.gsma.rcs.utils.DatabaseUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    // Folder table
    static final class Folder {
        static final String SELECTION_NAME = CmsFolder.KEY_NAME + "=?";
    }

    // Message table
    static final class Message {

        private static final String PROJ_MAX_UID = "MAX(" + CmsData.KEY_UID + ")";

        // @formatter:off

        static final String SEL_FOLDER_NAME = CmsData.KEY_FOLDER + "=?";
        static final String SEL_UID = CmsData.KEY_UID + "=?";
        static final String SEL_FOLDER_NAME_UID = SEL_FOLDER_NAME + " AND " + SEL_UID;
        private static final String SEL_TYPE = CmsData.KEY_MSG_TYPE + "=?";
        private static final String SEL_MSGID = CmsData.KEY_MSG_ID + "=?";
        static final String SEL_FOLDER_MSGID = SEL_FOLDER_NAME + " AND " + SEL_MSGID;

        private static final String SEL_PROVIDER_ID_NULL = CmsData.KEY_NATIVE_ID + " is null";
        private static final String SEL_PROVIDER_ID_NOT_NULL = CmsData.KEY_NATIVE_ID + " is not null";

        private static final String SEL_READ_STATUS = CmsData.KEY_READ_STATUS + "=?";
        private static final String SEL_DEL_STATUS = CmsData.KEY_DEL_STATUS + "=?";
        private static final String SEL_READ_STATUS_OR_DELETE_STATUS = "(" + SEL_READ_STATUS + " OR " + SEL_DEL_STATUS + ")";

        private static final String SEL_CHAT = CmsData.KEY_MSG_TYPE + "='" + MessageType.CHAT_MESSAGE + "'";
        private static final String SEL_IMDN = CmsData.KEY_MSG_TYPE + "='" + MessageType.IMDN + "'";
        private static final String SEL_FT = CmsData.KEY_MSG_TYPE + "='" + MessageType.FILE_TRANSFER + "'";
        private static final String SEL_GC_STATE = CmsData.KEY_MSG_TYPE + "='" + MessageType.GROUP_STATE + "'";
        private static final String SEL_CPM_SESSION = CmsData.KEY_MSG_TYPE + "='" + MessageType.CPM_SESSION + "'";
        private static final String SEL_SMS = CmsData.KEY_MSG_TYPE + "='" + MessageType.SMS + "'";
        private static final String SEL_MMS = CmsData.KEY_MSG_TYPE + "='" + MessageType.MMS + "'";
        private static final String SEL_XMS = "(" + SEL_SMS + " OR " + SEL_MMS + ")";

        private static final String SEL_CHAT_IMDN_FT = "(" + SEL_CHAT + " OR " + SEL_IMDN + " OR " + SEL_FT + ")";
        private static final String SEL_XMS_MSGID_FOLDER = SEL_XMS + " AND " + SEL_MSGID + " AND " + SEL_FOLDER_NAME;
        private static final String SEL_CHAT_MSGID = SEL_CHAT + " AND " + SEL_MSGID;
        private static final String SEL_CHAT_IMDN_FT_MSGID = SEL_CHAT_IMDN_FT + " AND " + SEL_MSGID;
        private static final String SEL_GC_STATE_MSGID = SEL_GC_STATE + " AND " + SEL_MSGID;
        private static final String SEL_CPM_SESSION_MSGID = SEL_CPM_SESSION + " AND " + SEL_MSGID;
        private static final String SEL_SMS_MSGID_FOLDER = SEL_SMS + " AND " + SEL_MSGID + " AND " + SEL_FOLDER_NAME;
        private static final String SEL_MMS_MSGID = SEL_MMS + " AND " + SEL_MSGID;
        private static final String SEL_MMS_MSGID_FOLDER = SEL_MMS_MSGID + " AND " + SEL_FOLDER_NAME;

        private static final String SEL_TYPE_MSGID = SEL_TYPE + " AND " + SEL_MSGID;

        private static final String SEL_CHATID = CmsData.KEY_CHAT_ID + "=?";
        private static final String SEL_TYPE_CHATID = SEL_TYPE + " AND " + SEL_CHATID;

        private static final String SEL_MESSAGES_TO_SYNC = SEL_FOLDER_NAME + " AND " + SEL_READ_STATUS_OR_DELETE_STATUS;

        private static final String SEL_DELETED = CmsData.KEY_DEL_STATUS + "=" + DeleteStatus.DELETED.toInt();
        private static final String SEL_DELETED_PROVIDER_ID_NULL = SEL_DELETED + " AND " + SEL_PROVIDER_ID_NULL;

        private static final String SEL_TYPE_PROVIDER_ID_NOT_NULL = SEL_TYPE + " AND " + SEL_PROVIDER_ID_NOT_NULL;

        private static final String SEL_XMS_TO_PUSH = SEL_XMS + " AND (" +
                CmsData.KEY_DEL_STATUS + "=" + DeleteStatus.DELETED_REPORT_REQUESTED.toInt() + " OR " +
                CmsData.KEY_READ_STATUS + "=" + ReadStatus.READ_REPORT_REQUESTED.toInt() + " OR " +
                CmsData.KEY_PUSH_STATUS + "=" + PushStatus.PUSH_REQUESTED.toInt() + ")";

        private static final String SEL_DEL_REPORTED = CmsData.KEY_DEL_STATUS + "=" + DeleteStatus.DELETED_REPORTED.toInt();
        private static final String SEL_READ_REPORTED = CmsData.KEY_READ_STATUS + "=" + ReadStatus.READ_REPORTED.toInt();

        private static final String SEL_READ_REPORTED_MSGID = SEL_READ_REPORTED + " AND " + SEL_MSGID;
        private static final String SEL_DEL_REPORTED_MSGID = SEL_DEL_REPORTED + " AND " + SEL_MSGID;

        // @formatter:on
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

    /**
     * Constructor
     *
     * @param ctx The context
     */
    private CmsLog(Context ctx) {
        mLocalContentResolver = new LocalContentResolver(ctx.getContentResolver());
    }

    /**
     * Adds folder
     *
     * @param folder The folder
     */
    public void addFolder(CmsFolder folder) {
        ContentValues values = new ContentValues();
        String name = folder.getName();
        values.put(CmsFolder.KEY_NAME, name);
        values.put(CmsFolder.KEY_NEXT_UID, folder.getNextUid());
        values.put(CmsFolder.KEY_HIGHESTMODSEQ, folder.getModseq());
        values.put(CmsFolder.KEY_UID_VALIDITY, folder.getUidValidity());
        if (sLogger.isActivated()) {
            sLogger.debug("Add folder: ".concat(name));
        }
        mLocalContentResolver.insert(CmsFolder.CONTENT_URI, values);
    }

    /**
     * Updates folder
     *
     * @param folder The folder
     * @return the number of rows affected.
     */
    public int updateFolder(CmsFolder folder) {
        ContentValues values = new ContentValues();
        String name = folder.getName();
        values.put(CmsFolder.KEY_NEXT_UID, folder.getNextUid());
        values.put(CmsFolder.KEY_HIGHESTMODSEQ, folder.getModseq());
        values.put(CmsFolder.KEY_UID_VALIDITY, folder.getUidValidity());
        if (sLogger.isActivated()) {
            sLogger.debug("Update folder: ".concat(name));
        }
        Uri uri = Uri.withAppendedPath(CmsFolder.CONTENT_URI, name);
        return mLocalContentResolver.update(uri, values, null, null);
    }

    /**
     * Checks if folder is persisted
     *
     * @param name the folder name
     * @return true if folder is persisted
     */
    public boolean isFolderPersisted(String name) {
        Cursor cursor = null;
        Uri uri = Uri.withAppendedPath(CmsFolder.CONTENT_URI, name);
        try {
            cursor = mLocalContentResolver.query(uri, null, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsFolder.CONTENT_URI);
            return cursor.moveToNext();

        } finally {
            CursorUtil.close(cursor);
        }
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
            int highestModSeqColumnIdx = cursor.getColumnIndexOrThrow(CmsFolder.KEY_HIGHESTMODSEQ);
            int uidValidityColumnIdx = cursor.getColumnIndexOrThrow(CmsFolder.KEY_UID_VALIDITY);
            while (cursor.moveToNext()) {
                String name = cursor.getString(nameColumnIdx);
                folders.put(
                        name,
                        new CmsFolder(name, cursor.getInt(nextUidColumnIdx), cursor
                                .getInt(highestModSeqColumnIdx), cursor
                                .getInt(uidValidityColumnIdx)));
            }
            return folders;

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
        Uri uri = Uri.withAppendedPath(CmsFolder.CONTENT_URI, name);
        return mLocalContentResolver.delete(uri, null, null);
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
     * Adds XMS message
     *
     * @param message The XMS message to add
     */
    public void addXmsMessage(CmsXmsObject message) {
        ContentValues values = new ContentValues();
        Long nativeId = message.getNativeProviderId();
        if (nativeId != null) {
            values.put(CmsData.KEY_NATIVE_ID, nativeId);
        }
        addMessage(message, values);
    }

    /**
     * Adds RCS message
     *
     * @param message The RCS message to add
     */
    public void addRcsMessage(CmsRcsObject message) {
        ContentValues values = new ContentValues();
        String chatId = message.getChatId();
        if (chatId != null) {
            values.put(CmsData.KEY_CHAT_ID, chatId);
        }
        addMessage(message, values);
    }

    private void addMessage(CmsObject message, ContentValues values) {
        values.put(CmsData.KEY_FOLDER, message.getFolder());
        values.put(CmsData.KEY_MSG_ID, message.getMessageId());
        values.put(CmsData.KEY_READ_STATUS, message.getReadStatus().toInt());
        values.put(CmsData.KEY_MSG_TYPE, message.getMessageType().name());
        values.put(CmsData.KEY_DEL_STATUS, message.getDeleteStatus().toInt());
        values.put(CmsData.KEY_PUSH_STATUS, message.getPushStatus().toInt());
        Integer uid = message.getUid();
        Integer id = INVALID_ID;
        if (uid != null) {
            values.put(CmsData.KEY_UID, uid);
            id = getMessageId(message.getFolder(), uid);
        }
        if (INVALID_ID == id) {
            if (sLogger.isActivated()) {
                sLogger.debug("Add ".concat(message.toString()));
            }
            mLocalContentResolver.insert(CmsData.CONTENT_URI, values);
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Update  messageData :".concat(message.toString()));
        }
        Uri uri = Uri.withAppendedPath(CmsData.CONTENT_URI, id.toString());
        mLocalContentResolver.update(uri, values, null, null);
    }

    /**
     * Updates message
     *
     * @param folder the folder name
     * @param messageId the ID
     * @param uid the UID
     */
    public void updateUid(String folder, String messageId, Integer uid) {
        boolean logActivated = sLogger.isActivated();
        ContentValues values = new ContentValues();
        values.put(CmsData.KEY_UID, uid);
        values.put(CmsData.KEY_PUSH_STATUS, PushStatus.PUSHED.toInt());
        if (logActivated) {
            sLogger.debug("updateUid ID=" + messageId + " folder=" + folder + " uid=" + uid);
        }
        mLocalContentResolver.update(CmsData.CONTENT_URI, values, Message.SEL_FOLDER_MSGID,
                new String[] {
                        folder, messageId
                });
    }

    /**
     * Updates message
     *
     * @param messageId the ID
     * @param folderName the folder name
     * @param uid the UID
     * @param seen the seen flag
     * @param deleted the deleted flag
     */
    public void updateMessage(String messageId, String folderName, Integer uid, Boolean seen,
            Boolean deleted) {
        ContentValues values = new ContentValues();
        values.put(CmsData.KEY_UID, uid);
        values.put(CmsData.KEY_PUSH_STATUS, PushStatus.PUSHED.toInt());
        if (seen) {
            values.put(CmsData.KEY_READ_STATUS, ReadStatus.READ.toInt());
        }
        if (deleted) {
            values.put(CmsData.KEY_DEL_STATUS, DeleteStatus.DELETED.toInt());
        }
        if (sLogger.isActivated()) {
            sLogger.debug("updateMessage ID=" + messageId + " folder=" + folderName + ", uid="
                    + uid + " seen=" + seen + " del=" + deleted);
        }
        mLocalContentResolver.update(CmsData.CONTENT_URI, values, Message.SEL_FOLDER_MSGID,
                new String[] {
                        folderName, messageId
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
        List<CmsObject> cmsObject = getCmsObjects(Message.SEL_FOLDER_NAME_UID, new String[] {
                folderName, String.valueOf(uid)
        }, null);
        if (cmsObject.isEmpty()) {
            return null;
        }
        return cmsObject.get(0);
    }

    /**
     * Get sms data by messageId
     *
     * @param contact the contact ID
     * @param messageId the message ID
     * @return CmsObject
     */
    public CmsXmsObject getSmsData(ContactId contact, String messageId) {
        String folder = CmsUtils.contactToCmsFolder(contact);
        return (CmsXmsObject) getData(messageId, Message.SEL_SMS_MSGID_FOLDER, new String[] {
            folder
        });
    }

    /**
     * Get mms data by messageId and contact
     *
     * @param contact the contact ID
     * @param messageId the message ID
     * @return CmsObject
     */
    public CmsXmsObject getMmsData(ContactId contact, String messageId) {
        String folder = CmsUtils.contactToCmsFolder(contact);
        return (CmsXmsObject) getData(messageId, Message.SEL_MMS_MSGID_FOLDER, new String[] {
            folder
        });
    }

    /**
     * Get chat or imdn data by messageId
     *
     * @param messageId the ID
     * @return CmsObject
     */
    public CmsRcsObject getChatOrImdnOrFileTransferData(String messageId) {
        return (CmsRcsObject) getData(messageId, Message.SEL_CHAT_IMDN_FT_MSGID, null);
    }

    /**
     * Get chat or imdn data by messageId
     *
     * @param messageId the ID
     * @return CmsObject
     */
    public CmsRcsObject getChatData(String messageId) {
        return (CmsRcsObject) getData(messageId, Message.SEL_CHAT_MSGID, null);
    }

    /**
     * Get groupChatObject data by messageId
     *
     * @param messageId the message ID
     * @return CmsObject
     */
    public CmsRcsObject getGroupChatObjectData(String messageId) {
        return (CmsRcsObject) getData(messageId, Message.SEL_GC_STATE_MSGID, null);
    }

    /**
     * Get cpmsession data by messageId
     *
     * @param messageId the message ID
     * @return CmsObject
     */
    public CmsRcsObject getCpmSessionData(String messageId) {
        return (CmsRcsObject) getData(messageId, Message.SEL_CPM_SESSION_MSGID, null);
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
            cursor = mLocalContentResolver.query(CmsData.CONTENT_URI, new String[] {
                Message.PROJ_MAX_UID
            }, Message.SEL_FOLDER_NAME, new String[] {
                folderName
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            return cursor.getInt(cursor.getColumnIndexOrThrow(Message.PROJ_MAX_UID));
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get list of messages to synchronize with CMS
     *
     * @param folderName the folder name
     * @return CmsObject
     */
    public List<CmsObject> getMessagesToSync(String folderName) {
        return getCmsObjects(Message.SEL_MESSAGES_TO_SYNC,
                new String[] {
                        folderName, String.valueOf(ReadStatus.READ_REPORT_REQUESTED.toInt()),
                        String.valueOf(DeleteStatus.DELETED_REPORT_REQUESTED.toInt())
                }, null);
    }

    /**
     * Gets messages to be pushed either to update the seen flag or to update the delete flag or to
     * create the message on the CMS server.
     *
     * @return the set CmsObjects
     */
    public Set<CmsXmsObject> getXmsMessagesToPush() {
        List<CmsObject> cmsObject = getCmsObjects(Message.SEL_XMS_TO_PUSH, null, null);
        Set<CmsXmsObject> messages = new HashSet<>();
        for (CmsObject item : cmsObject) {
            messages.add((CmsXmsObject) item);
        }
        return messages;
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
            cursor = mLocalContentResolver.query(CmsData.CONTENT_URI, PROJECTION_ID,
                    Message.SEL_FOLDER_NAME_UID, new String[] {
                            folderName, String.valueOf(uid)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, CmsData.CONTENT_URI);
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
     * Updates delete status of all messages
     *
     * @param deleteStatus the deleted status
     * @return the number of rows affected.
     */
    public int updateDeleteStatus(String folder, DeleteStatus deleteStatus) {
        ContentValues values = new ContentValues();
        values.put(CmsData.KEY_DEL_STATUS, deleteStatus.toInt());
        return mLocalContentResolver.update(CmsData.CONTENT_URI, values, Message.SEL_FOLDER_NAME,
                new String[] {
                    folder
                });
    }

    /**
     * Updates delete status by folder and uid
     *
     * @param folder the folder name
     * @param uid the UID
     * @param deleteStatus the deleted status
     * @return the number of rows affected.
     */
    public int updateDeleteStatus(String folder, Integer uid, DeleteStatus deleteStatus) {
        ContentValues values = new ContentValues();
        values.put(CmsData.KEY_DEL_STATUS, deleteStatus.toInt());
        return mLocalContentResolver.update(CmsData.CONTENT_URI, values,
                Message.SEL_FOLDER_NAME_UID, new String[] {
                        folder, String.valueOf(uid)
                });
    }

    /**
     * Updates delete status by messageType and messageId for RCS messages.
     *
     * @param messageType the type
     * @param messageId the message ID
     * @param deleteStatus the deleted status
     * @param uid the CMS uid
     * @return the number of rows affected.
     */
    public int updateRcsDeleteStatus(MessageType messageType, String messageId,
            DeleteStatus deleteStatus, Integer uid) {
        if (sLogger.isActivated()) {
            sLogger.debug("updateRcsDeleteStatus ID=" + messageId + " type=" + messageType
                    + " status=" + deleteStatus);
        }
        ContentValues values = new ContentValues();
        values.put(CmsData.KEY_DEL_STATUS, deleteStatus.toInt());
        if (uid != null) {
            values.put(CmsData.KEY_UID, uid);
        }
        if (MessageType.CPM_SESSION == messageType) {
            return mLocalContentResolver.update(CmsData.CONTENT_URI, values,
                    Message.SEL_TYPE_CHATID, new String[] {
                            messageType.toString(), messageId
                    });
        } else {
            return mLocalContentResolver.update(CmsData.CONTENT_URI, values,
                    Message.SEL_TYPE_MSGID, new String[] {
                            messageType.toString(), messageId
                    });
        }
    }

    /**
     * Updates delete status by messageId and contact for XMS messages
     *
     * @param contact the contact ID
     * @param messageId the message ID
     * @param deleteStatus the deleted status
     * @param uid the CMS uid
     * @return the number of rows affected.
     */
    public int updateXmsDeleteStatus(ContactId contact, String messageId,
            DeleteStatus deleteStatus, Integer uid) {
        if (sLogger.isActivated()) {
            sLogger.debug("updateXmsDeleteStatus ID=" + messageId + " contact=" + contact
                    + " status=" + deleteStatus);
        }
        ContentValues values = new ContentValues();
        values.put(CmsData.KEY_DEL_STATUS, deleteStatus.toInt());
        if (uid != null) {
            values.put(CmsData.KEY_UID, uid);
        }
        return mLocalContentResolver.update(CmsData.CONTENT_URI, values,
                Message.SEL_XMS_MSGID_FOLDER, new String[] {
                        messageId, CmsUtils.contactToCmsFolder(contact)
                });
    }

    /**
     * Updates read status by folder and uid
     *
     * @param folder the folder name
     * @param uid the CMS uid
     * @param readStatus the read status
     * @return the number of rows affected.
     */
    public int updateReadStatus(String folder, Integer uid, ReadStatus readStatus) {
        if (sLogger.isActivated()) {
            sLogger.debug("updateReadStatus uid=" + uid + " folder=" + folder + " status="
                    + readStatus);
        }
        ContentValues values = new ContentValues();
        values.put(CmsData.KEY_READ_STATUS, readStatus.toInt());
        return mLocalContentResolver.update(CmsData.CONTENT_URI, values,
                Message.SEL_FOLDER_NAME_UID, new String[] {
                        folder, String.valueOf(uid)
                });
    }

    /**
     * Updates read status by messageId and contact for XMS messages
     *
     * @param contact the contact ID
     * @param messageId the message ID
     * @param readStatus the read status
     * @param uid the CMS uid
     * @return the number of rows affected.
     */
    public int updateXmsReadStatus(ContactId contact, String messageId, ReadStatus readStatus,
            Integer uid) {
        if (sLogger.isActivated()) {
            sLogger.debug("updateXmsReadStatus ID=" + messageId + " contact=" + contact
                    + " status=" + readStatus);
        }
        ContentValues values = new ContentValues();
        values.put(CmsData.KEY_READ_STATUS, readStatus.toInt());
        if (uid != null) {
            values.put(CmsData.KEY_UID, uid);
        } else {
            if (ReadStatus.READ == readStatus) {
                sLogger.warn("updateXmsReadStatus ID=" + messageId
                        + " cannot be set to READ if not pushed");
            }
        }
        return mLocalContentResolver.update(CmsData.CONTENT_URI, values,
                Message.SEL_XMS_MSGID_FOLDER, new String[] {
                        messageId, CmsUtils.contactToCmsFolder(contact)
                });
    }

    /**
     * Updates read status by messageType and messageId for RCS message.
     *
     * @param messageType the type
     * @param messageId the message ID
     * @param readStatus the read status
     * @param uid the CMS uid
     * @return the number of rows affected.
     */
    public int updateRcsReadStatus(MessageType messageType, String messageId,
            ReadStatus readStatus, Integer uid) {
        if (sLogger.isActivated()) {
            sLogger.debug("updateRcsReadStatus ID=" + messageId + " type=" + messageType
                    + " status=" + readStatus);
        }
        ContentValues values = new ContentValues();
        values.put(CmsData.KEY_READ_STATUS, readStatus.toInt());
        if (uid != null) {
            values.put(CmsData.KEY_UID, uid);
        } else {
            if (ReadStatus.READ == readStatus) {
                sLogger.warn("updateRcsReadStatus ID=" + messageId
                        + " cannot be set to READ if not pushed");
            }
        }
        return mLocalContentResolver.update(CmsData.CONTENT_URI, values, Message.SEL_TYPE_MSGID,
                new String[] {
                        messageType.toString(), messageId
                });
    }

    /**
     * Updates push status and uid by messageId
     *
     * @param uid the UID
     * @param messageId the message ID
     * @param pushStatus the push status
     * @return the number of row affected
     */
    public int updateXmsPushStatus(Integer uid, ContactId contact, String messageId,
            PushStatus pushStatus) {
        if (sLogger.isActivated()) {
            sLogger.debug("updateXmsPushStatus ID=" + messageId + " contact=" + contact
                    + " status=" + pushStatus);
        }
        ContentValues values = new ContentValues();
        values.put(CmsData.KEY_UID, uid);
        values.put(CmsData.KEY_PUSH_STATUS, pushStatus.toInt());
        return mLocalContentResolver.update(CmsData.CONTENT_URI, values,
                Message.SEL_XMS_MSGID_FOLDER, new String[] {
                        messageId, CmsUtils.contactToCmsFolder(contact)
                });
    }

    /**
     * Removes messages from a folder
     *
     * @param folderName the folder name
     * @return The number of rows affected.
     */
    public int removeMessages(String folderName) {
        return mLocalContentResolver.delete(CmsData.CONTENT_URI, Message.SEL_FOLDER_NAME,
                new String[] {
                    folderName
                });
    }

    /**
     * Update the CMS entry with msgId to READ if READ_REPORTED and DELETED if DELETED_REPORTED
     *
     * @param msgId the message ID
     * @return the results of the applications
     */
    public ContentProviderResult[] updateStatusesWhereReported(String msgId) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation.newUpdate(CmsData.CONTENT_URI)
                .withValue(CmsData.KEY_READ_STATUS, ReadStatus.READ.toInt())
                .withSelection(Message.SEL_READ_REPORTED_MSGID, new String[] {
                    msgId
                }).build());
        ops.add(ContentProviderOperation.newUpdate(CmsData.CONTENT_URI)
                .withValue(CmsData.KEY_DEL_STATUS, DeleteStatus.DELETED.toInt())
                .withSelection(Message.SEL_DEL_REPORTED_MSGID, new String[] {
                    msgId
                }).build());
        try {
            return mLocalContentResolver.applyBatch(CmsData.CONTENT_URI, ops);

        } catch (OperationApplicationException e) {
            sLogger.error("updateStatusesWhereReported failed", e);
            return null;
        }
    }

    /**
     * Purges messages which are marked as deleted.
     *
     * @return The number of rows affected.
     */
    public int purgeDeletedMessages() {
        return mLocalContentResolver.delete(CmsData.CONTENT_URI,
                Message.SEL_DELETED_PROVIDER_ID_NULL, null);
    }

    /**
     * Removes all messages
     *
     * @return The number of rows affected.
     */
    public int removeMessages() {
        return mLocalContentResolver.delete(CmsData.CONTENT_URI, null, null);
    }

    /**
     * Gets native XMS messages
     *
     * @param messageType the type of XMS messages
     * @return the map of native IDs with their associated XMS messages.
     */
    public Map<Long, CmsXmsObject> getNativeMessages(MessageType messageType) {
        List<CmsObject> cmData = getCmsObjects(Message.SEL_TYPE_PROVIDER_ID_NOT_NULL, new String[] {
            messageType.toString()
        }, null);
        Map<Long, CmsXmsObject> messages = new HashMap<>();
        for (CmsObject item : cmData) {
            messages.put(((CmsXmsObject) item).getNativeProviderId(), (CmsXmsObject) item);
        }
        return messages;
    }

    /**
     * Resets rows that have ReadStatus set to READ_REPORTED to READ_REPORT_REQUESTED and
     * DeleteStatus set to DELETE_REPORTED to DELETE_REPORT_REQUESTED.
     */
    public ContentProviderResult[] resetReportedStatus() {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation.newUpdate(CmsData.CONTENT_URI)
                .withValue(CmsData.KEY_READ_STATUS, ReadStatus.READ_REPORT_REQUESTED.toInt())
                .withSelection(Message.SEL_READ_REPORTED, null).build());
        ops.add(ContentProviderOperation.newUpdate(CmsData.CONTENT_URI)
                .withValue(CmsData.KEY_DEL_STATUS, DeleteStatus.DELETED_REPORT_REQUESTED.toInt())
                .withSelection(Message.SEL_DEL_REPORTED, null).build());
        try {
            return mLocalContentResolver.applyBatch(CmsData.CONTENT_URI, ops);

        } catch (OperationApplicationException e) {
            sLogger.error("resetReportedStatus failed", e);
            return null;
        }
    }

    /**
     * Updates the message ID
     *
     * @param contact the contact ID
     * @param msgId the message ID
     * @param newMsgId the new message ID
     * @return true if update occurred successfully
     */
    public boolean updateSmsMessageId(ContactId contact, String msgId, String newMsgId) {
        if (sLogger.isActivated()) {
            sLogger.debug("updateSmsMessageId ID=" + msgId + " contact=" + contact + " new ID="
                    + newMsgId);
        }
        ContentValues values = new ContentValues();
        values.put(CmsData.KEY_MSG_ID, newMsgId);
        return mLocalContentResolver.update(CmsData.CONTENT_URI, values,
                Message.SEL_XMS_MSGID_FOLDER, new String[] {
                        msgId, CmsUtils.contactToCmsFolder(contact)
                }) > 0;
    }

    /**
     * Gets list of CMS objects
     * 
     * @param selection A selection criteria to apply when filtering rows
     * @param selectionArgs The array of arguments for the selection or null if no argument.
     * @param sortOrder the sorting order
     * @return the list of CMS objects
     */
    private List<CmsObject> getCmsObjects(String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        List<CmsObject> messages = new ArrayList<>();
        try {
            cursor = mLocalContentResolver.query(CmsData.CONTENT_URI, null, selection,
                    selectionArgs, sortOrder);
            CursorUtil.assertCursorIsNotNull(cursor, CmsData.CONTENT_URI);
            int msgTypeIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_MSG_TYPE);
            int folderIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_FOLDER);
            int msgIdIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_MSG_ID);
            int uidIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_UID);
            int pushIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_PUSH_STATUS);
            int readIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_READ_STATUS);
            int delIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_DEL_STATUS);
            int chatIdIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_CHAT_ID);
            int nativeIdIdx = cursor.getColumnIndexOrThrow(CmsData.KEY_NATIVE_ID);
            while (cursor.moveToNext()) {
                MessageType msgType = MessageType.valueOf(cursor.getString(msgTypeIdx));
                String folder = cursor.getString(folderIdx);
                String msgId = cursor.getString(msgIdIdx);
                Integer uid = cursor.isNull(uidIdx) ? null : cursor.getInt(uidIdx);
                PushStatus pushStatus = PushStatus.valueOf(cursor.getInt(pushIdx));
                ReadStatus readStatus = ReadStatus.valueOf(cursor.getInt(readIdx));
                DeleteStatus delStatus = DeleteStatus.valueOf(cursor.getInt(delIdx));
                String chatId = cursor.getString(chatIdIdx);
                Long nativeId = cursor.isNull(nativeIdIdx) ? null : cursor.getLong(nativeIdIdx);
                if (CmsObject.isXmsData(msgType)) {
                    messages.add(new CmsXmsObject(msgType, folder, msgId, uid, pushStatus,
                            readStatus, delStatus, nativeId));
                } else {
                    messages.add(new CmsRcsObject(msgType, folder, msgId, uid, pushStatus,
                            readStatus, delStatus, chatId));
                }
            }
            return messages;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private CmsObject getData(String messageId, String selection, String[] selArg) {
        selArg = DatabaseUtils.appendIdWithSelectionArgs(messageId, selArg);
        List<CmsObject> cmsObject = getCmsObjects(selection, selArg, null);
        if (cmsObject.isEmpty()) {
            return null;
        }
        return cmsObject.get(0);
    }

}
