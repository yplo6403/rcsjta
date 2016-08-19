/*
 * ******************************************************************************
 *  * Software Name : RCS IMS Stack
 *  *
 *  * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.cms.xms;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.event.XmsEventHandler;
import com.gsma.rcs.core.cms.xms.mms.MmsFileSizeException;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.smsmms.SmsMmsLog;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.service.api.ExceptionUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import com.klinker.android.send_message.ApnUtils;
import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.List;

/**
 * XMS Manager
 *
 * @author Philippe LEMORDANT
 */
public class XmsManager {

    private static final Logger sLogger = Logger.getLogger(XmsManager.class.getName());
    private static final String XMS_EVENT_ACTION = RcsServiceControl.RCS_STACK_PACKAGENAME
            .concat(Transaction.XMS_EVENT);

    private final Context mCtx;
    private final SmsMmsLog mSmsMmsLog;
    private final XmsLog mXmsLog;
    private ReceiveXmsEvent mReceiveXmsEvent;
    private MmsSettings mMmsSettings;
    private XmsEventHandler mXmsEventHandler;

    /**
     * Constructor
     *
     * @param ctx the context
     * @param xmsLog the XMS log accessor
     * @param smsMmsLog the SMS/MMS native log accessor
     */
    public XmsManager(Context ctx, XmsLog xmsLog, SmsMmsLog smsMmsLog) {
        mCtx = ctx;
        mXmsLog = xmsLog;
        mSmsMmsLog = smsMmsLog;
    }

    public void initialize(XmsEventHandler xmsEventHandler) {
        mXmsEventHandler = xmsEventHandler;
        mMmsSettings = MmsSettings.get(mCtx);
        PreferenceManager.getDefaultSharedPreferences(mCtx).edit()
                .putBoolean("system_mms_sending", true).commit();
        if (TextUtils.isEmpty(mMmsSettings.getMmsc())) {
            initApns();
        }
    }

    public void start() {
        // /* Register the broadcast receiver to get XMS events */
        mReceiveXmsEvent = new ReceiveXmsEvent();
        mCtx.registerReceiver(mReceiveXmsEvent, new IntentFilter(XMS_EVENT_ACTION));
    }

    private void initApns() {
        ApnUtils.initDefaultApns(mCtx, new ApnUtils.OnApnFinishedListener() {
            @Override
            public void onFinished() {
                mMmsSettings = MmsSettings.get(mCtx, true);
            }
        });
    }

    public void stop() {
        if (mReceiveXmsEvent != null) {
            mCtx.unregisterReceiver(mReceiveXmsEvent);
            mReceiveXmsEvent = null;
        }
    }

    /**
     * Sends a MMS
     *
     * @param contact the remote contact
     * @param files the file images to be sent
     * @param subject the subject
     * @param body the body text
     * @throws MmsFileSizeException
     * @throws FileAccessException
     */
    public void sendMultimediaMessage(ContactId contact, List<Uri> files, String subject,
            String body) throws MmsFileSizeException, FileAccessException {
        Settings sendSettings = new Settings();
        sendSettings.setMmsc(mMmsSettings.getMmsc());
        sendSettings.setProxy(mMmsSettings.getMmsProxy());
        sendSettings.setPort(mMmsSettings.getMmsPort());
        sendSettings.setUseSystemSending(true);
        Transaction transaction = new Transaction(mCtx, sendSettings);
        Message message = mSmsMmsLog.getMms(contact, files, subject, body);
        transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
    }

    /**
     * Sends a SMS
     *
     * @param contact the remote contact
     * @param text the body text
     */
    public void sendTextMessage(ContactId contact, String text) {
        Settings sendSettings = new Settings();
        sendSettings.setMmsc(mMmsSettings.getMmsc());
        sendSettings.setProxy(mMmsSettings.getMmsProxy());
        sendSettings.setPort(mMmsSettings.getMmsPort());
        sendSettings.setUseSystemSending(true);
        Transaction transaction = new Transaction(mCtx, sendSettings);
        Message message = new Message(text, new String[] {
            contact.toString()
        });
        transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
    }

    private class ReceiveXmsEvent extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (!XMS_EVENT_ACTION.equals(action)) {
                    sLogger.error("Discard XMS event: invalid action " + action);
                    return;
                }
                Transaction.XmsEvent event = (Transaction.XmsEvent) intent
                        .getSerializableExtra("event");
                Uri uri = intent.getParcelableExtra("message_uri");
                if (uri == null) {
                    sLogger.error("Discard XMS event (" + event + "): invalid URI");
                    return;
                }
                long ntpOffset = NtpTrustedTime.currentTimeMillis() - System.currentTimeMillis();
                switch (event) {
                    case SMS_RECEIVED:
                        SmsDataObject smsDataObject = mSmsMmsLog.getSmsFromNativeProvider(uri,
                                null, ntpOffset);
                        if (smsDataObject == null) {
                            if (sLogger.isActivated()) {
                                sLogger.error("Cannot insert SMS into XMS provider for URI=" + uri);
                            }
                            return;
                        }
                        mXmsEventHandler.onIncomingSms(smsDataObject);
                        break;

                    case SMS_INSERT:
                        smsDataObject = mSmsMmsLog.getSmsFromNativeProvider(uri, null, ntpOffset);
                        if (smsDataObject == null) {
                            if (sLogger.isActivated()) {
                                sLogger.error("Cannot insert SMS into XMS provider for URI=" + uri);
                            }
                            return;
                        }
                        mXmsEventHandler.onOutgoingSms(smsDataObject);
                        break;

                    case SMS_SENT_OR_FAILED:
                        smsDataObject = getSmsDataObjectFromProviders(uri, ntpOffset);
                        if (smsDataObject == null) {
                            if (sLogger.isActivated()) {
                                sLogger.error("Cannot mark SMS as sent into XMS provider for URI="
                                        + uri);
                            }
                            return;
                        }
                        mXmsEventHandler.onSmsMessageStateChanged(smsDataObject);
                        break;

                    case SMS_DELIVERED_OR_FAILED:
                        smsDataObject = getSmsDataObjectFromProviders(uri, ntpOffset);
                        if (smsDataObject == null) {
                            if (sLogger.isActivated()) {
                                sLogger.error("Cannot mark SMS as delivered into XMS provider for URI="
                                        + uri);
                            }
                            return;
                        }
                        mXmsEventHandler.onSmsMessageStateChanged(smsDataObject);
                        break;

                    case MMS_RECEIVED:
                        if (sLogger.isActivated()) {
                            sLogger.debug("Received MMS URI=" + uri);
                        }
                        long nativeId = ContentUris.parseId(uri);
                        List<MmsDataObject> mmsMessages = mSmsMmsLog.getMmsFromNativeProvider(
                                nativeId, ntpOffset);
                        for (MmsDataObject mms : mmsMessages) {
                            mXmsEventHandler.onIncomingMms(mms);
                        }
                        break;

                    case MMS_INSERT:
                        if (sLogger.isActivated()) {
                            sLogger.debug("New outgoing MMS uri=" + uri);
                        }
                        nativeId = ContentUris.parseId(uri);
                        List<MmsDataObject> mmsDataObjects = mSmsMmsLog.getMmsFromNativeProvider(
                                nativeId, ntpOffset);
                        if (mmsDataObjects == null || mmsDataObjects.isEmpty()) {
                            if (sLogger.isActivated()) {
                                sLogger.error("Cannot insert MMS into XMS provider for URI=" + uri);
                            }
                            return;
                        }
                        for (MmsDataObject mms : mmsDataObjects) {
                            mXmsEventHandler.onOutgoingMms(mms);
                        }
                        break;

                    case MMS_SENT_OR_FAILED:
                        nativeId = ContentUris.parseId(uri);
                        if (nativeId == -1) {
                            sLogger.error("Discard MMS event: invalid URI " + uri);
                            return;
                        }
                        if (sLogger.isActivated()) {
                            sLogger.debug("Sent or failed notification for outgoing MMS uri=" + uri);
                        }
                        mmsMessages = mSmsMmsLog.getMmsFromNativeProvider(nativeId, ntpOffset);
                        if (mmsMessages.isEmpty()) {
                            if (sLogger.isActivated()) {
                                sLogger.error("Cannot mark MMS as sent into XMS provider for URI="
                                        + uri);
                            }
                        } else {
                            for (MmsDataObject mms : mmsMessages) {
                                if (mms.getMessageId() == null) {
                                    if (sLogger.isActivated()) {
                                        sLogger.error("Cannot mark MMS as sent for URI=" + uri
                                                + " : Message-Id is null");
                                    }
                                } else {
                                    mXmsEventHandler.onMmsMessageStateChanged(mms);
                                }
                            }
                        }
                        break;
                }
            } catch (RuntimeException e) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));

            } catch (FileAccessException e) {
                sLogger.error("Failed to persist outgoing MMS "
                        + ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    private SmsDataObject getSmsDataObjectFromProviders(Uri uri, long ntpOffset) {
        long nativeId = ContentUris.parseId(uri);
        /*
         * Checks that SMS has already been inserted into XMS provider and retrieved its assigned
         * msgId.
         */
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getSmsMessage(nativeId);
            if (!cursor.moveToNext()) {
                return null;
            }
            String msgId = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID));
            if (msgId == null) {
                sLogger.debug("Cannot get SMS from XMS provider for URI= " + uri);
                return null;
            }
            return mSmsMmsLog.getSmsFromNativeProvider(uri, msgId, ntpOffset);

        } finally {
            CursorUtil.close(cursor);
        }
    }

}
