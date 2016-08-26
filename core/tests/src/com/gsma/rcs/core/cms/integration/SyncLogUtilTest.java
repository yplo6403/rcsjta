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

package com.gsma.rcs.core.cms.integration;

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfoLog;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class SyncLogUtilTest {

    private static final String SORT_BY_DATE_DESC = XmsData.KEY_TIMESTAMP + " DESC";

    private static final String SELECTION_XMS_CONTACT = XmsData.KEY_CONTACT + "=?" + " AND "
            + XmsData.KEY_MIME_TYPE + "=?";

    protected final LocalContentResolver mLocalContentResolver;

    /**
     * Current instance
     */
    private static volatile SyncLogUtilTest sInstance;

    private SyncLogUtilTest(Context context) {
        mLocalContentResolver = new LocalContentResolver(context.getContentResolver());
    }

    public static SyncLogUtilTest getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (SyncLogUtilTest.class) {
            if (sInstance == null) {
                sInstance = new SyncLogUtilTest(context);
            }
        }
        return sInstance;
    }

    public class GroupDeliveryInfo {
        public final String mChatId;
        public final ContactId mContact;
        public final com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo.Status mStatus;
        public final com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo.ReasonCode mReason;
        public final long mTimestampDelivered;
        public final long mTimestampDisplayed;

        public GroupDeliveryInfo(String chatId, ContactId contact,
                com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo.Status status,
                com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo.ReasonCode reason,
                long timestampDelivered, long timestampDisplayed) {
            mChatId = chatId;
            mContact = contact;
            mStatus = status;
            mReason = reason;
            mTimestampDelivered = timestampDelivered;
            mTimestampDisplayed = timestampDisplayed;
        }
    }

    public List<GroupDeliveryInfo> getGroupDeliveryInfo(String msgId) {
        List<GroupDeliveryInfo> result = new ArrayList<>();
        Uri contentUri = Uri.withAppendedPath(GroupDeliveryInfoLog.CONTENT_URI, msgId);
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(contentUri, null, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, contentUri);
            Integer columnChatId = cursor.getColumnIndexOrThrow(GroupDeliveryInfoLog.CHAT_ID);
            Integer columnContact = cursor.getColumnIndexOrThrow(GroupDeliveryInfoLog.CONTACT);
            Integer columnTimestampDeliver = cursor
                    .getColumnIndexOrThrow(GroupDeliveryInfoLog.TIMESTAMP_DELIVERED);
            Integer columnTimestampDisplay = cursor
                    .getColumnIndexOrThrow(GroupDeliveryInfoLog.TIMESTAMP_DISPLAYED);
            Integer columnStatus = cursor.getColumnIndexOrThrow(GroupDeliveryInfoLog.STATUS);
            Integer columnReason = cursor.getColumnIndexOrThrow(GroupDeliveryInfoLog.REASON_CODE);

            while (cursor.moveToNext()) {
                String chatId = cursor.getString(columnChatId);
                ContactId contact = ContactUtil.createContactIdFromTrustedData(cursor
                        .getString(columnContact));
                Long timestampDeliver = cursor.getLong(columnTimestampDeliver);
                com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo.Status status = com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo.Status
                        .valueOf(cursor.getInt(columnStatus));
                com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo.ReasonCode reason = com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo.ReasonCode
                        .valueOf(cursor.getInt(columnReason));
                Long timestampDisplay = cursor.getLong(columnTimestampDisplay);
                GroupDeliveryInfo item = new GroupDeliveryInfo(chatId, contact, status, reason,
                        timestampDeliver, timestampDisplay);
                result.add(item);
            }
            return result;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * @param contact the contact ID
     * @return the list of XmsDataObject ordered by timestamp
     */
    public List<XmsDataObject> getMessages(String mimeType, ContactId contact) {
        List<XmsDataObject> messages = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null, SELECTION_XMS_CONTACT,
                    new String[] {
                            contact.toString(), mimeType
                    }, SORT_BY_DATE_DESC);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            int messageIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_MESSAGE_ID);
            int nativeIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
            int contentIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTENT);
            int dateIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_TIMESTAMP);
            int directionIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DIRECTION);
            int readStatusIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_READ_STATUS);
            List<MmsPart> parts = new ArrayList<>();
            while (cursor.moveToNext()) {
                if (MimeType.TEXT_MESSAGE.equals(mimeType)) {
                    messages.add(new SmsDataObject(cursor.getString(messageIdIdx), ContactUtil
                            .createContactIdFromTrustedData(cursor.getString(contactIdx)), cursor
                            .getString(contentIdx), RcsService.Direction.valueOf(cursor
                            .getInt(directionIdx)),
                            ReadStatus.valueOf(cursor.getInt(readStatusIdx)), cursor
                                    .getLong(dateIdx), cursor.isNull(nativeIdIdx) ? null : cursor
                                    .getLong(nativeIdIdx)));
                } else {
                    messages.add(new MmsDataObject(cursor.getString(messageIdIdx), ContactUtil
                            .createContactIdFromTrustedData(cursor.getString(contactIdx)), cursor
                            .getString(contentIdx), RcsService.Direction.valueOf(cursor
                            .getInt(directionIdx)),
                            ReadStatus.valueOf(cursor.getInt(readStatusIdx)), cursor
                                    .getLong(dateIdx), cursor.isNull(nativeIdIdx) ? null : cursor
                                    .getLong(nativeIdIdx), parts));
                }
            }
            return messages;

        } finally {
            CursorUtil.close(cursor);
        }
    }
}
