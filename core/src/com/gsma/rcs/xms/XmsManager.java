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

import com.gsma.rcs.core.ims.service.cms.mms.MmsSessionListener;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.contact.ContactId;

import android.annotation.TargetApi;
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
import android.os.Parcelable;
import android.provider.Telephony.TextBasedSmsColumns;
import android.telephony.SmsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Philippe LEMORDANT on 12/11/2015.
 */
public class XmsManager {

    private final static Logger sLogger = Logger.getLogger(XmsManager.class.getSimpleName());

    private static final String MMS_SENT_ACTION = "MMS_SENT";
    private static final String SMS_SENT_ACTION = "SMS_SENT";
    private static final String SMS_DELIVERED_ACTION = "SMS_DELIVERED";

    private static final String EXTRA_TRANSACTION_ID = "trans_id";
    private static final String EXTRA_MMS_ID = "mms_id";
    private static final String EXTRA_CONTACT = "contact";
    private static final String EXTRA_PART_ID = "part_id";
    private static final String EXTRA_SMS_URI = "sms_uri";

    private static final String MMS_FILE_PROVIDER_AUTH = "com.gsma.rcs.provider.xms.MmsFileProvider";

    private final ContentResolver mContentResolver;
    private final Context mCtx;
    private final Map<String, MmsSessionListener> mMmsCallbacks;

    private ReceiveSmsEventSent mReceiveSmsEventSent;
    private ReceiveSmsEventDelivered mReceiveSmsEventDelivered;
    private ReceiveMmsEventSent mReceiveMmsEventSent;

    /**
     * Constructor
     *
     * @param ctx The context
     * @param contentResolver The content resolver
     */
    public XmsManager(Context ctx, ContentResolver contentResolver) {
        mCtx = ctx;
        mContentResolver = contentResolver;
        mMmsCallbacks = new HashMap<>();
    }

    public void start() {
        /* Register the broadcast receiver to get SMS sent and delivery events */
        mReceiveSmsEventSent = new ReceiveSmsEventSent();
        mCtx.registerReceiver(mReceiveSmsEventSent, new IntentFilter(SMS_SENT_ACTION));
        mReceiveSmsEventDelivered = new ReceiveSmsEventDelivered();
        mCtx.registerReceiver(mReceiveSmsEventDelivered, new IntentFilter(SMS_DELIVERED_ACTION));
        /* Register the broadcast receiver to get MMS sent events */
        mReceiveMmsEventSent = new ReceiveMmsEventSent();
        mCtx.registerReceiver(mReceiveMmsEventSent, new IntentFilter(MMS_SENT_ACTION));
    }

    public void stop() {
        mMmsCallbacks.clear();
        if (mReceiveSmsEventSent != null) {
            mCtx.unregisterReceiver(mReceiveSmsEventSent);
            mReceiveSmsEventSent = null;
        }
        if (mReceiveSmsEventDelivered != null) {
            mCtx.unregisterReceiver(mReceiveSmsEventDelivered);
            mReceiveSmsEventDelivered = null;
        }
        if (mReceiveMmsEventSent != null) {
            mCtx.unregisterReceiver(mReceiveMmsEventSent);
            mReceiveMmsEventSent = null;
        }
    }

    private void persistMmsIntoLocalFile(File sendFile, byte[] content) throws IOException {
        FileOutputStream writer = null;
        try {
            writer = new FileOutputStream(sendFile);
            writer.write(content);

        } finally {
            // noinspection ThrowableResultOfMethodCallIgnored
            CloseableUtils.tryToClose(writer);
        }
    }

    private String getTemporaryMmsPduFile(String transactionId) {
        return "SendMms_" + transactionId + ".dat";
    }

    /**
     * Send MMS
     * 
     * @param messageId The message ID
     * @param contact The remote contact
     * @param pdu The PDU
     * @param callback The callback
     * @throws IOException
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void sendMms(String messageId, ContactId contact, byte[] pdu, MmsSessionListener callback)
            throws IOException {
        String transId = IdGenerator.generateMessageID();
        String fileName = getTemporaryMmsPduFile(transId);
        File sendFile = new File(mCtx.getCacheDir(), fileName);
        Uri writerUri = (new Uri.Builder()).authority(MMS_FILE_PROVIDER_AUTH).path(fileName)
                .scheme(ContentResolver.SCHEME_CONTENT).build();
        persistMmsIntoLocalFile(sendFile, pdu);
        Intent mmsSentIntent = new Intent(MMS_SENT_ACTION);
        mmsSentIntent.putExtra(EXTRA_TRANSACTION_ID, transId);
        mmsSentIntent.putExtra(EXTRA_MMS_ID, messageId);
        mmsSentIntent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        PendingIntent mSentPendingIntent = PendingIntent.getBroadcast(mCtx, 0, mmsSentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mMmsCallbacks.put(transId, callback);
        SmsManager.getDefault().sendMultimediaMessage(mCtx, writerUri, null, null,
                mSentPendingIntent);
        if (sLogger.isActivated()) {
            sLogger.debug("Send MMS mId=" + messageId + " tId=" + transId + " contact=" + contact);
        }
    }

    /**
     * Send SMS
     * 
     * @param contact The remote contact
     * @param text The body text
     */
    public void sendSms(final ContactId contact, final String text) {
        SmsManager smsManager = SmsManager.getDefault();
        String transactionId = IdGenerator.generateMessageID();
        Intent smsSentIntent = new Intent(SMS_SENT_ACTION);
        smsSentIntent.putExtra(EXTRA_TRANSACTION_ID, transactionId);
        Intent smsDeliveredIntent = new Intent(SMS_DELIVERED_ACTION);
        smsDeliveredIntent.putExtra(EXTRA_TRANSACTION_ID, transactionId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            /*
             * Insert SMS into the native SMS provider if release is before KitKat.
             */
            ContentValues values = new ContentValues();
            values.put("address", contact.toString());
            values.put("body", text);
            Uri smsUri = mContentResolver.insert(Uri.parse("content://sms/sent"), values);
            smsSentIntent.putExtra(EXTRA_SMS_URI, smsUri.toString());
            smsDeliveredIntent.putExtra(EXTRA_SMS_URI, smsUri.toString());
        }
        PendingIntent mSentPendingIntent = PendingIntent.getBroadcast(mCtx, 0, smsSentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent mDeliveredPendingIntent = PendingIntent.getBroadcast(mCtx, 0,
                smsDeliveredIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        ArrayList<String> parts = smsManager.divideMessage(text);

        /* Message too large for a single SMS, but may be sent as a multi-part SMS */
        if (parts.size() > 1) {
            ArrayList<PendingIntent> sentPIs = new ArrayList<>(parts.size());
            ArrayList<PendingIntent> deliveredPIs = new ArrayList<>(parts.size());
            for (int i = 0; i < parts.size(); i++) {
                smsSentIntent.putExtra(EXTRA_PART_ID, i);
                sentPIs.add(mSentPendingIntent);
                smsDeliveredIntent.putExtra(EXTRA_PART_ID, i);
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
            String transId = intent.getStringExtra(EXTRA_TRANSACTION_ID);
            if (transId == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("ReceiveSmsEventSent: ignore intent with no transaction ID");
                }
                return;
            }
            int partIndex = intent.getIntExtra(EXTRA_PART_ID, -1);
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
                        String smsUri = intent.getStringExtra(EXTRA_SMS_URI);
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
                        sLogger.debug("No PDU provided");
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
            String transId = intent.getStringExtra(EXTRA_TRANSACTION_ID);
            if (transId == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("ReceiveSmsEventSent: ignore intent with no transaction ID");
                }
                return;
            }
            int partIndex = intent.getIntExtra(EXTRA_PART_ID, -1);
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
                        String smsUri = intent.getStringExtra(EXTRA_SMS_URI);
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

    private class ReceiveMmsEventSent extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("ReceiveMmsEventSent: ignore intent with no extra data");
                }
                return;
            }
            String transId = intent.getStringExtra(EXTRA_TRANSACTION_ID);
            if (transId == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("ReceiveMmsEventSent: ignore intent with no transaction ID");
                }
                return;
            }
            String mmsId = intent.getStringExtra(EXTRA_MMS_ID);
            if (mmsId == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("ReceiveMmsEventSent: ignore intent with no MMS ID");
                }
                return;
            }
            ContactId contact = intent.getParcelableExtra(EXTRA_CONTACT);
            if (contact == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("ReceiveMmsEventSent: ignore intent with no contact");
                }
                return;
            }
            int resultCode = getResultCode();
            switch (resultCode) {
                case Activity.RESULT_OK:
                    MmsSessionListener callback = mMmsCallbacks.get(transId);
                    if (callback != null) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("MMS sent successfully message ID=" + mmsId
                                    + " to contact " + contact);
                        }
                        callback.onMmsTransferred(contact, mmsId);
                        mMmsCallbacks.remove(transId);
                    } else {
                        if (sLogger.isActivated()) {
                            sLogger.warn("Cannot notify MMS sent successfully message ID=" + mmsId
                                    + " to contact " + contact);
                        }
                    }
                    break;

                default:
                    if (sLogger.isActivated()) {
                        sLogger.debug("Unknown result code=" + resultCode + " for messageId="
                                + mmsId);
                    }
                    callback = mMmsCallbacks.get(transId);
                    if (callback != null) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("MMS sent failure message ID=" + mmsId + " to contact "
                                    + contact);
                        }
                        // TODO give detailed error
                        callback.onMmsTransferError(
                                XmsMessage.ReasonCode.FAILED_ERROR_GENERIC_FAILURE, contact, mmsId);
                        mMmsCallbacks.remove(transId);
                    } else {
                        if (sLogger.isActivated()) {
                            sLogger.warn("Cannot notify MMS sent failure message ID=" + mmsId
                                    + " to contact " + contact);
                        }
                    }
                    break;
            }
            String tempMmsPduFile = getTemporaryMmsPduFile(transId);
            File sentFile = new File(mCtx.getCacheDir(), tempMmsPduFile);
            if (sentFile.exists()) {
                // noinspection ResultOfMethodCallIgnored
                sentFile.delete();
            }
        }
    }
}
