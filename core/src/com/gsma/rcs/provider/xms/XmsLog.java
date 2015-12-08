/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.provider.xms;

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * XMS log utilities
 */
public class XmsLog {

    private static final int FIRST_COLUMN_IDX = 0;

    private static final String SORT_BY_DATE_ASC = XmsData.KEY_TIMESTAMP + " ASC";

    private static final String SORT_BY_DATE_DESC = XmsData.KEY_TIMESTAMP + " DESC";

    private static final String SELECTION_XMS_CONTACT = XmsData.KEY_CONTACT + "=?";
    private static final String SELECTION_PART_CONTACT = PartData.KEY_CONTACT + "=?";
    private static final String SELECTION_XMS_MESSAGE_ID = XmsData.KEY_MESSAGE_ID + "=?";
    private static final String SELECTION_PART_MESSAGE_ID = PartData.KEY_MESSAGE_ID + "=?";

    private static final String SELECTION_XMS_MMS_ID = XmsData.KEY_MMS_ID + "=?";
    private static final String SELECTION_XMS_NATIVE_ID = XmsData.KEY_NATIVE_ID + "=?";
    private static final String SELECTION_XMS_MIME_TYPE = XmsData.KEY_MIME_TYPE + "=?";
    private static final String SELECTION_XMS_NATIVE_ID_MIME_TYPE = SELECTION_XMS_NATIVE_ID
            + " AND " + SELECTION_XMS_MIME_TYPE;

    private static final String SELECTION_UNREAD = XmsData.KEY_READ_STATUS + "="
            + ReadStatus.UNREAD.toInt();
    private static final String SELECTION_NATIVE_THREAD_ID = XmsData.KEY_NATIVE_THREAD_ID + "=?";
    private static final String SELECTION_NATIVE_THREAD_ID_UNREAD = SELECTION_NATIVE_THREAD_ID
            + " AND " + SELECTION_UNREAD;

    private static final String SELECTION_XMS_SMS = XmsData.KEY_MIME_TYPE + "='"
            + MimeType.TEXT_MESSAGE + "'";
    private static final String SELECTION_XMS_DIRECTION = XmsData.KEY_DIRECTION + "=?";
    private static final String SELECTION_XMS_CORRELATOR = XmsData.KEY_MESSAGE_CORRELATOR + "=?";
    private static final String SELECTION_XMS_CONTACT_DIRECTION_CORRELATOR = SELECTION_XMS_CONTACT
            + " AND " + SELECTION_XMS_DIRECTION + " AND " + SELECTION_XMS_CORRELATOR + " AND "
            + SELECTION_XMS_SMS;
    private static final Logger sLogger = Logger.getLogger(XmsLog.class.getSimpleName());

    private static final String SELECTION_QUEUED_MMS = XmsData.KEY_STATE + "="
            + State.QUEUED.toInt() + " AND " + XmsData.KEY_DIRECTION + "="
            + Direction.OUTGOING.toInt() + " AND " + XmsData.KEY_MIME_TYPE + "='"
            + MimeType.MULTIMEDIA_MESSAGE + "'" + " AND " + XmsData.KEY_NATIVE_ID + " IS NULL";

    private static final String SELECTION_BY_INTERRUPTED_MMS_TRANSFERS = XmsData.KEY_STATE + "='"
            + State.SENDING.toInt() + "'" + " AND " + XmsData.KEY_NATIVE_ID + " IS NULL";

    /**
     * Current instance
     */
    private static volatile XmsLog sInstance;
    private final LocalContentResolver mLocalContentResolver;

    /**
     * Constructor
     *
     * @param localContentResolver Local content resolver
     */
    private XmsLog(LocalContentResolver localContentResolver) {
        mLocalContentResolver = localContentResolver;
    }

    /**
     * Creates the instance of XmsLog singleton
     *
     * @param localContentResolver Local content resolver
     * @return the instance of XmsLog singleton
     */
    public static XmsLog createInstance(LocalContentResolver localContentResolver) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (XmsLog.class) {
            if (sInstance == null) {
                sInstance = new XmsLog(localContentResolver);
            }
        }
        return sInstance;
    }

    public static XmsLog getInstance() {
        return sInstance;
    }

    public Cursor getXmsMessage(String messageId) {
        Uri contentUri = Uri.withAppendedPath(XmsData.CONTENT_URI, messageId);
        Cursor cursor = mLocalContentResolver.query(contentUri, null, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        return cursor;
    }

    public String getContact(String xmsId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Get XMS contact for ".concat(xmsId));
        }
        Cursor cursor = getXmsData(XmsData.KEY_CONTACT, xmsId);
        if (cursor == null) {
            return null;
        }
        return getDataAsString(cursor);
    }

    public String getMimeType(String xmsId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Get XMS mime type for ".concat(xmsId));
        }
        Cursor cursor = getXmsData(XmsData.KEY_MIME_TYPE, xmsId);
        if (cursor == null) {
            return null;
        }
        return getDataAsString(cursor);
    }

    public Long getTimestamp(String xmsId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Get XMS timestamp for ".concat(xmsId));
        }
        Cursor cursor = getXmsData(XmsData.KEY_TIMESTAMP, xmsId);
        if (cursor == null) {
            return null;
        }
        return getDataAsLong(cursor);
    }

    private Cursor getXmsData(String columnName, String xmsId) {
        String[] projection = {
            columnName
        };
        Uri contentUri = Uri.withAppendedPath(XmsData.CONTENT_URI, xmsId);
        Cursor cursor = mLocalContentResolver.query(contentUri, projection, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        if (!cursor.moveToNext()) {
            CursorUtil.close(cursor);
            return null;
        }
        return cursor;
    }

    private Long getDataAsLong(Cursor cursor) {
        try {
            if (cursor.isNull(FIRST_COLUMN_IDX)) {
                return null;
            }
            return cursor.getLong(FIRST_COLUMN_IDX);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private String getDataAsString(Cursor cursor) {
        try {
            return cursor.getString(FIRST_COLUMN_IDX);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    public Long getSentTimestamp(String xmsId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Get XMS sent timestamp for ".concat(xmsId));
        }
        Cursor cursor = getXmsData(XmsData.KEY_TIMESTAMP_SENT, xmsId);
        if (cursor == null) {
            return null;
        }
        return getDataAsLong(cursor);
    }

    public XmsMessage.State getState(String xmsId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Get XMS state for ".concat(xmsId));
        }
        Cursor cursor = getXmsData(XmsData.KEY_STATE, xmsId);
        if (cursor == null) {
            return null;
        }
        return XmsMessage.State.valueOf(getDataAsInteger(cursor));
    }

    public RcsService.ReadStatus getReadStatus(String xmsId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Get XMS read status for ".concat(xmsId));
        }
        Cursor cursor = getXmsData(XmsData.KEY_READ_STATUS, xmsId);
        if (cursor == null) {
            return null;
        }
        return RcsService.ReadStatus.valueOf(getDataAsInteger(cursor));
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

    public XmsMessage.ReasonCode getReasonCode(String xmsId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Get XMS reason code for ".concat(xmsId));
        }
        Cursor cursor = getXmsData(XmsData.KEY_REASON_CODE, xmsId);
        if (cursor == null) {
            return null;
        }
        return XmsMessage.ReasonCode.valueOf(getDataAsInteger(cursor));
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
        values.put(XmsData.KEY_READ_STATUS, RcsService.ReadStatus.READ == sms.getReadStatus() ? 1
                : 0);
        values.put(XmsData.KEY_NATIVE_ID, sms.getNativeProviderId());
        values.put(XmsData.KEY_NATIVE_THREAD_ID, sms.getNativeThreadId());
        values.put(XmsData.KEY_MESSAGE_CORRELATOR, sms.getCorrelator());
        mLocalContentResolver.insert(XmsData.CONTENT_URI, values);
    }

    public void markMessageAsRead(String messageId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Marking message as read Id=" + messageId);
        }
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_READ_STATUS, RcsService.ReadStatus.READ.toInt());

        if (mLocalContentResolver.update(Uri.withAppendedPath(XmsData.CONTENT_URI, messageId),
                values, null, null) < 1) {
            if (sLogger.isActivated()) {
                sLogger.warn("There was no message with msgId '" + messageId + "' to mark as read.");
            }
        }
    }

    public void updateState(String messageId, State state) {
        if (sLogger.isActivated()) {
            sLogger.debug("Update message status for Id=" + messageId + ", state="
                    + state.toString());
        }
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_STATE, state.toInt());

        if (mLocalContentResolver.update(Uri.withAppendedPath(XmsData.CONTENT_URI, messageId),
                values, null, null) < 1) {
            if (sLogger.isActivated()) {
                sLogger.warn("There was no message with msgId '" + messageId);
            }
        }
    }

    public void markConversationAsRead(ContactId contactId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Marking message as read Id=" + contactId.toString());
        }
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_READ_STATUS, RcsService.ReadStatus.READ.toInt());

        if (mLocalContentResolver.update(XmsData.CONTENT_URI, values, SELECTION_XMS_CONTACT,
                new String[] {
                    contactId.toString()
                }) < 1) {
            if (sLogger.isActivated()) {
                sLogger.warn("There was no message with msgId '" + contactId.toString()
                        + "' to mark as read.");
            }
        }
    }

    public void addMms(MmsDataObject mms) {
        String contact = mms.getContact().toString();
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_MESSAGE_ID, mms.getMessageId());
        values.put(XmsData.KEY_CONTACT, contact);
        values.put(XmsData.KEY_CHAT_ID, contact);
        values.put(XmsData.KEY_CONTENT, mms.getSubject());
        values.put(XmsData.KEY_MIME_TYPE, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE);
        values.put(XmsData.KEY_DIRECTION, mms.getDirection().toInt());
        values.put(XmsData.KEY_TIMESTAMP, mms.getTimestamp());
        values.put(XmsData.KEY_TIMESTAMP_SENT, mms.getTimestampSent());
        values.put(XmsData.KEY_TIMESTAMP_DELIVERED, mms.getTimestampDelivered());
        values.put(XmsData.KEY_STATE, mms.getState().toInt());
        values.put(XmsData.KEY_REASON_CODE, mms.getReasonCode().toInt());
        values.put(XmsData.KEY_READ_STATUS, RcsService.ReadStatus.READ == mms.getReadStatus() ? 1
                : 0);
        values.put(XmsData.KEY_NATIVE_ID, mms.getNativeProviderId());
        values.put(XmsData.KEY_MMS_ID, mms.getMmsId());
        mLocalContentResolver.insert(XmsData.CONTENT_URI, values);

        for (MmsDataObject.MmsPart mmsPart : mms.getMmsPart()) {
            String mimeType = mmsPart.getMimeType();
            String content = mmsPart.getBody();
            if (content == null) {
                content = mmsPart.getFile().toString();
            }
            values.clear();
            values.put(PartData.KEY_MESSAGE_ID, mmsPart.getMessageId());
            values.put(PartData.KEY_CONTACT, contact);
            values.put(PartData.KEY_MIME_TYPE, mimeType);
            values.put(PartData.KEY_FILENAME, mmsPart.getFileName());
            values.put(PartData.KEY_FILESIZE, mmsPart.getFileSize());
            values.put(PartData.KEY_CONTENT, content);
            values.put(PartData.KEY_FILEICON, mmsPart.getFileIcon());
            mLocalContentResolver.insert(PartData.CONTENT_URI, values);
        }
    }

    /**
     * Delete all entries
     */
    public void deleteAllEntries() {
        mLocalContentResolver.delete(XmsData.CONTENT_URI, null, null);
        mLocalContentResolver.delete(PartData.CONTENT_URI, null, null);
    }

    public void deleteXmsMessages(ContactId contact) {
        int count = mLocalContentResolver.delete(XmsData.CONTENT_URI, SELECTION_XMS_CONTACT,
                new String[] {
                    contact.toString()
                });
        if (count > 0) {
            mLocalContentResolver.delete(PartData.CONTENT_URI, SELECTION_PART_CONTACT,
                    new String[] {
                        contact.toString()
                    });
        }
    }

    public void deleteXmsMessage(String messageId) {
        Cursor cursor = getXmsData(XmsData.KEY_MIME_TYPE, messageId);
        if (cursor == null) {
            return;
        }
        String mimeType = getDataAsString(cursor);
        mLocalContentResolver.delete(XmsData.CONTENT_URI, SELECTION_XMS_MESSAGE_ID, new String[] {
            messageId
        });
        if (XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)) {
            mLocalContentResolver.delete(PartData.CONTENT_URI, SELECTION_PART_MESSAGE_ID,
                    new String[] {
                        messageId
                    });
        }
    }

    public Cursor getXmsMessage(long nativeId, String mimeType) {
        Cursor cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                SELECTION_XMS_NATIVE_ID_MIME_TYPE, new String[] {
                        String.valueOf(nativeId), mimeType
                }, null);
        CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
        return cursor;
    }

    public Cursor getMmsMessage(String mmsId) {
        Cursor cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                SELECTION_XMS_MMS_ID, new String[] {
                    mmsId
                }, null);
        CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
        return cursor;
    }

    public Cursor getUnreadXmsMessages(Long nativeThreadId) {
        Cursor cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                SELECTION_NATIVE_THREAD_ID_UNREAD, new String[] {
                    String.valueOf(nativeThreadId)
                }, null);
        CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
        return cursor;
    }

    public Cursor getXmsMessages(Long nativeThreadId) {
        Cursor cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                SELECTION_NATIVE_THREAD_ID, new String[] {
                    String.valueOf(nativeThreadId)
                }, null);
        CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
        return cursor;
    }

    public Cursor getXmsMessages(ContactId contactId) {
        Cursor cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                SELECTION_XMS_CONTACT, new String[] {
                    contactId.toString()
                }, null);
        CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
        return cursor;
    }

    public Cursor getXmsMessages(String mimeType) {
        Cursor cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                SELECTION_XMS_MIME_TYPE, new String[] {
                    mimeType
                }, null);
        CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
        return cursor;
    }

    public List<String> getMessageIds(String contact, Direction direction, String correlator) {
        Cursor cursor = null;
        List<String> messageIds = new ArrayList<>();
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                    SELECTION_XMS_CONTACT_DIRECTION_CORRELATOR, new String[] {
                            contact, direction.toString(), correlator
                    }, SORT_BY_DATE_DESC);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            int messageIdIdx = cursor.getColumnIndex(XmsData.KEY_MESSAGE_ID);
            while (cursor.moveToNext()) {
                messageIds.add(cursor.getString(messageIdIdx));
            }
            return messageIds;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    public Cursor getMmsPart(String messageId) {
        Cursor cursor = mLocalContentResolver.query(PartData.CONTENT_URI, null,
                SELECTION_PART_MESSAGE_ID, new String[] {
                    messageId
                }, null);
        CursorUtil.assertCursorIsNotNull(cursor, PartData.CONTENT_URI);
        return cursor;
    }

    public Cursor getQueuedMms() {
        Cursor cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                SELECTION_QUEUED_MMS, null, SORT_BY_DATE_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
        return cursor;
    }

    public boolean setStateAndTimestamp(String messageId, State state,
            XmsMessage.ReasonCode reasonCode, long timestamp, long timestampSent) {
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_STATE, state.toInt());
        values.put(XmsData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(XmsData.KEY_TIMESTAMP, timestamp);
        values.put(XmsData.KEY_TIMESTAMP_SENT, timestampSent);
        return mLocalContentResolver.update(Uri.withAppendedPath(XmsData.CONTENT_URI, messageId),
                values, null, null) > 0;
    }

    public Set<MmsDataObject.MmsPart> getParts(String mmsId) {
        Cursor cursor = null;
        Set<MmsDataObject.MmsPart> parts = new HashSet<>();
        try {
            cursor = getMmsPart(mmsId);
            if (!cursor.moveToNext()) {
                return parts;
            }
            int messageIdIdx = cursor.getColumnIndexOrThrow(PartData.KEY_MESSAGE_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(PartData.KEY_CONTACT);
            int contentIdx = cursor.getColumnIndexOrThrow(PartData.KEY_CONTENT);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(PartData.KEY_MIME_TYPE);
            int filenameIdx = cursor.getColumnIndexOrThrow(PartData.KEY_FILENAME);
            int fileSizeIdx = cursor.getColumnIndexOrThrow(PartData.KEY_FILESIZE);
            int fileiconIdx = cursor.getColumnIndexOrThrow(PartData.KEY_FILEICON);
            do {
                String number = cursor.getString(contactIdx);
                ContactId contact = com.gsma.rcs.utils.ContactUtil
                        .createContactIdFromTrustedData(number);
                MmsDataObject.MmsPart partData = new MmsDataObject.MmsPart(
                        cursor.getString(messageIdIdx), contact, cursor.getString(mimeTypeIdx),
                        cursor.getString(filenameIdx), cursor.isNull(fileSizeIdx) ? null
                                : cursor.getLong(fileSizeIdx), cursor.getString(contentIdx),
                        cursor.getBlob(fileiconIdx));
                parts.add(partData);
            } while (cursor.moveToNext());
            return parts;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    public Cursor getInterruptedMmsTransfers() {
        Cursor cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,
                SELECTION_BY_INTERRUPTED_MMS_TRANSFERS, null, SORT_BY_DATE_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
        return cursor;
    }

    /**
     * Set state and reason code. Note that this method should not be used for State.DELIVERED and
     * State.DISPLAYED. These states require timestamps and should be set through
     * setMessageDelivered and setMessageDisplayed respectively. TODO
     *
     * @param messageId The message ID
     * @param state The state
     * @param reasonCode The reason code
     * @return True if set is successful
     */
    public boolean setStateAndReasonCode(String messageId, XmsMessage.State state,
            XmsMessage.ReasonCode reasonCode) {
        switch (state) {
            case DELIVERED:
            case DISPLAYED:
                throw new IllegalArgumentException("State that requires "
                        + "timestamp passed, use specific method taking timestamp"
                        + " to set state " + state);
            default:
        }
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_STATE, state.toInt());
        values.put(XmsData.KEY_REASON_CODE, reasonCode.toInt());
        return mLocalContentResolver.update(Uri.withAppendedPath(XmsData.CONTENT_URI, messageId),
                values, null, null) > 0;
    }
}
