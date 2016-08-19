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
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

public class DeliveredReceiver extends BroadcastReceiver {

    private static final String SMS_DELIVERED = ".SMS_DELIVERED";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String packageName = context.getPackageName();
            String SMS_DELIVERED_ACTION = packageName.concat(SMS_DELIVERED);
            String action = intent.getAction();
            if (!SMS_DELIVERED_ACTION.equals(action)) {
                Log.e("delivery_receiver", "Discard SMS delivered event: invalid action " + action);
                return;
            }
            String messageUri = intent.getStringExtra("message_uri");
            if (TextUtils.isEmpty(messageUri)) {
                Log.e("delivery_receiver", "Discard SMS delivered event: cannot retrieve URI!");
                return;
            }
            Uri uri = Uri.parse(messageUri);
            if (uri == null || "".equals(uri.toString())) {
                Log.e("delivery_receiver", "Discard SMS delivered event: invalid URI " + uri);
                return;
            }
            int resultCode = getResultCode();
            switch (resultCode) {
                case Activity.RESULT_OK:
                    Log.d("delivery_receiver", "Marking SMS as delivered for URI=" + uri);
                    ContentValues values = new ContentValues();
                    values.put(Telephony.TextBasedSmsColumns.STATUS,
                            Telephony.TextBasedSmsColumns.STATUS_COMPLETE);
                    values.put(Telephony.TextBasedSmsColumns.DATE_SENT, System.currentTimeMillis());
                    values.put(Telephony.TextBasedSmsColumns.READ, 1);
                    context.getContentResolver().update(uri, values, null, null);
                    break;

                case Activity.RESULT_CANCELED:
                    Log.d("delivery_receiver", "Marking SMS as failed for URI=" + uri);
                    values = new ContentValues();
                    values.put(Telephony.TextBasedSmsColumns.STATUS,
                            Telephony.TextBasedSmsColumns.STATUS_FAILED);
                    values.put(Telephony.TextBasedSmsColumns.DATE_SENT, System.currentTimeMillis());
                    values.put(Telephony.TextBasedSmsColumns.READ, true);
                    values.put(Telephony.TextBasedSmsColumns.ERROR_CODE, resultCode);
                    context.getContentResolver().update(uri, values, null, null);
                    break;
            }
            // Notify user that SMS is delivered or failed
            Transaction.notifyXmsEvent(context, uri, Transaction.XmsEvent.SMS_DELIVERED_OR_FAILED);

        } catch (RuntimeException e) {
            Log.e("delivery_receiver", "Cannot mark SMS as delivered", e);
        }
    }
}
