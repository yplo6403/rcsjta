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

package com.gsma.rcs.provider.xms;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.MmsPartLog;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * XMS log utilities
 */
public class XmsLog {

    private static final Logger sLogger = Logger.getLogger(XmsLog.class.getName());

    private static final int FIRST_COLUMN_IDX = 0;

    private static final String SORT_BY_DATE_DESC = XmsData.KEY_TIMESTAMP + " DESC";

    private static final String SEL_XMS_CONTACT = XmsData.KEY_CONTACT + "=?";
    private static final String SEL_XMS_MSGID = XmsData.KEY_MESSAGE_ID + "=?";
    private static final String SEL_PART_MSID = PartData.KEY_MESSAGE_ID + "=?";

    private static final String SEL_XMS_NATIVE_ID = XmsData.KEY_NATIVE_ID + "=?";

    private static final String SEL_UNREAD = XmsData.KEY_READ_STATUS + "="
            + ReadStatus.UNREAD.toInt();
    private static final String SEL_NATIVE_THREAD_ID = XmsData.KEY_NATIVE_THREAD_ID + "=?";

    private static final String SEL_XMS_MMS = XmsData.KEY_MIME_TYPE + "='"
            + MimeType.MULTIMEDIA_MESSAGE + "'";

    private static final String SEL_NATIVE_THREAD_ID_MMS = SEL_NATIVE_THREAD_ID + " AND "
            + SEL_XMS_MMS;

    private static final String SEL_NATIVE_THREAD_ID_UNREAD = SEL_NATIVE_THREAD_ID + " AND "
            + SEL_UNREAD;
    private static final String SEL_DIR_INCOMING = XmsData.KEY_DIRECTION + "="
            + Direction.INCOMING.toInt();

    private static final String SEL_XMS_SMS = XmsData.KEY_MIME_TYPE + "='" + MimeType.TEXT_MESSAGE
            + "'";

    private static final String SEL_XMS_SMS_NATIVE_ID = SEL_XMS_NATIVE_ID + " AND " + SEL_XMS_SMS;

    private static final String SEL_XMS_MMS_NATIVE_ID = SEL_XMS_NATIVE_ID + " AND " + SEL_XMS_MMS;

    private static final String SEL_NATIVE_THREAD_ID_MMS_UNREAD_INCOMING = SEL_NATIVE_THREAD_ID_UNREAD
            + " AND " + SEL_DIR_INCOMING + " AND " + SEL_XMS_MMS;

    private static final String SEL_XMS_DIR = XmsData.KEY_DIRECTION + "=?";
    private static final String SEL_XMS_CORRELATOR = XmsData.KEY_MESSAGE_CORRELATOR + "=?";
    private static final String SEL_XMS_CONTACT_DIR_CORRELATOR = SEL_XMS_CONTACT + " AND "
            + SEL_XMS_DIR + " AND " + SEL_XMS_CORRELATOR + " AND " + SEL_XMS_SMS;

    private static final String[] PROJ_MSGID = new String[] {
        XmsData.KEY_MESSAGE_ID
    };

    private static final String[] PROJ_MSGID_CONTACT = new String[] {
            XmsData.KEY_MESSAGE_ID, XmsData.KEY_CONTACT
    };

    private static final String EXT_IMAGE_JPEG = "jpg";
    private static final String EXT_IMAGE_PNG = "png";

    public static final String SEL_XMS_CONTACT_MSGID = SEL_XMS_CONTACT + " AND " + SEL_XMS_MSGID;

    private static final String SEL_CONTACT_MSGID_NOT_READ_INCOMING = SEL_XMS_CONTACT_MSGID
            + " AND " + SEL_UNREAD + " AND " + SEL_DIR_INCOMING;

    /**
     * Current instance
     */
    private static volatile XmsLog sInstance;
    private final LocalContentResolver mLocalContentResolver;
    private final Context mCtx;
    private final RcsSettings mRcsSettings;

    private XmsLog(Context ctx, RcsSettings settings, LocalContentResolver localContentResolver) {
        mCtx = ctx;
        mRcsSettings = settings;
        mLocalContentResolver = localContentResolver;
    }

    /**
     * Gets the instance of XmsLog singleton
     *
     * @param ctx The context
     * @param localContentResolver Local content resolver
     * @return the instance of XmsLog singleton
     */
    public static XmsLog getInstance(Context ctx, RcsSettings settings,
            LocalContentResolver localContentResolver) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (XmsLog.class) {
            if (sInstance == null) {
                sInstance = new XmsLog(ctx, settings, localContentResolver);
            }
        }
        return sInstance;
    }

    public static XmsLog getInstance() {
        return sInstance;
    }

    public Cursor getXmsMessage(ContactId contact, String messageId) {
        Cursor cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                SEL_XMS_CONTACT_MSGID, new String[] {
                        contact.toString(), messageId
                }, null);
        CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
        return cursor;
    }

    private Cursor getXmsData(String columnName, ContactId contact, String xmsId) {
        String[] projection = {
            columnName
        };
        Cursor cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, projection,
                SEL_XMS_CONTACT_MSGID, new String[] {
                        contact.toString(), xmsId
                }, null);
        CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
        if (!cursor.moveToNext()) {
            CursorUtil.close(cursor);
            return null;
        }
        return cursor;
    }

    public XmsMessage.State getState(ContactId contact, String xmsId) {
        Cursor cursor = getXmsData(XmsData.KEY_STATE, contact, xmsId);
        if (cursor == null) {
            return null;
        }
        Integer state = getDataAsInteger(cursor);
        if (state == null) {
            throw new ServerApiPersistentStorageException("State is null for ID=" + xmsId
                    + " contact=" + contact.toString());
        }
        return XmsMessage.State.valueOf(state);
    }

    private Integer getDataAsInteger(Cursor cursor) {
        try {
            if (cursor.isNull(FIRST_COLUMN_IDX)) {
                return null;
            }
            return cursor.getInt(FIRST_COLUMN_IDX);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    public XmsMessage.ReasonCode getReasonCode(ContactId contact, String xmsId) {
        Cursor cursor = getXmsData(XmsData.KEY_REASON_CODE, contact, xmsId);
        if (cursor == null) {
            return null;
        }
        Integer reason = getDataAsInteger(cursor);
        if (reason == null) {
            throw new ServerApiPersistentStorageException("Reason code is null for ID=" + xmsId
                    + " contact=" + contact.toString());
        }
        return XmsMessage.ReasonCode.valueOf(reason);
    }

    public void addSms(SmsDataObject sms) {
        String contact = sms.getContact().toString();
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_MESSAGE_ID, sms.getMessageId());
        values.put(XmsData.KEY_CONTACT, contact);
        values.put(XmsData.KEY_CHAT_ID, contact);
        values.put(XmsData.KEY_CONTENT, sms.getBody());
        values.put(XmsData.KEY_MIME_TYPE, XmsMessageLog.MimeType.TEXT_MESSAGE);
        values.put(XmsData.KEY_DIRECTION, sms.getDirection().toInt());
        values.put(XmsData.KEY_TIMESTAMP, sms.getTimestamp());
        values.put(XmsData.KEY_TIMESTAMP_SENT, sms.getTimestampSent());
        values.put(XmsData.KEY_TIMESTAMP_DELIVERED, sms.getTimestampDelivered());
        values.put(XmsData.KEY_STATE, sms.getState().toInt());
        values.put(XmsData.KEY_REASON_CODE, sms.getReasonCode().toInt());
        values.put(XmsData.KEY_READ_STATUS, sms.getReadStatus().toInt());
        values.put(XmsData.KEY_NATIVE_ID, sms.getNativeProviderId());
        values.put(XmsData.KEY_NATIVE_THREAD_ID, sms.getNativeThreadId());
        values.put(XmsData.KEY_MESSAGE_CORRELATOR, sms.getCorrelator());
        mLocalContentResolver.insert(XmsData.CONTENT_URI, values);
    }

    /**
     * Update XMS message read status
     *
     * @param contact the contact ID (i.e. the folder name)
     * @param msgId message ID
     * @return true if update is successful.
     */
    public boolean markMessageAsRead(ContactId contact, String msgId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Marking message as read Id=" + msgId);
        }
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_READ_STATUS, RcsService.ReadStatus.READ.toInt());
        return mLocalContentResolver.update(XmsData.CONTENT_URI, values,
                SEL_CONTACT_MSGID_NOT_READ_INCOMING, new String[] {
                        contact.toString(), msgId
                }) > 0;
    }

    public void addIncomingMms(MmsDataObject mms) {
        ContactId remote = mms.getContact();
        String contact = remote.toString();
        String mmsId = mms.getMessageId();
        if (isMessagePersisted(remote, mmsId)) {
            throw new ServerApiPersistentStorageException("MMS already exists ID=" + mmsId
                    + " for contact=" + contact);
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Insert mms: " + mms);
        }
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (MmsDataObject.MmsPart mmsPart : mms.getMmsParts()) {
            String content = mmsPart.getContentText();
            if (content == null) {
                content = mmsPart.getFile().toString();
            }
            ContentProviderOperation op = ContentProviderOperation.newInsert(PartData.CONTENT_URI)
                    .withValue(PartData.KEY_MESSAGE_ID, mmsPart.getMessageId())
                    .withValue(PartData.KEY_MIME_TYPE, mmsPart.getMimeType())
                    .withValue(PartData.KEY_FILENAME, mmsPart.getFileName())
                    .withValue(PartData.KEY_FILESIZE, mmsPart.getFileSize())
                    .withValue(PartData.KEY_CONTENT, content)
                    .withValue(PartData.KEY_FILEICON, mmsPart.getFileIcon()).build();
            ops.add(op);
        }
        if (!ops.isEmpty()) {
            try {
                mLocalContentResolver.applyBatch(PartData.CONTENT_URI, ops);

            } catch (OperationApplicationException e) {
                sLogger.error("Failed to insert parts for MMS id=" + mmsId, e);
            }
        }
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_MESSAGE_ID, mmsId);
        values.put(XmsData.KEY_CONTACT, contact);
        values.put(XmsData.KEY_CHAT_ID, contact);
        values.put(XmsData.KEY_CONTENT, mms.getSubject());
        values.put(XmsData.KEY_MIME_TYPE, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE);
        values.put(XmsData.KEY_DIRECTION, Direction.INCOMING.toInt());
        values.put(XmsData.KEY_TIMESTAMP, mms.getTimestamp());
        values.put(XmsData.KEY_TIMESTAMP_SENT, mms.getTimestampSent());
        values.put(XmsData.KEY_TIMESTAMP_DELIVERED, mms.getTimestampDelivered());
        values.put(XmsData.KEY_STATE, mms.getState().toInt());
        values.put(XmsData.KEY_REASON_CODE, mms.getReasonCode().toInt());
        values.put(XmsData.KEY_READ_STATUS, mms.getReadStatus().toInt());
        values.put(XmsData.KEY_NATIVE_ID, mms.getNativeProviderId());
        mLocalContentResolver.insert(XmsData.CONTENT_URI, values);
    }

    private void persistPdu(Uri file, byte[] pdu) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file.getPath());
            fos.write(pdu);
        } finally {
            CloseableUtils.tryToClose(fos);
        }
    }

    public boolean isPartPersisted(String mmsId) {
        Cursor cursor = null;
        try {
            cursor = getMmsPart(mmsId);
            return cursor.moveToNext();

        } finally {
            CursorUtil.close(cursor);
        }
    }

    public String addOutgoingMms(MmsDataObject mms) throws FileAccessException {
        ContactId remote = mms.getContact();
        String contact = remote.toString();
        String mmsId = mms.getMessageId();
        if (mmsId == null) {
            mmsId = IdGenerator.generateMessageID();
        } else {
            if (isMessagePersisted(remote, mmsId)) {
                throw new ServerApiPersistentStorageException("MMS already exists ID=" + mmsId
                        + " for contact=" + contact);
            }
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Insert mms: " + mms);
        }
        /*
         * The MMS may be persisted several time: one for each recipient. But the part is persisted
         * only once even if sent to multiple contacts.
         */
        if (!isPartPersisted(mmsId)) {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            for (MmsDataObject.MmsPart mmsPart : mms.getMmsParts()) {
                String mimeType = mmsPart.getMimeType();
                ContentProviderOperation.Builder build = ContentProviderOperation
                        .newInsert(PartData.CONTENT_URI).withValue(PartData.KEY_MESSAGE_ID, mmsId)
                        .withValue(PartData.KEY_MIME_TYPE, mimeType);
                String content = mmsPart.getContentText();
                if (content == null) {
                    /* It is a MMS file */
                    Uri fileUri = mmsPart.getFile();
                    build.withValue(PartData.KEY_FILEICON, mmsPart.getFileIcon());
                    if (Direction.OUTGOING == mms.getDirection()) {
                        byte[] pdu = mmsPart.getPdu();
                        try {
                            if (pdu == null) {
                                Uri localFile = FileUtils.createCopyOfSentFile(fileUri,
                                        mmsPart.getFileName(), mimeType, mRcsSettings);
                                String filename = FileUtils.getFileName(mCtx, localFile);
                                long fileSize = FileUtils.getFileSize(mCtx, localFile);
                                build.withValue(PartData.KEY_FILENAME, filename)
                                        .withValue(PartData.KEY_FILESIZE, fileSize)
                                        .withValue(PartData.KEY_CONTENT, localFile.toString());

                            } else {
                                /* Image is compressed into JPEG format */
                                String filename = mmsPart.getFileName();
                                /* Change file extension to JPEG if PNG originally */
                                String fileExt = MimeManager.getFileExtension(filename);
                                if (fileExt != null && EXT_IMAGE_PNG.equals(fileExt.toLowerCase())) {
                                    filename = filename.substring(0, filename.lastIndexOf('.') + 1)
                                            .concat(EXT_IMAGE_JPEG);
                                }
                                Uri localUri = ContentManager.generateUriForSentContent(filename,
                                        mimeType, mRcsSettings);
                                persistPdu(localUri, pdu);
                                build.withValue(PartData.KEY_FILENAME, filename)
                                        .withValue(PartData.KEY_FILESIZE, pdu.length)
                                        .withValue(PartData.KEY_CONTENT, localUri.toString());
                            }
                        } catch (IOException e) {
                            // Nothing to do
                        }
                    } else {
                        build.withValue(PartData.KEY_CONTENT, fileUri.toString())
                                .withValue(PartData.KEY_FILENAME, mmsPart.getFileName())
                                .withValue(PartData.KEY_FILESIZE, mmsPart.getFileSize());
                    }
                } else {
                    /* it is the body */
                    build.withValue(PartData.KEY_CONTENT, content);
                }
                ops.add(build.build());
            }
            if (!ops.isEmpty()) {
                try {
                    mLocalContentResolver.applyBatch(PartData.CONTENT_URI, ops);

                } catch (OperationApplicationException e) {
                    sLogger.error("Failed to insert parts for MMS id=" + mmsId, e);
                }
            }
        }
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_MESSAGE_ID, mmsId);
        values.put(XmsData.KEY_CONTACT, contact);
        values.put(XmsData.KEY_CHAT_ID, contact);
        values.put(XmsData.KEY_CONTENT, mms.getSubject());
        values.put(XmsData.KEY_MIME_TYPE, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE);
        values.put(XmsData.KEY_DIRECTION, Direction.OUTGOING.toInt());
        values.put(XmsData.KEY_TIMESTAMP, mms.getTimestamp());
        values.put(XmsData.KEY_TIMESTAMP_SENT, mms.getTimestampSent());
        values.put(XmsData.KEY_TIMESTAMP_DELIVERED, mms.getTimestampDelivered());
        values.put(XmsData.KEY_STATE, mms.getState().toInt());
        values.put(XmsData.KEY_REASON_CODE, mms.getReasonCode().toInt());
        values.put(XmsData.KEY_READ_STATUS, mms.getReadStatus().toInt());
        values.put(XmsData.KEY_NATIVE_ID, mms.getNativeProviderId());
        mLocalContentResolver.insert(XmsData.CONTENT_URI, values);
        return mmsId;
    }

    private Set<Uri> getPartUris(String mmsId) {
        Cursor cursor = null;
        Set<Uri> uris = new HashSet<>();
        try {
            cursor = getMmsPart(mmsId);
            if (!cursor.moveToNext()) {
                return uris;
            }
            int contentIdx = cursor.getColumnIndexOrThrow(PartData.KEY_CONTENT);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(PartData.KEY_MIME_TYPE);
            do {
                String mimeType = cursor.getString(mimeTypeIdx);
                if (!MmsPartLog.MimeType.TEXT_MESSAGE.equals(mimeType)
                        && !MmsPartLog.MimeType.APPLICATION_SMIL.equals(mimeType)) {
                    uris.add(Uri.parse(cursor.getString(contentIdx)));
                }
            } while (cursor.moveToNext());
            return uris;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Deletes MMS parts
     *
     * @param messageId the The message ID
     * @param dir the direction
     * @return the number of rows affected
     */
    public int deleteMmsParts(String messageId, Direction dir) {
        /* First delete the local file for outgoing MMS */
        if (Direction.OUTGOING == dir) {
            for (Uri file : getPartUris(messageId)) {
                new File(file.getPath()).delete();
            }
        }
        return mLocalContentResolver.delete(PartData.CONTENT_URI, SEL_PART_MSID, new String[] {
            messageId
        });
    }

    public Cursor getSmsMessage(long nativeId) {
        Cursor cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                SEL_XMS_SMS_NATIVE_ID, new String[] {
                    String.valueOf(nativeId)
                }, null);
        CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
        return cursor;
    }

    /**
     * Get unread MMS from native thread ID
     * 
     * @param nativeThreadId the native thread ID
     * @return set of unread MMS Ids by contact
     */
    public Map<ContactId, Set<String>> getUnreadMms(Long nativeThreadId) {
        Cursor cursor = null;
        Map<ContactId, Set<String>> unreads = new HashMap<>();
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJ_MSGID_CONTACT,
                    SEL_NATIVE_THREAD_ID_MMS_UNREAD_INCOMING, new String[] {
                        String.valueOf(nativeThreadId)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            int messageIdIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.CONTACT);
            while (cursor.moveToNext()) {
                String msgId = cursor.getString(messageIdIdx);
                String number = cursor.getString(contactIdx);
                ContactId contact = ContactUtil.createContactIdFromTrustedData(number);
                if (unreads.containsKey(contact)) {
                    Set<String> msgIds = unreads.get(contact);
                    msgIds.add(msgId);

                } else {
                    Set<String> msgIds = new HashSet<>();
                    msgIds.add(msgId);
                    unreads.put(contact, msgIds);
                }
            }
            return unreads;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Gets the MMSs by native thread ID
     * 
     * @param nativeThreadId the native thread ID
     * @return the MMSs by native thread ID
     */
    public Map<ContactId, Set<String>> getMmsMessages(Long nativeThreadId) {
        Cursor cursor = null;
        Map<ContactId, Set<String>> mmss = new HashMap<>();
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                    SEL_NATIVE_THREAD_ID_MMS, new String[] {
                        String.valueOf(nativeThreadId)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            int messageIdIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.CONTACT);
            while (cursor.moveToNext()) {
                String msgId = cursor.getString(messageIdIdx);
                String number = cursor.getString(contactIdx);
                ContactId contact = ContactUtil.createContactIdFromTrustedData(number);
                if (mmss.containsKey(contact)) {
                    Set<String> msgIds = mmss.get(contact);
                    msgIds.add(msgId);

                } else {
                    Set<String> msgIds = new HashSet<>();
                    msgIds.add(msgId);
                    mmss.put(contact, msgIds);
                }
            }
            return mmss;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Gets the set of contacts having a message ID
     * 
     * @param msgId message ID
     * @return set of contacts having a message ID
     */
    public Set<ContactId> getContactsForXmsId(String msgId) {
        Cursor cursor = null;
        Set<ContactId> contacts = new HashSet<>();
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJ_MSGID_CONTACT,
                    SEL_XMS_MSGID, new String[] {
                        msgId
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.CONTACT);
            while (cursor.moveToNext()) {
                String number = cursor.getString(contactIdx);
                ContactId contact = ContactUtil.createContactIdFromTrustedData(number);
                contacts.add(contact);
            }
            return contacts;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get SMS messages IDs
     * 
     * @param contact the contact ID (i.e. the folder name)
     * @param direction the direction
     * @param correlator the correlation string
     * @return the list of SMS messages IDs sorted by descending tiemstamp order
     */
    public List<String> getMessageIdsMatchingCorrelator(ContactId contact, Direction direction,
            String correlator) {
        Cursor cursor = null;
        List<String> messageIds = new ArrayList<>();
        // Escape quote to perform SQL query
        correlator = correlator.replaceAll("'", "\'");
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                    SEL_XMS_CONTACT_DIR_CORRELATOR, new String[] {
                            contact.toString(), String.valueOf(direction.toInt()), correlator
                    }, SORT_BY_DATE_DESC);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            int messageIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_MESSAGE_ID);
            while (cursor.moveToNext()) {
                messageIds.add(cursor.getString(messageIdIdx));
            }
            return messageIds;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Cursor getMmsPart(String messageId) {
        Cursor cursor = mLocalContentResolver.query(PartData.CONTENT_URI, null, SEL_PART_MSID,
                new String[] {
                    messageId
                }, null);
        CursorUtil.assertCursorIsNotNull(cursor, PartData.CONTENT_URI);
        return cursor;
    }

    public boolean setStateAndTimestamp(ContactId contact, String messageId, State state,
            XmsMessage.ReasonCode reasonCode, long timestamp, long timestampSent) {
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_STATE, state.toInt());
        values.put(XmsData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(XmsData.KEY_TIMESTAMP, timestamp);
        values.put(XmsData.KEY_TIMESTAMP_SENT, timestampSent);
        return mLocalContentResolver.update(XmsData.CONTENT_URI, values, SEL_XMS_CONTACT_MSGID,
                new String[] {
                        contact.toString(), messageId
                }) > 0;
    }

    public XmsDataObject getXmsDataObject(ContactId contact, String messageId) {
        Cursor cursor = null;
        try {
            cursor = getXmsMessage(contact, messageId);
            if (!cursor.moveToNext()) {
                return null;
            }
            String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(XmsData.KEY_MIME_TYPE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(XmsData.KEY_CONTENT));
            Direction dir = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(XmsData.KEY_DIRECTION)));
            ReadStatus readStatus = ReadStatus.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(XmsData.KEY_READ_STATUS)));
            long date = cursor.getLong(cursor.getColumnIndexOrThrow(XmsData.KEY_TIMESTAMP));
            if (MimeType.TEXT_MESSAGE.equals(mimeType)) {
                return new SmsDataObject(messageId, contact, content, dir, date, readStatus);

            } else {
                List<MmsDataObject.MmsPart> mmsParts = new ArrayList<>(getParts(messageId));
                return new MmsDataObject(messageId, contact, content, dir, readStatus, date, null,
                        null, mmsParts);
            }
        } finally {
            CursorUtil.close(cursor);
        }
    }

    public List<MmsDataObject.MmsPart> getParts(String mmsId) {
        Cursor cursor = null;
        List<MmsDataObject.MmsPart> parts = new ArrayList<>();
        try {
            cursor = getMmsPart(mmsId);
            if (!cursor.moveToNext()) {
                return parts;
            }
            int messageIdIdx = cursor.getColumnIndexOrThrow(PartData.KEY_MESSAGE_ID);
            int contentIdx = cursor.getColumnIndexOrThrow(PartData.KEY_CONTENT);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(PartData.KEY_MIME_TYPE);
            int filenameIdx = cursor.getColumnIndexOrThrow(PartData.KEY_FILENAME);
            int fileSizeIdx = cursor.getColumnIndexOrThrow(PartData.KEY_FILESIZE);
            int fileiconIdx = cursor.getColumnIndexOrThrow(PartData.KEY_FILEICON);
            do {
                String mimeType = cursor.getString(mimeTypeIdx);
                MmsDataObject.MmsPart partData;
                if (MmsPartLog.MimeType.TEXT_MESSAGE.equals(mimeType)
                        || MmsPartLog.MimeType.APPLICATION_SMIL.equals(mimeType)) {
                    partData = new MmsDataObject.MmsPart(cursor.getString(messageIdIdx), mimeType,
                            cursor.getString(contentIdx));
                } else {
                    Long fileSize = cursor.isNull(fileSizeIdx) ? null : cursor.getLong(fileSizeIdx);
                    byte[] fileIcon = cursor.isNull(fileiconIdx) ? null : cursor
                            .getBlob(fileiconIdx);
                    Uri file = Uri.parse(cursor.getString(contentIdx));
                    partData = new MmsDataObject.MmsPart(cursor.getString(messageIdIdx),
                            cursor.getString(filenameIdx), fileSize, mimeType, file, fileIcon);
                }
                parts.add(partData);

            } while (cursor.moveToNext());
            return parts;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    public boolean setMessageDelivered(ContactId contact, String msgId, long timestampDelivered) {
        if (sLogger.isActivated()) {
            sLogger.debug("setMessageDelivered msgId=" + msgId + ", timestamp="
                    + timestampDelivered);
        }
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_STATE, State.DELIVERED.toInt());
        values.put(XmsData.KEY_REASON_CODE, XmsMessage.ReasonCode.UNSPECIFIED.toInt());
        values.put(XmsData.KEY_TIMESTAMP_DELIVERED, timestampDelivered);
        return mLocalContentResolver.update(XmsData.CONTENT_URI, values, SEL_XMS_CONTACT_MSGID,
                new String[] {
                        contact.toString(), msgId
                }) > 0;
    }

    public boolean setMessageSent(ContactId contact, String msgId, long timestampSent) {
        if (sLogger.isActivated()) {
            sLogger.debug("setMessageSent  msgId=" + msgId + ", timestamp=" + timestampSent);
        }
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_STATE, State.SENT.toInt());
        values.put(XmsData.KEY_REASON_CODE, XmsMessage.ReasonCode.UNSPECIFIED.toInt());
        values.put(XmsData.KEY_TIMESTAMP_SENT, timestampSent);
        return mLocalContentResolver.update(XmsData.CONTENT_URI, values, SEL_XMS_CONTACT_MSGID,
                new String[] {
                        contact.toString(), msgId
                }) > 0;
    }

    /**
     * Set state and reason code. Note that this method should not be used for State.DELIVERED and
     * State.SENT. These states require timestamps and should be set through setMessageDelivered and
     * setMessageSent respectively.
     *
     * @param contact the contact ID (i.e. the folder name)
     * @param msgId The message ID
     * @param state The state
     * @param reasonCode The reason code
     * @return True if set is successful
     */
    public boolean setStateAndReasonCode(ContactId contact, String msgId, XmsMessage.State state,
            XmsMessage.ReasonCode reasonCode) {
        switch (state) {
            case DELIVERED:
            case SENT:
                throw new IllegalArgumentException("State that requires "
                        + "timestamp passed, use specific method taking timestamp"
                        + " to set state " + state);
            default:
        }
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_STATE, state.toInt());
        values.put(XmsData.KEY_REASON_CODE, reasonCode.toInt());
        return mLocalContentResolver.update(XmsData.CONTENT_URI, values, SEL_XMS_CONTACT_MSGID,
                new String[] {
                        contact.toString(), msgId
                }) > 0;
    }

    /**
     * Checks if message is persisted
     *
     * @param contact the contact ID (i.e. the folder name)
     * @param msgId the message ID
     * @return True if the message is persisted
     */
    public boolean isMessagePersisted(ContactId contact, String msgId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJ_MSGID,
                    SEL_XMS_CONTACT_MSGID, new String[] {
                            contact.toString(), msgId
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            return cursor.moveToNext();

        } finally {
            CursorUtil.close(cursor);
        }
    }

    public int getCountXms(String msgId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJ_MSGID_CONTACT,
                    SEL_XMS_MSGID, new String[] {
                        msgId
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            return cursor.getCount();

        } finally {
            CursorUtil.close(cursor);
        }
    }

    public boolean updateSmsMessageId(ContactId contact, String msgId, String newMsgId) {
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_MESSAGE_ID, newMsgId);
        return mLocalContentResolver.update(XmsData.CONTENT_URI, values, SEL_XMS_CONTACT_MSGID,
                new String[] {
                        contact.toString(), msgId
                }) > 0;
    }

    private String getMmsMessageId(long nativeId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJ_MSGID,
                    SEL_XMS_MMS_NATIVE_ID, new String[] {
                        String.valueOf(nativeId)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            if (!cursor.moveToNext()) {
                return null;
            }
            int messageIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_MESSAGE_ID);
            return cursor.getString(messageIdIdx);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    public boolean updateMmsMessageId(ContactId contact, Long nativeId, String msgId) {
        String oldMsgId = getMmsMessageId(nativeId);
        if (oldMsgId == null) {
            if (sLogger.isActivated()) {
                sLogger.error("Failed to update message-id for MMS id=" + nativeId);
            }
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(PartData.KEY_MESSAGE_ID, msgId);
        mLocalContentResolver.update(PartData.CONTENT_URI, values, SEL_PART_MSID, new String[] {
            oldMsgId
        });
        values.clear();
        values.put(XmsData.KEY_MESSAGE_ID, msgId);
        return mLocalContentResolver.update(XmsData.CONTENT_URI, values, SEL_XMS_CONTACT_MSGID,
                new String[] {
                        contact.toString(), oldMsgId
                }) > 0;
    }
}
