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
 *
 ******************************************************************************/

package com.gsma.rcs.core.cms.utils;

import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.Telephony;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for SMS
 *
 * @author Philippe LEMORDANT
 */
public class SmsUtils {

    private static final Logger sLogger = Logger.getLogger(SmsUtils.class.getName());

    /**
     * Gets the SMS from the native provider
     *
     * @param cursor the cursor
     * @param ntpLocalOffset the time offset to apply
     * @return the list of SMS objects
     */
    public static List<SmsDataObject> getSmsFromNativeProvider(Cursor cursor, long ntpLocalOffset) {
        List<SmsDataObject> smsObjects = new ArrayList<>();
        if (!cursor.moveToFirst()) {
            return smsObjects;
        }
        int idIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        int threadIdIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.THREAD_ID);
        int addressIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.ADDRESS);
        int dateIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.DATE);
        int dateSentIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.DATE_SENT);
        int protocolIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.PROTOCOL);
        int bodyIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.BODY);
        int readIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.READ);
        int typeIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.TYPE);
        int statusIdx = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.STATUS);
        do {
            Long _id = cursor.getLong(idIdx);
            Long threadId = cursor.getLong(threadIdIdx);
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
            long date_sent = cursor.getLong(dateSentIdx) + ntpLocalOffset;
            String protocol = cursor.getString(protocolIdx);
            String body = cursor.getString(bodyIdx);
            int read = cursor.getInt(readIdx);
            int type = cursor.getInt(typeIdx);
            int status = cursor.getInt(statusIdx);
            RcsService.Direction direction = RcsService.Direction.OUTGOING;
            if (protocol != null) {
                direction = RcsService.Direction.INCOMING;
            }
            RcsService.ReadStatus readStatus = RcsService.ReadStatus.READ;
            if (RcsService.Direction.INCOMING == direction && read == 0) {
                // Only incoming messages can be unread
                readStatus = RcsService.ReadStatus.UNREAD;
            }
            SmsDataObject smsDataObject = new SmsDataObject(IdGenerator.generateMessageID(),
                    contactId, body, direction, readStatus, date, _id, threadId);
            smsDataObject.setTimestampDelivered(date_sent);
            if (RcsService.Direction.INCOMING == direction) {
                XmsMessage.State state = (readStatus == RcsService.ReadStatus.READ ? XmsMessage.State.DISPLAYED
                        : XmsMessage.State.RECEIVED);
                smsDataObject.setState(state);

            } else {
                if (type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED) {
                    smsDataObject.setState(XmsMessage.State.FAILED);
                } else if (status == Telephony.TextBasedSmsColumns.STATUS_COMPLETE) {
                    smsDataObject.setState(XmsMessage.State.DELIVERED);
                } else {
                    smsDataObject.setState(XmsMessage.State.SENT);
                }
            }
            smsObjects.add(smsDataObject);
        } while (cursor.moveToNext());
        return smsObjects;
    }
}
