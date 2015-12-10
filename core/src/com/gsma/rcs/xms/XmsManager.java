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

package com.gsma.rcs.xms;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony.TextBasedSmsColumns;
import android.telephony.SmsManager;

import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.ArrayList;

/**
 * Created by Philippe LEMORDANT on 12/11/2015.
 */
public class XmsManager {

    private final static Logger sLogger = Logger.getLogger(XmsManager.class.getSimpleName());
    private static final String SMS_SENT_ACTION = "SMS_SENT";
    private static final String SMS_DELIVERED_ACTION = "SMS_DELIVERED";
    private static final String KEY_TRANSACTION_ID = "trans_id";
    private static final String KEY_PART_ID = "part_id";
    private static final String KEY_SMS_URI = "sms_uri";
    private final ContentResolver mContentResolver;
    private final Context mCtx;
    private ReceiveSmsEventSent mReceiveSmsEventSent;
    private ReceiveSmsEventDelivered mReceiveSmsEventDelivered;

    /**
     * Constructor
     *
     * @param ctx The context
     * @param contentResolver The content resolver
     */
    public XmsManager(Context ctx, ContentResolver contentResolver) {
        mCtx = ctx;
        mContentResolver = contentResolver;
    }

    public void start() {
        /* Register the broadcast receiver to get SMS sent and delivery events */
        mReceiveSmsEventSent = new ReceiveSmsEventSent();
        mCtx.registerReceiver(mReceiveSmsEventSent, new IntentFilter(SMS_SENT_ACTION));
        mReceiveSmsEventDelivered = new ReceiveSmsEventDelivered();
        mCtx.registerReceiver(mReceiveSmsEventDelivered, new IntentFilter(SMS_DELIVERED_ACTION));
    }

    public void stop() {
        if (mReceiveSmsEventSent != null) {
            mCtx.unregisterReceiver(mReceiveSmsEventSent);
            mReceiveSmsEventSent = null;
        }
        if (mReceiveSmsEventDelivered != null) {
            mCtx.unregisterReceiver(mReceiveSmsEventDelivered);
            mReceiveSmsEventDelivered = null;
        }
    }

    public void sendSms(final ContactId contact, final String text) {
        SmsManager smsManager = SmsManager.getDefault();
        String transactionId = IdGenerator.generateMessageID();
        Intent smsSentIntent = new Intent(SMS_SENT_ACTION);
        smsSentIntent.putExtra(KEY_TRANSACTION_ID, transactionId);
        Intent smsDeliveredIntent = new Intent(SMS_DELIVERED_ACTION);
        smsDeliveredIntent.putExtra(KEY_TRANSACTION_ID, transactionId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            ContentValues values = new ContentValues();
            values.put("address", contact.toString());
            values.put("body", text);
            Uri smsUri = mContentResolver.insert(Uri.parse("content://sms/sent"), values);
            smsSentIntent.putExtra(KEY_SMS_URI, smsUri.toString());
            smsDeliveredIntent.putExtra(KEY_SMS_URI, smsUri.toString());
        }
        PendingIntent mSentPendingIntent = PendingIntent.getBroadcast(mCtx, 0, smsSentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent mDeliveredPendingIntent = PendingIntent.getBroadcast(mCtx, 0,
                smsDeliveredIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        ArrayList<String> parts = smsManager.divideMessage(text);

        /* Message too large for a single SMS, but may be sent as a multi-part SMS */
        if (parts.size() > 1) {
            ArrayList<PendingIntent> sentPIs = new ArrayList<>(parts.size());
            ArrayList<PendingIntent> deliveredPIs = new ArrayList<>(parts.size());
            for (int i = 0; i < parts.size(); i++) {
                smsSentIntent.putExtra(KEY_PART_ID, i);
                sentPIs.add(mSentPendingIntent);
                smsDeliveredIntent.putExtra(KEY_PART_ID, i);
                deliveredPIs.add(mDeliveredPendingIntent);
            }
            if (sLogger.isActivated()) {
                sLogger.debug("Sending split message of " + text.length() + " characters into "
                        + parts.size() + " parts.");
            }
            smsManager.sendMultipartTextMessage(contact.toString(), null, parts, sentPIs,
                    deliveredPIs);
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("sendTextMessage to " + contact + " text='" + text
                        + "' with transactionId=" + transactionId);
            }
            smsManager.sendTextMessage(contact.toString(), null, text, mSentPendingIntent,
                    mDeliveredPendingIntent);
        }

    }

    public void initialize() {
    }

    private class ReceiveSmsEventSent extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("ReceiveSmsEventSent: ignore intent with no extra data");
                }
                return;
            }
            String transId = intent.getStringExtra(KEY_TRANSACTION_ID);
            if (transId == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("ReceiveSmsEventSent: ignore intent with no transaction ID");
                }
                return;
            }
            int partIndex = intent.getIntExtra(KEY_PART_ID, -1);
            if (partIndex != -1) {
                if (sLogger.isActivated()) {
                    sLogger.debug("ReceiveSmsEventSent for transaction ID=" + transId
                            + " for part number " + partIndex);
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("ReceiveSmsEventSent for transaction ID=" + transId);
                }
            }
            int resultCode = getResultCode();
            switch (resultCode) {
                case Activity.RESULT_OK:
                    if (sLogger.isActivated()) {
                        sLogger.debug("SMS sent successfully");
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        String smsUri = intent.getStringExtra(KEY_SMS_URI);
                        ContentValues values = new ContentValues();
                        values.put(TextBasedSmsColumns.STATUS, TextBasedSmsColumns.STATUS_PENDING);
                        mContentResolver.update(Uri.parse(smsUri), values, null, null);
                    }
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    if (sLogger.isActivated()) {
                        sLogger.debug("Generic failure cause");
                    }
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    if (sLogger.isActivated()) {
                        sLogger.debug("Service is currently unavailable");
                    }
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    if (sLogger.isActivated()) {
                        sLogger.debug("No pdu provided");
                    }
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    if (sLogger.isActivated()) {
                        sLogger.debug("Radio was explicitly turned off");
                    }
                    break;
                default:
                    if (sLogger.isActivated()) {
                        sLogger.warn("Unknown result code=" + resultCode);
                    }
                    break;
            }
        }
    }

    private class ReceiveSmsEventDelivered extends ReceiveSmsEventSent {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("ReceiveSmsEventDelivered: ignore intent with no extra data");
                }
                return;
            }
            String transId = intent.getStringExtra(KEY_TRANSACTION_ID);
            if (transId == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("ReceiveSmsEventSent: ignore intent with no transaction ID");
                }
                return;
            }
            int partIndex = intent.getIntExtra(KEY_PART_ID, -1);
            if (partIndex != -1) {
                if (sLogger.isActivated()) {
                    sLogger.debug("ReceiveSmsEventDelivered for transaction ID=" + transId
                            + " for part number " + partIndex);
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("ReceiveSmsEventDelivered for transaction ID=" + transId);
                }
            }
            int resultCode = getResultCode();
            switch (resultCode) {
                case Activity.RESULT_OK:
                    if (sLogger.isActivated()) {
                        sLogger.debug("SMS delivered");
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        String smsUri = intent.getStringExtra(KEY_SMS_URI);
                        ContentValues values = new ContentValues();
                        values.put(TextBasedSmsColumns.STATUS, TextBasedSmsColumns.STATUS_COMPLETE);
                        mContentResolver.update(Uri.parse(smsUri), values, null, null);
                    }
                    break;
                case Activity.RESULT_CANCELED:
                    if (sLogger.isActivated()) {
                        sLogger.debug("SMS not delivered");
                    }
                    break;
                default:
                    if (sLogger.isActivated()) {
                        sLogger.warn("Unknown result code=" + resultCode);
                    }
                    break;
            }
        }
    }
}
