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
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import java.util.ArrayList;

/**
 *
 */
public class XmsLog {

    private static final int FIRST_COLUMN_IDX = 0;
    private static final String SELECTION_XMS_CONTACT = XmsData.KEY_CONTACT + "=?";
    private static final String SELECTION_PART_CONTACT = PartData.KEY_CONTACT + "=?";
    private static final String SELECTION_XMS_MESSAGE_ID = XmsData.KEY_MESSAGE_ID + "=?";
    private static final String SELECTION_PART_MESSAGE_ID = PartData.KEY_MESSAGE_ID + "=?";

    private static final Logger sLogger = Logger.getLogger(XmsLog.class.getSimpleName());

    /**
     * Current instance
     */
    private static volatile XmsLog sInstance;
    private final LocalContentResolver mLocalContentResolver;
    private final ContentResolver mContentResolver;

    /**
     * Constructor
     *
     * @param contentResolver Content resolver
     * @param localContentResolver Local content resolver
     */
    private XmsLog(ContentResolver contentResolver, LocalContentResolver localContentResolver) {
        mLocalContentResolver = localContentResolver;
        mContentResolver = contentResolver;
    }

    /**
     * Creates the instance of XmsLog singleton
     *
     * @param contentResolver Content resolver
     * @param localContentResolver Local content resolver
     * @return the instance of XmsLog singleton
     */
    public static XmsLog createInstance(ContentResolver contentResolver,
            LocalContentResolver localContentResolver) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (XmsLog.class) {
            if (sInstance == null) {
                sInstance = new XmsLog(contentResolver, localContentResolver);
            }
        }
        return sInstance;
    }

    public Cursor getXmsMessage(String messageId) {
        Uri contentUri = Uri.withAppendedPath(XmsData.CONTENT_URI, messageId);
        Cursor cursor = mLocalContentResolver.query(contentUri, null, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        return cursor;
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
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_MESSAGE_ID, sms.getMessageId());
        values.put(XmsData.KEY_CONTACT, sms.getContact().toString());
        values.put(XmsData.KEY_BODY, sms.getBody());
        values.put(XmsData.KEY_MIME_TYPE, XmsMessageLog.MimeType.TEXT_MESSAGE);
        values.put(XmsData.KEY_DIRECTION, sms.getDirection().toInt());
        values.put(XmsData.KEY_TIMESTAMP, sms.getTimestamp());
        values.put(XmsData.KEY_TIMESTAMP_SENT, sms.getTimestampSent());
        values.put(XmsData.KEY_TIMESTAMP_DELIVERED, sms.getTimestampDelivered());
        values.put(XmsData.KEY_STATE, sms.getReadStatus().toInt());
        values.put(XmsData.KEY_REASON_CODE, sms.getReasonCode().toInt());
        values.put(XmsData.KEY_READ_STATUS, RcsService.ReadStatus.READ == sms.getReadStatus() ? 1
                : 0);
        values.put(XmsData.KEY_NATIVE_ID, sms.getNativeProviderId());
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

    public void addMms(MmsDataObject mms) throws RemoteException, OperationApplicationException {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation
                .newInsert(XmsData.CONTENT_URI)
                .withValue(XmsData.KEY_MESSAGE_ID, mms.getMessageId())
                .withValue(XmsData.KEY_CONTACT, mms.getContact().toString())
                .withValue(XmsData.KEY_BODY, mms.getBody())
                .withValue(XmsData.KEY_MIME_TYPE, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE)
                .withValue(XmsData.KEY_DIRECTION, mms.getDirection().toInt())
                .withValue(XmsData.KEY_TIMESTAMP, mms.getTimestamp())
                .withValue(XmsData.KEY_TIMESTAMP_SENT, mms.getTimestampSent())
                .withValue(XmsData.KEY_TIMESTAMP_DELIVERED, mms.getTimestampDelivered())
                .withValue(XmsData.KEY_STATE, mms.getReadStatus().toInt())
                .withValue(XmsData.KEY_REASON_CODE, mms.getReasonCode().toInt())
                .withValue(XmsData.KEY_READ_STATUS,
                        RcsService.ReadStatus.READ == mms.getReadStatus() ? 1 : 0)
                .withValue(XmsData.KEY_NATIVE_ID, mms.getNativeProviderId())
                .withValue(XmsData.KEY_MMS_ID, mms.getMmsId()).build());
        for (MmsDataObject.MmsPart mmsPart : mms.getMmsPart()) {
            ops.add(ContentProviderOperation.newInsert(PartData.CONTENT_URI)
                    .withValue(PartData.KEY_MESSAGE_ID, mmsPart.getMessageId())
                    .withValue(PartData.KEY_CONTACT, mmsPart.getContact().toString())
                    .withValue(PartData.KEY_MIME_TYPE, mmsPart.getMimeType())
                    .withValue(PartData.KEY_FILENAME, mmsPart.getFileName())
                    .withValue(PartData.KEY_FILESIZE, mmsPart.getFileSize())
                    .withValue(PartData.KEY_CONTENT, mmsPart.getContent())
                    .withValue(PartData.KEY_FILEICON, mmsPart.getFileIcon()).build());
        }
        mContentResolver.applyBatch(XmsData.CONTENT_URI.getAuthority(), ops);
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
}
