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

package com.gsma.rcs.core.cms.xms;

import com.gsma.rcs.service.api.ExceptionUtil;
import com.gsma.rcs.utils.logger.Logger;

import com.klinker.android.send_message.Transaction;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;

/**
 * Process intent for received SMS
 */
public class SmsReceiver extends BroadcastReceiver {

    private final static Logger sLogger = Logger.getLogger(SmsReceiver.class.getName());

    public static final int MESSAGE_IS_NOT_READ = 0;
    public static final int MESSAGE_IS_NOT_SEEN = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action)) {
            sLogger.warn("Discard SMS receive intent: invalid action " + action);
            return;
        }
        try {
            SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if (msgs == null) {
                sLogger.warn("Discard SMS receive intent: invalid PDU");
                return;
            }
            for (SmsMessage sms : msgs) {
                String phoneNumber = sms.getDisplayOriginatingAddress();
                String body = sms.getDisplayMessageBody();
                if (sLogger.isActivated()) {
                    sLogger.debug("Received SMS from " + phoneNumber + ", body='" + body + "'");
                }
                ContentValues values = new ContentValues();
                values.put(Telephony.TextBasedSmsColumns.ADDRESS, phoneNumber);
                values.put(Telephony.TextBasedSmsColumns.DATE, sms.getTimestampMillis());
                values.put(Telephony.TextBasedSmsColumns.READ, MESSAGE_IS_NOT_READ);
                values.put(Telephony.TextBasedSmsColumns.STATUS, sms.getStatus());
                values.put(Telephony.TextBasedSmsColumns.TYPE,
                        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);
                values.put(Telephony.TextBasedSmsColumns.SEEN, MESSAGE_IS_NOT_SEEN);
                values.put(Telephony.TextBasedSmsColumns.BODY, body);
                values.put(Telephony.TextBasedSmsColumns.PROTOCOL, sms.getProtocolIdentifier());
                String subject = sms.getPseudoSubject();
                if (!TextUtils.isEmpty(subject)) {
                    values.put(Telephony.TextBasedSmsColumns.SUBJECT, subject);
                }
                Uri uri = context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI,
                        values);
                if (uri != null) {
                    // Notify user that SMS is received
                    Transaction.notifyXmsEvent(context, uri, Transaction.XmsEvent.SMS_RECEIVED);
                }
            }
        } catch (RuntimeException e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
        }
    }

}
