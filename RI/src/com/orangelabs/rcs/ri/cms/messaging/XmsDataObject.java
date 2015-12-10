/*
 * ******************************************************************************
 *  * Software Name : RCS IMS Stack
 *  *
 *  * Copyright (C) 2010 France Telecom S.A.
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

package com.orangelabs.rcs.ri.cms.messaging;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.ri.utils.ContactUtil;

import android.content.Context;
import android.database.Cursor;

/**
 * XMS Data Object
 *
 * @author YPLO6403
 */
public class XmsDataObject {

    // @formatter:off
    private static final String[] PROJECTION = {
            XmsMessageLog.MIME_TYPE, XmsMessageLog.CONTENT, XmsMessageLog.CONTACT,
            XmsMessageLog.TIMESTAMP, XmsMessageLog.DIRECTION
    };
    // @formatter:on

    private static final String SELECTION = XmsMessageLog.MESSAGE_ID + "=?";
    private final String mMessageId;
    private final String mMimeType;
    private final ContactId mContact;
    private final long mTimestamp;
    private final String mContent;
    private final Direction mDirection;

    public XmsDataObject(String messageId, String content, String mimeType,
                         ContactId contact, long timestamp, Direction direction) {
        mMessageId = messageId;
        mMimeType = mimeType;
        mContact = contact;
        mTimestamp = timestamp;
        mContent = content;
        mDirection = direction;
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
            cursor = ctx.getContentResolver().query(XmsMessageLog.CONTENT_URI, PROJECTION,
                    SELECTION, new String[] {
                        messageId
                    }, null);
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
            return new XmsDataObject(messageId,content,mimeType,contact,timestamp, direction);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getMessageId() {
        return mMessageId;
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

    public Direction getDirection(){
        return mDirection;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("XmsDataObject{");
        sb.append("mMessageId='").append(mMessageId).append('\'');
        sb.append(", mMimeType='").append(mMimeType).append('\'');
        sb.append(", mContact=").append(mContact);
        sb.append(", mTimestamp=").append(mTimestamp);
        sb.append(", mContent='").append(mContent).append('\'');
        sb.append(", mDirection=").append(mDirection);
        sb.append('}');
        return sb.toString();
    }
}
