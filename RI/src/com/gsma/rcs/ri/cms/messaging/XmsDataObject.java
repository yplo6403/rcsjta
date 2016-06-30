/*
 * ******************************************************************************
 *  * Software Name : RCS IMS Stack
 *  *
 *  * Copyright (C) 2010-2016 Orange.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package com.gsma.rcs.ri.cms.messaging;

import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.database.Cursor;

/**
 * XMS Data Object
 *
 * @author YPLO6403
 */
public class XmsDataObject {

    private static final String SELECTION = XmsMessageLog.MESSAGE_ID + "=?";
    private final String mMessageId;
    private final String mMimeType;
    private final ContactId mContact;
    private final long mTimestamp;
    private final String mContent;
    private final Direction mDirection;
    private XmsMessage.State mState;
    private XmsMessage.ReasonCode mReasonCode;
    private long mTimestampSent;
    private long mTimestampDelivered;
    private RcsService.ReadStatus mReadStatus;

    private XmsDataObject(String messageId, String content, String mimeType, ContactId contact,
            XmsMessage.State state, XmsMessage.ReasonCode reason, long timestamp,
            Direction direction, long timestampSent, long timestampDelivered,
            RcsService.ReadStatus readStatus) {
        mMessageId = messageId;
        mMimeType = mimeType;
        mContact = contact;
        mTimestamp = timestamp;
        mContent = content;
        mDirection = direction;
        mTimestampSent = timestampSent;
        mTimestampDelivered = timestampDelivered;
        mState = state;
        mReasonCode = reason;
        mReadStatus = readStatus;
    }

    /**
     * Gets instance of XMS message from XmsMessageLog provider
     *
     * @param ctx the context
     * @param messageId the message ID
     * @return instance or null if entry not found
     */
    public static XmsDataObject getXms(Context ctx, String messageId) {
        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(XmsMessageLog.CONTENT_URI, null, SELECTION,
                    new String[] {
                        messageId
                    }, null);
            if (cursor == null) {
                throw new IllegalStateException("Cannot query XMS with ID=" + messageId);
            }
            if (!cursor.moveToNext()) {
                return null;
            }
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.MIME_TYPE);
            int timestampIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.TIMESTAMP);
            int contentIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.CONTENT);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.CONTACT);
            int directionIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.DIRECTION);
            String mimeType = cursor.getString(mimeTypeIdx);
            String content = cursor.getString(contentIdx);
            String number = cursor.getString(contactIdx);
            ContactId contact = ContactUtil.formatContact(number);
            long timestamp = cursor.getLong(timestampIdx);
            Direction direction = Direction.valueOf(cursor.getInt(directionIdx));
            long timestampSent = cursor.getLong(cursor
                    .getColumnIndexOrThrow(XmsMessageLog.TIMESTAMP_SENT));
            long timestampDelivered = cursor.getLong(cursor
                    .getColumnIndexOrThrow(XmsMessageLog.TIMESTAMP_DELIVERED));
            XmsMessage.State state = XmsMessage.State.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(XmsMessageLog.STATE)));
            XmsMessage.ReasonCode reason = XmsMessage.ReasonCode.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(XmsMessageLog.REASON_CODE)));
            RcsService.ReadStatus readStatus = RcsService.ReadStatus.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(XmsMessageLog.READ_STATUS)));
            return new XmsDataObject(messageId, content, mimeType, contact, state, reason,
                    timestamp, direction, timestampSent, timestampDelivered, readStatus);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getMimeType() {
        return mMimeType;
    }

    public String getContent() {
        return mContent;
    }

    public ContactId getContact() {
        return mContact;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public Direction getDirection() {
        return mDirection;
    }

    @Override
    public String toString() {
        return "XmsDataObject{" + "mMessageId='" + mMessageId + '\'' + ", mMimeType='" + mMimeType
                + '\'' + ", mContact=" + mContact + ", mTimestamp=" + mTimestamp + ", mContent='"
                + mContent + '\'' + ", mDirection=" + mDirection + '}';
    }

    public XmsMessage.State getState() {
        return mState;
    }

    public XmsMessage.ReasonCode getReasonCode() {
        return mReasonCode;
    }

    public long getTimestampSent() {
        return mTimestampSent;
    }

    public long getTimestampDelivered() {
        return mTimestampDelivered;
    }

    public RcsService.ReadStatus getReadStatus() {
        return mReadStatus;
    }
}
