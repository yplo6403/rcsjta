/*
 * Copyright 2013 Jacob Klinker
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
 */

package com.klinker.android.send_message;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.*;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

public class SentReceiver extends BroadcastReceiver {

    private static final String TAG = "SentReceiver";
    private static final String SMS_SENT = ".SMS_SENT";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String packageName = context.getPackageName();
            String SMS_SENT_ACTION = packageName.concat(SMS_SENT);
            String action = intent.getAction();
            if (!SMS_SENT_ACTION.equals(action)) {
                Log.e(TAG, "Discard SMS sent event: invalid action " + action);
                return;
            }
            String messageUri = intent.getStringExtra("message_uri");
            if (TextUtils.isEmpty(messageUri)) {
                Log.e(TAG, "Discard SMS sent event: cannot retrieve URI!");
                return;
            }
            Uri uri = Uri.parse(messageUri);
            if (uri == null || "".equals(uri.toString())) {
                Log.e(TAG, "Discard SMS sent event: invalid URI " + uri);
                return;
            }
            int resultCode = getResultCode();
            switch (resultCode) {
                case Activity.RESULT_OK:
                    Log.d(TAG, "Marking SMS as sent for URI=" + uri);
                    ContentValues values = new ContentValues();
                    values.put(Telephony.TextBasedSmsColumns.TYPE,
                            Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT);
                    values.put(Telephony.TextBasedSmsColumns.READ, 1);
                    values.put(Telephony.TextBasedSmsColumns.DATE_SENT, System.currentTimeMillis());
                    context.getContentResolver().update(uri, values, null, null);
                    break;

                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                case SmsManager.RESULT_ERROR_NULL_PDU:
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Log.d(TAG, "Marking SMS as failed for URI=" + uri + " error=" + resultCode);
                    values = new ContentValues();
                    values.put(Telephony.TextBasedSmsColumns.TYPE,
                            Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED);
                    values.put(Telephony.TextBasedSmsColumns.READ, 1);
                    values.put(Telephony.TextBasedSmsColumns.ERROR_CODE, resultCode);
                    values.put(Telephony.TextBasedSmsColumns.DATE_SENT, System.currentTimeMillis());
                    context.getContentResolver().update(uri, values, null, null);
                    break;
            }
            // Notify user that SMS is sent or failed
            Transaction.notifyXmsEvent(context, uri, Transaction.XmsEvent.SMS_SENT_OR_FAILED);

        } catch (RuntimeException e) {
            Log.e(TAG, "Cannot mark SMS as sent", e);
        }
    }

}
