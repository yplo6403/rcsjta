/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.smsmms;

import static com.gsma.rcs.provider.CursorUtil.assertCursorIsNotNull;

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.telephony.SmsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to manage access to the native SMS provider.
 *
 * @author Philippe LEMORDANT
 */
public class SmsLog implements ISmsLog {

    private static final Logger sLogger = Logger.getLogger(SmsLog.class.getName());

    private final ContentResolver mContentResolver;

    public static class Sms {
        public final static Uri URI = Uri.parse("content://sms/");
    }

    /* package private */SmsLog(ContentResolver resolver) {
        mContentResolver = resolver;
    }

    private XmsMessage.State getSmsState(int type, int status) {
        switch (type) {
            case Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED:
                return XmsMessage.State.FAILED;

            case Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX:
                return XmsMessage.State.SENDING;

            case Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT:
                if (Telephony.TextBasedSmsColumns.STATUS_COMPLETE == status) {
                    return XmsMessage.State.DELIVERED;
                }
                return XmsMessage.State.SENT;

            case Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX:
                return XmsMessage.State.RECEIVED;

            default:
                return XmsMessage.State.SENT;
        }
    }

    private XmsMessage.ReasonCode getSmsReasonCode(int status, int error) {
        if (Telephony.TextBasedSmsColumns.STATUS_FAILED != status) {
            return XmsMessage.ReasonCode.UNSPECIFIED;
        }
        switch (error) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                return XmsMessage.ReasonCode.FAILED_ERROR_GENERIC_FAILURE;

            case SmsManager.RESULT_ERROR_NO_SERVICE:
                return XmsMessage.ReasonCode.FAILED_ERROR_NO_SERVICE;

            case SmsManager.RESULT_ERROR_RADIO_OFF:
                return XmsMessage.ReasonCode.FAILED_ERROR_RADIO_OFF;

            case SmsManager.RESULT_ERROR_NULL_PDU:
                return XmsMessage.ReasonCode.FAILED_ERROR_NULL_PDU;

            default:
                return XmsMessage.ReasonCode.FAILED_ERROR_GENERIC_FAILURE;
        }
    }

    @Override
    public List<SmsDataObject> getSmsFromNativeProvider(Cursor cursor, String messageId,
            long ntpLocalOffset) {
        List<SmsDataObject> smsObjects = new ArrayList<>();
        if (!cursor.moveToFirst()) {
            return smsObjects;
        }
        if (messageId == null) {
            messageId = IdGenerator.generateMessageID();
        }
        int idIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        int addressIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.ADDRESS);
        int dateIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.DATE);
        int dateSentIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.DATE_SENT);
        int bodyIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.BODY);
        int readIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.READ);
        int typeIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.TYPE);
        int statusIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.STATUS);
        int errorCodeIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.ERROR_CODE);
        do {
            Long _id = cursor.getLong(idIdx);
            String address = cursor.getString(addressIdx);
            ContactUtil.PhoneNumber phoneNumber = ContactUtil
                    .getValidPhoneNumberFromAndroid(address);
            if (phoneNumber == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("Cannot read contact from SMS ID=".concat(_id.toString()));
                }
                continue;
            }
            ContactId contactId = ContactUtil.createContactIdFromValidatedData(phoneNumber);
            long date = cursor.getLong(dateIdx) + ntpLocalOffset;
            long date_sent = cursor.getLong(dateSentIdx);
            String body = cursor.getString(bodyIdx);
            int read = cursor.getInt(readIdx);
            int type = cursor.getInt(typeIdx);
            int status = cursor.getInt(statusIdx);
            RcsService.Direction direction = RcsService.Direction.OUTGOING;
            if (Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX == type) {
                direction = RcsService.Direction.INCOMING;
            }
            RcsService.ReadStatus readStatus = RcsService.ReadStatus.READ;
            if (RcsService.Direction.INCOMING == direction && read == 0) {
                // Only incoming messages can be unread
                readStatus = RcsService.ReadStatus.UNREAD;
            }
            SmsDataObject smsDataObject = new SmsDataObject(messageId, contactId, body, direction,
                    readStatus, date, _id);
            if (date_sent != 0) {
                smsDataObject.setTimestampSent(date_sent + ntpLocalOffset);
            }
            if (RcsService.Direction.INCOMING == direction) {
                XmsMessage.State state = (readStatus == RcsService.ReadStatus.READ ? XmsMessage.State.DISPLAYED
                        : XmsMessage.State.RECEIVED);
                smsDataObject.setState(state);
            } else {
                smsDataObject.setState(getSmsState(type, status));
                int errorCode = cursor.getInt(errorCodeIdx);
                smsDataObject.setReasonCode(getSmsReasonCode(status, errorCode));
            }
            smsObjects.add(smsDataObject);

        } while (cursor.moveToNext());
        return smsObjects;
    }

    @Override
    public SmsDataObject getSmsFromNativeProvider(Uri uri, String messageId, long ntpLocalOffset) {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, null, null, null, null);
            assertCursorIsNotNull(cursor, uri);
            if (!cursor.moveToNext()) {
                return null;
            }
            List<SmsDataObject> smsObjects = getSmsFromNativeProvider(cursor, messageId,
                    ntpLocalOffset);
            if (smsObjects.isEmpty()) {
                return null;
            }
            return smsObjects.get(0);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public int deleteSms(long nativeId) {
        return mContentResolver.delete(ContentUris.withAppendedId(Sms.URI, nativeId), null, null);
    }

}
