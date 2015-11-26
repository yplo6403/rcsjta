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
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;

/**
 * XmsPersistedStorageAccessor helps in retrieving persisted data related to a XMS message from the
 * persisted storage. It can utilize caching for such data that will not be changed after creation
 * of the XMS message to speed up consecutive access.
 */
public class XmsPersistedStorageAccessor {

    private final String mXmsId;

    private final XmsLog mXmsLog;

    private ContactId mContact;

    private RcsService.Direction mDirection;

    private String mMimeType;

    private Long mTimestamp;

    private Long mTimestampSent;

    private Long mTimestampDelivered;

    private RcsService.ReadStatus mRead;

    private String mBody;

    private String mNativeId;

    private String mCorrelator;

    private String mMmsId;

    private String mChatId;

    public XmsPersistedStorageAccessor(XmsLog xmsLog, XmsDataObject sms) {
        this(xmsLog, sms.getMessageId());
        mContact = sms.getContact();
        mDirection = sms.getDirection();
        mMimeType = sms.getMimeType();
        mBody = sms.getBody();
        mTimestamp = sms.getTimestamp();
        mCorrelator = sms.getCorrelator();
        mChatId = sms.getChatId();
    }

    public XmsPersistedStorageAccessor(XmsLog xmsLog, String xmsId) {
        mXmsLog = xmsLog;
        mXmsId = xmsId;
    }

    public ContactId getRemoteContact() {
        /*
         * Utilizing cache here as contact can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mContact == null) {
            cacheData();
        }
        return mContact;
    }

    private void cacheData() {
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getXmsMessage(mXmsId);
            if (!cursor.moveToNext()) {
                throw new ServerApiPersistentStorageException("Data not found for XMS message "
                        + mXmsId);
            }
            if (mContact == null) {
                String contact = cursor
                        .getString(cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT));
                mContact = ContactUtil.createContactIdFromTrustedData(contact);
            }
            if (mDirection == null) {
                mDirection = RcsService.Direction.valueOf(cursor.getInt(cursor
                        .getColumnIndexOrThrow(XmsData.KEY_DIRECTION)));
            }
            if (mMimeType == null) {
                mMimeType = cursor.getString(cursor.getColumnIndexOrThrow(XmsData.KEY_MIME_TYPE));
            }
            if (mBody == null) {
                mBody = cursor.getString(cursor.getColumnIndexOrThrow(XmsData.KEY_BODY));
            }
            if (mTimestamp == null) {
                mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(XmsData.KEY_TIMESTAMP));
            }
            if (mTimestampSent == null || mTimestampSent == 0) {
                mTimestampSent = cursor.getLong(cursor
                        .getColumnIndexOrThrow(XmsData.KEY_TIMESTAMP_SENT));
            }
            if (mTimestampDelivered == null || mTimestampDelivered == 0) {
                mTimestampDelivered = cursor.getLong(cursor
                        .getColumnIndexOrThrow(XmsData.KEY_TIMESTAMP_DELIVERED));
            }
            if (RcsService.ReadStatus.READ != mRead) {
                mRead = RcsService.ReadStatus.valueOf(cursor.getInt(cursor
                        .getColumnIndexOrThrow(XmsData.KEY_READ_STATUS)));
            }
            if (mNativeId == null) {
                mNativeId = cursor.getString(cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_ID));
            }
            if (mCorrelator == null) {
                mCorrelator = cursor.getString(cursor
                        .getColumnIndexOrThrow(XmsData.KEY_MESSAGE_CORRELATOR));
            }
            if (mMmsId == null) {
                mMmsId = cursor.getString(cursor.getColumnIndexOrThrow(XmsData.KEY_MMS_ID));
            }
            if (mChatId == null) {
                mChatId = cursor.getString(cursor.getColumnIndexOrThrow(XmsData.KEY_CHAT_ID));
            }
        } finally {
            CursorUtil.close(cursor);
        }
    }

    public RcsService.Direction getDirection() {
        /*
         * Utilizing cache here as direction can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mDirection == null) {
            cacheData();
        }
        return mDirection;
    }

    public String getMimeType() {
        /*
         * Utilizing cache here as mime type can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mMimeType == null) {
            cacheData();
        }
        return mMimeType;
    }

    public String getBody() {
        /*
         * Utilizing cache here as mime type can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mBody == null) {
            cacheData();
        }
        return mBody;
    }

    public long getTimestamp() {
        /*
         * Utilizing cache here as timestamp type can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mTimestamp == null) {
            cacheData();
        }
        return mTimestamp;
    }

    public long getTimestampSent() {
        /*
         * Utilizing cache here as Timestamp sent can't be changed in persistent storage after it
         * has been set to some value bigger than zero, so no need to query for it multiple times.
         */
        if (mTimestampSent == null || mTimestampSent == 0) {
            cacheData();
        }
        return mTimestampSent;
    }

    public long getTimestampDelivered() {
        /*
         * Utilizing cache here as Timestamp delivered can't be changed in persistent storage after
         * it has been set to some value bigger than zero, so no need to query for it multiple
         * times.
         */
        if (mTimestampDelivered == null || mTimestampDelivered == 0) {
            cacheData();
        }
        return mTimestampDelivered;
    }

    public XmsMessage.State getState() {
        XmsMessage.State state = mXmsLog.getState(mXmsId);
        if (state == null) {
            throw new ServerApiPersistentStorageException("State not found for XMS ID " + mXmsId);
        }
        return state;
    }

    public XmsMessage.ReasonCode getReasonCode() {
        XmsMessage.ReasonCode reasonCode = mXmsLog.getReasonCode(mXmsId);
        if (reasonCode == null) {
            throw new ServerApiPersistentStorageException("Reason code not found for XMS ID "
                    + mXmsId);
        }
        return reasonCode;
    }

    public boolean isRead() {
        /*
         * No need to read from provider unless not already marked as read.
         */
        if (RcsService.ReadStatus.READ != mRead) {
            cacheData();
        }
        return RcsService.ReadStatus.READ == mRead;
    }

    public String getNativeId() {
        /*
         * Utilizing cache here as native ID can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mNativeId == null) {
            cacheData();
        }
        return mNativeId;
    }

    public String getMessageCorrelator() {
        /*
         * Utilizing cache here as message correlator can't be changed in persistent storage after
         * entry insertion anyway so no need to query for it multiple times.
         */
        if (mCorrelator == null) {
            cacheData();
        }
        return mCorrelator;
    }

    public String getMmsId() {
        /*
         * Utilizing cache here as mms_id can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mMmsId == null) {
            cacheData();
        }
        return mMmsId;
    }

    public String getChatId() {
        /*
         * Utilizing cache here as chatId can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mChatId == null) {
            cacheData();
        }
        return mChatId;
    }
}
