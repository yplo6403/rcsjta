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

package com.klinker.android.send_message;

import com.google.android.mms.pdu_alt.GenericPdu;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduParser;
import com.google.android.mms.pdu_alt.PduPersister;
import com.google.android.mms.pdu_alt.SendConf;
import com.google.android.mms.util_alt.SqliteWrapper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

public class MmsSentReceiver extends BroadcastReceiver {

    private static final String TAG = "MmsSentReceiver";

    public static final String MMS_SENT = "com.klinker.android.messaging.MMS_SENT";
    public static final String EXTRA_CONTENT_URI = "content_uri";
    public static final String EXTRA_FILE_PATH = "file_path";

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    String getMessageId(Intent sendRequestResponse) {
        final byte[] response = sendRequestResponse.getByteArrayExtra(SmsManager.EXTRA_MMS_DATA);
        if (response != null) {
            final GenericPdu pdu = new PduParser(response).parse();
            if (pdu instanceof SendConf) {
                final SendConf sendConf = (SendConf) pdu;
                int status = sendConf.getResponseStatus();
                if (PduHeaders.RESPONSE_STATUS_OK == status) {
                    String messageId = PduPersister.toIsoString(sendConf.getMessageId());
                    String transactionId = PduPersister.toIsoString(sendConf.getTransactionId());
                    Log.e(TAG, "MMS sent OK message-Id='" + messageId + "', transaction-ID='"
                            + transactionId + "'");
                    if (TextUtils.isEmpty(messageId)) {
                        return null;
                    }
                    return messageId;
                } else {
                    Log.e(TAG, "MMS sent, error=" + status);
                }
            } else {
                Log.e(TAG, "MMS sent, invalid response");
            }
        } else {
            Log.e(TAG, "MMS sent, empty response");
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
            if (!MMS_SENT.equals(action)) {
                Log.e(TAG, "Discard MMS sent event: invalid action " + action);
                return;
            }
            String messageUri = intent.getStringExtra(EXTRA_CONTENT_URI);
            if (TextUtils.isEmpty(messageUri)) {
                Log.e(TAG, "Discard MMS sent event: cannot retrieve URI!");
                return;
            }
            String messageId = getMessageId(intent);
            Uri uri = Uri.parse(messageUri);
            int resultCode = getResultCode();
            switch (resultCode) {
                case Activity.RESULT_OK:
                    Log.d(TAG, "MMS has finished sending, marking it as so for URI=" + uri);
                    ContentValues values = new ContentValues(1);
                    values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_SENT);
                    if (messageId != null) {
                        values.put(Telephony.Mms.MESSAGE_ID, messageId);
                    }
                    values.put(Telephony.Mms.DATE_SENT, System.currentTimeMillis());
                    SqliteWrapper.update(context, context.getContentResolver(), uri, values, null,
                            null);
                    break;

                case SmsManager.MMS_ERROR_UNSPECIFIED:
                case SmsManager.MMS_ERROR_INVALID_APN:
                case SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS:
                case SmsManager.MMS_ERROR_HTTP_FAILURE:
                case SmsManager.MMS_ERROR_IO_ERROR:
                case SmsManager.MMS_ERROR_RETRY:
                case SmsManager.MMS_ERROR_CONFIGURATION_ERROR:
                case SmsManager.MMS_ERROR_NO_DATA_NETWORK:
                    Log.d(TAG, "Marking SMS as failed for URI=" + uri + " error=" + resultCode);
                    values = new ContentValues();
                    values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_FAILED);
                    values.put(Telephony.TextBasedSmsColumns.TYPE,
                            Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED);
                    values.put(Telephony.TextBasedSmsColumns.ERROR_CODE, resultCode);
                    if (messageId != null) {
                        values.put(Telephony.Mms.MESSAGE_ID, messageId);
                    }
                    values.put(Telephony.Mms.DATE_SENT, System.currentTimeMillis());
                    context.getContentResolver().update(uri, values, null, null);
                    break;
            }
            // Notify user that MMS is sent or failed
            Transaction.notifyXmsEvent(context, uri, Transaction.XmsEvent.MMS_SENT_OR_FAILED);

            String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
            Log.v(TAG, filePath);
            new File(filePath).delete();

        } catch (RuntimeException e) {
            Log.e(TAG, "Cannot mark MMS as sent", e);
        }
    }
}
