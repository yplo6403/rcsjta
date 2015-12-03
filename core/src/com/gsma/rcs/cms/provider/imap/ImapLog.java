
package com.gsma.rcs.cms.provider.imap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.gsma.rcs.cms.provider.imap.MessageData.DeleteStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.utils.logger.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImapLog {

    /**
     * Current instance
     */
    private static volatile ImapLog sInstance;

    private final LocalContentResolver mLocalContentResolver;

    private static final String SORT_BY_UID_ASC = new StringBuilder(
            MessageData.KEY_UID).append(" ASC").toString();
    
    private static final String[] PROJECTION_ID = new String[] {
            BaseColumns._ID
    };
    private static final String[] PROJECTION_UID = new String[] {
            MessageData.KEY_UID
    };

    // Folder table
    private static final class Folder{

        private static final String SELECTION_NAME = new StringBuilder(FolderData.KEY_NAME)
                .append("=?").toString();
    }

    // Message table
    private static final class Message{

        private static final String PROJECTION_MAX_UID = new StringBuilder().append("MAX(").append(MessageData.KEY_UID).append(")").toString();

        private static final String[] PROJECTION_NATIVE_PROVIDERID_READ_DELETE = new String[] {
                MessageData.KEY_NATIVE_PROVIDER_ID,
                MessageData.KEY_READ_STATUS,
                MessageData.KEY_DELETE_STATUS
        };


        private static final String SELECTION_FOLDER_NAME = new StringBuilder(MessageData.KEY_FOLDER_NAME).append("=?").toString();
        private static final String SELECTION_UID = new StringBuilder(MessageData.KEY_UID).append("=?").toString();

        private static final String SELECTION_MESSAGE_TYPE = new StringBuilder(MessageData.KEY_MESSAGE_TYPE).append("=?").toString();
        private static final String SELECTION_MESSAGE_ID = new StringBuilder(MessageData.KEY_MESSAGE_ID).append("=?").toString();

        private static final String SELECTION_PROVIDER_ID = new StringBuilder(MessageData.KEY_NATIVE_PROVIDER_ID).append("=?").toString();
        private static final String SELECTION_PROVIDER_ID_NULL = new StringBuilder(MessageData.KEY_NATIVE_PROVIDER_ID).append(" is null").toString();
        private static final String SELECTION_PROVIDER_ID_NOT_NULL = new StringBuilder(MessageData.KEY_NATIVE_PROVIDER_ID).append(" is not null").toString();

        private static final String SELECTION_READ_STATUS = new StringBuilder(MessageData.KEY_READ_STATUS).append("=?").toString();
        private static final String SELECTION_DELETE_STATUS = new StringBuilder(MessageData.KEY_DELETE_STATUS).append("=?").toString();
        private static final String SELECTION_READ_STATUS_OR_DELETE_STATUS = new StringBuilder("(").append(SELECTION_READ_STATUS).append(" OR ").append(SELECTION_DELETE_STATUS).append(")").toString();
        private static final String SELECTION_PUSH_STATUS = new StringBuilder(MessageData.KEY_PUSH_STATUS).append("=?").toString();

        private static final String SELECTION_SMS = new StringBuilder(MessageData.KEY_MESSAGE_TYPE).append("='").append(MessageType.SMS).append("'").toString();
        private static final String SELECTION_MMS = new StringBuilder(MessageData.KEY_MESSAGE_TYPE).append("='").append(MessageType.MMS).append("'").toString();
        private static final String SELECTION_XMS = new StringBuilder("(").append(SELECTION_SMS).append(" OR ").append(SELECTION_MMS).append(")").toString();
        private static final String SELECTION_FOLDER_NAME_UID = new StringBuilder(SELECTION_FOLDER_NAME).append(" AND ").append(SELECTION_UID).toString();
        private static final String SELECTION_FOLDER_NAME_MESSAGEID = new StringBuilder(SELECTION_FOLDER_NAME).append(" AND ").append(SELECTION_MESSAGE_ID).toString();
        private static final String SELECTION_XMS_MESSAGEID = new StringBuilder(SELECTION_XMS).append(" AND ").append(SELECTION_MESSAGE_ID).toString();

        private static final String SELECTION_XMS_PUSH_STATUS = new StringBuilder(SELECTION_XMS).append(" AND ").append(SELECTION_PUSH_STATUS).toString();

        private static final String SELECTION_MESSAGE_TYPE_MESSAGE_ID = new StringBuilder(SELECTION_MESSAGE_TYPE).append(" AND ").append(SELECTION_MESSAGE_ID).toString();
        private static final String SELECTION_MESSAGE_TYPE_PROVIDER_ID = new StringBuilder(SELECTION_MESSAGE_TYPE).append(" AND ").append(SELECTION_PROVIDER_ID).toString();
        private static final String SELECTION_FOLDER_NAME_READ_STATUS_DELETE_STATUS = new StringBuilder(SELECTION_FOLDER_NAME).append(" AND ").append(SELECTION_READ_STATUS_OR_DELETE_STATUS).toString();

        private static final String SELECTION_DELETE_STATUS_PROVIDER_ID_NULL = new StringBuilder(SELECTION_DELETE_STATUS).append(" AND ").append(SELECTION_PROVIDER_ID_NULL).toString();
        private static final String SELECTION_MESSAGE_TYPE_PROVIDER_ID_NOT_NULL = new StringBuilder(SELECTION_MESSAGE_TYPE).append(" AND ").append(SELECTION_PROVIDER_ID_NOT_NULL).toString();
    }

    static final int INVALID_ID = -1;

    private static final Logger logger = Logger.getLogger(ImapLog.class.getSimpleName());

    /**
     * Gets the instance of SecurityLog singleton
     * 
     * @param context
     * @return the instance of SecurityLog singleton
     */
    public static ImapLog createInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (ImapLog.class) {
            if (sInstance == null) {
                sInstance = new ImapLog(context);
            }
        }
        return sInstance;
    }

    public static ImapLog getInstance() {
        return sInstance;
    }

    /**
     * Constructor
     * 
     * @param context
     */
    private ImapLog(Context context) {
        mLocalContentResolver = new LocalContentResolver(context.getContentResolver());
    }

    /**
     * Add mailboxData
     * 
     * @param folder
     */
    public void addFolder(FolderData folder) {
        boolean logActivated = logger.isActivated();

        ContentValues values = new ContentValues();
        String name = folder.getName();
        values.put(FolderData.KEY_NEXT_UID, folder.getNextUid());
        values.put(FolderData.KEY_HIGHESTMODSEQ, folder.getModseq());
        values.put(FolderData.KEY_UID_VALIDITY, folder.getUidValidity());
        values.put(FolderData.KEY_NAME, name);

        Integer id = getFolderId(name);
        if (INVALID_ID == id) {
            if (logActivated) {
                logger.debug(new StringBuilder("Add mailboxData : ").append(folder).toString());
            }
            mLocalContentResolver.insert(FolderData.CONTENT_URI, values);
            return;
        }
        if (logActivated) {
            logger.debug(new StringBuilder("Update  mailboxData :").append(folder).toString());
        }
        Uri uri = Uri.withAppendedPath(FolderData.CONTENT_URI, id.toString());
        mLocalContentResolver.update(uri, values, null, null);
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
            cursor = mLocalContentResolver.query(FolderData.CONTENT_URI, null, Folder.SELECTION_NAME,
                    new String[] {
                            folderName
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, FolderData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }

            return new FolderData(folderName,
                    cursor.getInt(cursor.getColumnIndexOrThrow(FolderData.KEY_NEXT_UID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(FolderData.KEY_HIGHESTMODSEQ)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(FolderData.KEY_UID_VALIDITY)));
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get folders data
     * 
     * @return FolderData
     */
    public Map<String, FolderData> getFolders() {
        Cursor cursor = null;
        Map<String, FolderData> folders = new HashMap<String, FolderData>();
        try {
            cursor = mLocalContentResolver.query(FolderData.CONTENT_URI, null, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, FolderData.CONTENT_URI);

            int nameColumnIdx = cursor.getColumnIndexOrThrow(FolderData.KEY_NAME);
            int nextUidColumnIdx = cursor.getColumnIndexOrThrow(FolderData.KEY_NEXT_UID);
            int highestmodseqColumnIdx = cursor.getColumnIndexOrThrow(FolderData.KEY_HIGHESTMODSEQ);
            int uidvalidityColumnIdx = cursor.getColumnIndexOrThrow(FolderData.KEY_UID_VALIDITY);
            while (cursor.moveToNext()) {
                String name = cursor.getString(nameColumnIdx);
                folders.put(name,
                        new FolderData(name, cursor.getInt(nextUidColumnIdx),
                                cursor.getInt(highestmodseqColumnIdx),
                                cursor.getInt(uidvalidityColumnIdx)));
            }
            return folders;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get folderId for a folderName
     * 
     * @param name
     * @return id
     */
    public Integer getFolderId(String name) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(FolderData.CONTENT_URI, PROJECTION_ID,
                    Folder.SELECTION_NAME, new String[] {
                            name
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, FolderData.CONTENT_URI);
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
     * Delete a folder
     * 
     * @param name
     * @param deleteMessages
     * @return id
     */
    public int removeFolder(String name, boolean deleteMessages) {
        if (deleteMessages) {
            removeMessages(name);
        }
        return mLocalContentResolver.delete(FolderData.CONTENT_URI, Folder.SELECTION_NAME,
                new String[] {
                        name
        });
    }

    /**
     * Remove all folders
     * 
     * @param deleteMessages
     * @return int
     */
    public int removeFolders(boolean deleteMessages) {
        if (deleteMessages) {
            removeMessages();
        }
        return mLocalContentResolver.delete(FolderData.CONTENT_URI, null, null);
    }

    /**
     * Add mailboxData
     * 
     * @param message
     */
    public void addMessage(MessageData message) {
        boolean logActivated = logger.isActivated();

        ContentValues values = new ContentValues();
        Integer uid = message.getUid();
        values.put(MessageData.KEY_UID, uid);
        values.put(MessageData.KEY_FOLDER_NAME, message.getFolder());
        values.put(MessageData.KEY_MESSAGE_ID, message.getMessageId());
        values.put(MessageData.KEY_READ_STATUS, message.getReadStatus().toInt());
        values.put(MessageData.KEY_MESSAGE_TYPE, message.getMessageType().name());
        values.put(MessageData.KEY_DELETE_STATUS, message.getDeleteStatus().toInt());
        values.put(MessageData.KEY_PUSH_STATUS, message.getPushStatus().toInt());
        values.put(MessageData.KEY_NATIVE_PROVIDER_ID, message.getNativeProviderId());

        Integer id = INVALID_ID;
        if(message.getFolder()!=null && uid!=null){
            id = getMessageId(message.getFolder(), uid);
        }
        if (INVALID_ID == id) {
            if (logActivated) {
                logger.debug(new StringBuilder("Add messageData : ").append(message).toString());
            }
            mLocalContentResolver.insert(MessageData.CONTENT_URI, values);
            return;
        }
        if (logActivated) {
            logger.debug(new StringBuilder("Update  messageData :").append(message).toString());
        }
        Uri uri = Uri.withAppendedPath(MessageData.CONTENT_URI, id.toString());
        mLocalContentResolver.update(uri, values, null, null);
    }

    public void updateMessage(
            MessageType messageType,
            String messageId,
            String folderName,
            Integer uid,
            Boolean seen,
            Boolean deleted
            ) {
        boolean logActivated = logger.isActivated();

        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_UID, uid);
        values.put(MessageData.KEY_FOLDER_NAME, folderName);
        if(seen){
            values.put(MessageData.KEY_READ_STATUS, ReadStatus.READ.toInt());
        }
        if(deleted){
            values.put(MessageData.KEY_DELETE_STATUS, DeleteStatus.DELETED.toInt());
        }

        if (logActivated) {
            logger.debug(new StringBuilder("Update messageData : ").append(messageType)
                    .append(",").append(messageId).append(",").append(folderName).append(",")
                    .append(uid).toString());
        }
        mLocalContentResolver.update(
                MessageData.CONTENT_URI,
                values,
                Message.SELECTION_MESSAGE_TYPE_MESSAGE_ID,
                new String[]{messageType.toString(), messageId});
    }
    /**
     * Get message by folderName and uid
     * 
     * @param folderName
     * @param uid
     * @return MessageData
     */
    public MessageData getMessage(String folderName, Integer uid) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                   Message.SELECTION_FOLDER_NAME_UID, new String[] {
                            folderName, String.valueOf(uid)
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new MessageData(
                    folderName,
                    uid,
                    ReadStatus.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(MessageData.KEY_READ_STATUS))),
                    DeleteStatus.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(MessageData.KEY_DELETE_STATUS))),
                    PushStatus.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(MessageData.KEY_PUSH_STATUS))),
                    MessageType.valueOf(cursor
                            .getString(cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_TYPE))),
                    cursor.getString(cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID)
                    ));
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
                    Message.SELECTION_FOLDER_NAME_MESSAGEID, new String[] {
                            folderName, messageId
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new MessageData(folderName,
                    cursor.getInt(cursor.getColumnIndexOrThrow(MessageData.KEY_UID)),
                    ReadStatus.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(MessageData.KEY_READ_STATUS))),
                    DeleteStatus.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(MessageData.KEY_DELETE_STATUS))),
                    PushStatus.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(MessageData.KEY_PUSH_STATUS))),
                    MessageType.valueOf(cursor
                            .getString(cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_TYPE))),
                    cursor.getString(cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID)));
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Return the max uid for messages of a folder
     * 
     * @param folderName
     * @return maxUid or null
     */
    public Integer getMaxUidForMessages(String folderName) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, new String[] {
                    Message.PROJECTION_MAX_UID
            }, Message.SELECTION_FOLDER_NAME, new String[] {
                    folderName
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            return cursor.getInt(cursor.getColumnIndexOrThrow( Message.PROJECTION_MAX_UID));
        } finally {
            CursorUtil.close(cursor);
        }
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
                    Message.SELECTION_FOLDER_NAME, new String[] {
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
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID);

            while (cursor.moveToNext()) {
                Integer uid = cursor.getInt(uidIdx);
                if(uid==0){
                    continue;
                }
                messages.put(uid,
                        new MessageData(
                                cursor.getString(folderIdx),
                                uid,
                                ReadStatus.valueOf(cursor.getInt(seenIdx)),
                                DeleteStatus.valueOf(cursor.getInt(deletedIdx)),
                                PushStatus.valueOf(cursor.getInt(pushIdx)),
                                MessageType.valueOf(cursor.getString(messageTypeIdx)),
                                cursor.getString(messageIdIdx),
                                cursor.getLong(nativeProviderIdIdx)));
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get messages by folderName
     *
     * @param folderName
     * @return MessageData
     */
    public List<MessageData> getMessages(String folderName, ReadStatus readStatus, DeleteStatus deleteStatus) {
        Cursor cursor = null;
        List<MessageData> messages = new ArrayList<>();
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                    Message.SELECTION_FOLDER_NAME_READ_STATUS_DELETE_STATUS, new String[] {
                            folderName,
                            String.valueOf(readStatus.toInt()),
                            String.valueOf(deleteStatus.toInt()),
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            int uidIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_UID);
            int folderIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_FOLDER_NAME);
            int seenIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_READ_STATUS);
            int deletedIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_DELETE_STATUS);
            int pushIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_PUSH_STATUS);
            int messageTypeIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_TYPE);
            int messageIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                Integer uid = cursor.getInt(uidIdx);
                messages.add(new MessageData(
                        cursor.getString(folderIdx),
                        uid,
                        ReadStatus.valueOf(cursor.getInt(seenIdx)),
                        DeleteStatus.valueOf(cursor.getInt(deletedIdx)),
                        PushStatus.valueOf(cursor.getInt(pushIdx)),
                        MessageType.valueOf(cursor.getString(messageTypeIdx)),
                        cursor.getString(messageIdIdx),
                        cursor.getLong(nativeProviderIdIdx)));

            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get messages by readStatus
     *
     * @param readStatus
     * @return MessageData
     */
    public List<MessageData> getMessages(ReadStatus readStatus) {
        Cursor cursor = null;
        List<MessageData> messages = new ArrayList<>();
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                    Message.SELECTION_READ_STATUS, new String[] {
                            String.valueOf(readStatus.toInt())
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            int uidIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_UID);
            int folderIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_FOLDER_NAME);
            int seenIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_READ_STATUS);
            int deletedIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_DELETE_STATUS);
            int pushIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_PUSH_STATUS);
            int messageTypeIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_TYPE);
            int messageIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                Integer uid = cursor.getInt(uidIdx);
                messages.add(new MessageData(
                        cursor.getString(folderIdx),
                        uid,
                        ReadStatus.valueOf(cursor.getInt(seenIdx)),
                        DeleteStatus.valueOf(cursor.getInt(deletedIdx)),
                        PushStatus.valueOf(cursor.getInt(pushIdx)),
                        MessageType.valueOf(cursor.getString(messageTypeIdx)),
                        cursor.getString(messageIdIdx),
                        cursor.getLong(nativeProviderIdIdx)));

            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get messages by deleteStatus
     *
     * @param deleteStatus
     * @return MessageData
     */
    public List<MessageData> getMessages(DeleteStatus deleteStatus) {
        Cursor cursor = null;
        List<MessageData> messages = new ArrayList<>();
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                    Message.SELECTION_DELETE_STATUS, new String[] {
                            String.valueOf(deleteStatus.toInt())
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            int uidIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_UID);
            int folderIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_FOLDER_NAME);
            int seenIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_READ_STATUS);
            int deletedIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_DELETE_STATUS);
            int pushIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_PUSH_STATUS);
            int messageTypeIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_TYPE);
            int messageIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                Integer uid = cursor.getInt(uidIdx);
                messages.add(new MessageData(
                        cursor.getString(folderIdx),
                        uid,
                        ReadStatus.valueOf(cursor.getInt(seenIdx)),
                        DeleteStatus.valueOf(cursor.getInt(deletedIdx)),
                        PushStatus.valueOf(cursor.getInt(pushIdx)),
                        MessageType.valueOf(cursor.getString(messageTypeIdx)),
                        cursor.getString(messageIdIdx),
                        cursor.getLong(nativeProviderIdIdx)));

            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get messages by deleteStatus
     *
     * @param pushStatus
     * @return MessageData
     */
    public List<MessageData> getXmsMessages(PushStatus pushStatus) {
        Cursor cursor = null;
        List<MessageData> messages = new ArrayList<>();
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                    Message.SELECTION_XMS_PUSH_STATUS, new String[] {
                            String.valueOf(pushStatus.toInt())
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            int uidIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_UID);
            int folderIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_FOLDER_NAME);
            int seenIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_READ_STATUS);
            int deletedIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_DELETE_STATUS);
            int pushIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_PUSH_STATUS);
            int messageTypeIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_TYPE);
            int messageIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                Integer uid = cursor.getInt(uidIdx);
                messages.add(new MessageData(
                        cursor.getString(folderIdx),
                        uid,
                        ReadStatus.valueOf(cursor.getInt(seenIdx)),
                        DeleteStatus.valueOf(cursor.getInt(deletedIdx)),
                        PushStatus.valueOf(cursor.getInt(pushIdx)),
                        MessageType.valueOf(cursor.getString(messageTypeIdx)),
                        cursor.getString(messageIdIdx),
                        cursor.getLong(nativeProviderIdIdx)));

            }
            return messages;
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
        Map<Integer, MessageData> messages = new HashMap<Integer, MessageData>();
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
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                messages.put(cursor.getInt(idIdx),
                        new MessageData(
                                cursor.getString(folderIdx),
                                cursor.getInt(uidIdx),
                                ReadStatus.valueOf(cursor.getInt(seenIdx)),
                                DeleteStatus.valueOf(cursor.getInt(deletedIdx)),
                                PushStatus.valueOf(cursor.getInt(pushIdx)),
                                MessageType.valueOf(cursor.getString(messageTypeIdx)),
                                cursor.getString(messageIdIdx),
                                cursor.getLong(nativeProviderIdIdx)));

            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get messageId for a folder name and uid message
     * 
     * @param folderName
     * @param uid
     * @return id
     */
    public Integer getMessageId(String folderName, Integer uid) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, PROJECTION_ID,
                    Message.SELECTION_FOLDER_NAME_UID, new String[] {
                            folderName, String.valueOf(uid)
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            if (cursor.moveToFirst()) {
                int idColumnIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
                return cursor.getInt(idColumnIdx);
            }
            return INVALID_ID;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    public Integer getUidForXmsMessage(String baseId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, PROJECTION_UID,
                    Message.SELECTION_XMS_MESSAGEID, new String[] {baseId}, null);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            if (cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow(MessageData.KEY_UID));
            }
            return null;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    public void updateDeleteStatus(DeleteStatus deleteStatus){
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_DELETE_STATUS, deleteStatus.toInt());
        mLocalContentResolver.update(
                MessageData.CONTENT_URI,
                values,
                null,
                null);
    }

    public void updateDeleteStatus(String folder, Integer uid, DeleteStatus deleteStatus){
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_DELETE_STATUS, deleteStatus.toInt());
        mLocalContentResolver.update(
                MessageData.CONTENT_URI,
                values,
                Message.SELECTION_FOLDER_NAME_UID,
                new String[]{folder, String.valueOf(uid)});
    }

    public void updateDeleteStatus(MessageType messageType, String messageId, DeleteStatus deleteStatus){
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_DELETE_STATUS, deleteStatus.toInt());
        mLocalContentResolver.update(
                MessageData.CONTENT_URI,
                values,
                Message.SELECTION_MESSAGE_TYPE_MESSAGE_ID,
                new String[]{messageType.toString(), messageId});
    }

    public void updateReadStatus(String folder, Integer uid, ReadStatus readStatus){
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_READ_STATUS, readStatus.toInt());
        mLocalContentResolver.update(
                MessageData.CONTENT_URI,
                values,
                Message.SELECTION_FOLDER_NAME_UID,
                new String[]{folder, String.valueOf(uid)});
    }

    public void updateReadStatus(MessageType messageType, String messageId, ReadStatus readStatus){
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_READ_STATUS, readStatus.toInt());
        mLocalContentResolver.update(
                MessageData.CONTENT_URI,
                values,
                Message.SELECTION_MESSAGE_TYPE_MESSAGE_ID,
                new String[]{messageType.toString(), messageId});
    }

    public void updateXmsPushStatus(Integer uid, String messageId, PushStatus pushStatus){
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_UID, uid);
        values.put(MessageData.KEY_PUSH_STATUS, pushStatus.toInt());
        mLocalContentResolver.update(
                MessageData.CONTENT_URI,
                values,
                Message.SELECTION_XMS_MESSAGEID,
                new String[]{messageId});
    }

    public void updatePushStatus(MessageType messageType, PushStatus pushStatus){
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_PUSH_STATUS, pushStatus.toInt());
        mLocalContentResolver.update(
                MessageData.CONTENT_URI,
                values,
                Message.SELECTION_MESSAGE_TYPE,
                new String[]{messageType.toString()});
    }

    /**
     * Remove a message
     * 
     * @param folderName
     * @param uid
     * @return int
     */
    public int removeMessage(String folderName, Integer uid) {
        return mLocalContentResolver.delete(MessageData.CONTENT_URI, Message.SELECTION_FOLDER_NAME_UID,
                new String[] {
                        folderName, String.valueOf(uid)
        });
    }

    /**
     * Remove messages from a folder
     * 
     * @param folderName
     * @return int
     */
    public int removeMessages(String folderName) {
        return mLocalContentResolver.delete(MessageData.CONTENT_URI, Message.SELECTION_FOLDER_NAME,
                new String[] {
                        folderName
        });
    }

    public void updateDeleteStatus(MessageType messageType, Long nativeProviderId, DeleteStatus deleteStatus){
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_DELETE_STATUS, deleteStatus.toInt());
        values.put(MessageData.KEY_NATIVE_PROVIDER_ID, (String)null);
        mLocalContentResolver.update(
                MessageData.CONTENT_URI,
                values,
                Message.SELECTION_MESSAGE_TYPE_PROVIDER_ID,
                new String[]{messageType.toString(), String.valueOf(nativeProviderId)});
    }

    public void updateReadStatus(MessageType messageType, Long nativeProviderId, ReadStatus readStatus){
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_READ_STATUS, readStatus.toInt());
        mLocalContentResolver.update(
                MessageData.CONTENT_URI,
                values,
                Message.SELECTION_MESSAGE_TYPE_PROVIDER_ID,
                new String[]{messageType.toString(), String.valueOf(nativeProviderId)});
    }

    /**
     * Purge messages
     * @return int
     */
    public int purgeMessages() {
        return mLocalContentResolver.delete(MessageData.CONTENT_URI, Message.SELECTION_DELETE_STATUS_PROVIDER_ID_NULL, new String[]{String.valueOf(DeleteStatus.DELETED.toInt())});
    }

    /**
     * Remove all messages
     * 
     * @return int
     */
    public int removeMessages() {
        return mLocalContentResolver.delete(MessageData.CONTENT_URI, null, null);
    }

    public Map<Long,MessageData> getNativeMessages(MessageType messageType) {
        Cursor cursor = null;
        Map<Long,MessageData> messages = new HashMap<>();
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, Message.PROJECTION_NATIVE_PROVIDERID_READ_DELETE,
                    Message.SELECTION_MESSAGE_TYPE_PROVIDER_ID_NOT_NULL, new String[] {
                            messageType.toString()}, null);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            int readIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_READ_STATUS);
            int deletedIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_DELETE_STATUS);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(nativeProviderIdIdx);
                messages.put(id,
                        new MessageData(
                                id,
                                ReadStatus.valueOf(cursor.getInt(readIdx)),
                                DeleteStatus.valueOf(cursor.getInt(deletedIdx))));

            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

}
