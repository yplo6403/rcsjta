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

package com.gsma.rcs.ri.cms.messaging;

import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.messaging.OneToOneTalkView;
import com.gsma.rcs.ri.messaging.TalkList;
import com.gsma.rcs.ri.messaging.chat.ChatPendingIntentManager;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.services.rcs.cms.XmsMessageIntent;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * XMS intent service
 *
 * @author Philippe LEMORDANT
 */
public class XmsIntentService extends IntentService {

    private static final String LOGTAG = LogUtils.getTag(XmsIntentService.class.getSimpleName());
    private ChatPendingIntentManager mChatPendingIntentManager;

    /**
     * Creates an IntentService.
     */
    public XmsIntentService() {
        super("XmsIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mChatPendingIntentManager = ChatPendingIntentManager.getChatPendingIntentManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        /* We want this service to stop running if forced stop so return not sticky. */
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action;
        if ((action = intent.getAction()) == null) {
            return;
        }
        String msgId = intent.getStringExtra(XmsMessageIntent.EXTRA_MESSAGE_ID);
        if (msgId == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read message ID");
            }
            return;
        }
        String mimeType = intent.getStringExtra(XmsMessageIntent.EXTRA_MIME_TYPE);
        if (mimeType == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read message mime-type");
            }
            return;
        }
        if (XmsMessageIntent.ACTION_NEW_XMS_MESSAGE.equals(action)) {
            handleNewXmsMessage(intent, msgId);
        }
    }

    private void handleNewXmsMessage(Intent messageIntent, String msgId) {
        /* Read message from provider */
        XmsDataObject xms = XmsDataObject.getXms(this, msgId);
        if (xms == null) {
            Log.e(LOGTAG, "Cannot find XMS message with ID=".concat(msgId));
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "XMS message ".concat(xms.toString()));
        }
        forwardXmsMessage2UI(messageIntent, xms);
    }

    private void forwardXmsMessage2UI(Intent messageIntent, XmsDataObject message) {
        ContactId contact = message.getContact();
        Intent intent = OneToOneTalkView.forgeIntentOnStackEvent(this, contact, messageIntent);
        Integer uniqueId = mChatPendingIntentManager.tryContinueChatConversation(intent, message
                .getContact().toString());
        if (uniqueId == null) {
            /* The conversation is already on foreground */
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "forwardXmsMessage2UI : conversation opened for contact " + contact);
            }
            return;
        }

        PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        String displayName = RcsContactUtil.getInstance(this).getDisplayName(contact);
        String title;
        if (XmsMessageLog.MimeType.TEXT_MESSAGE.equals(message.getMimeType())) {
            title = getString(R.string.title_recv_sms, displayName);
        } else {
            title = getString(R.string.title_recv_mms, displayName);
        }
        Notification notif = buildNotification(contentIntent, title, message.getContent());
        mChatPendingIntentManager.postNotification(uniqueId, notif);
        TalkList.notifyNewConversationEvent(this, XmsMessageIntent.ACTION_NEW_XMS_MESSAGE);
    }

    private Notification buildNotification(PendingIntent invitation, String title, String message) {
        NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
        notif.setContentIntent(invitation);
        notif.setSmallIcon(R.drawable.ri_notif_chat_icon);
        notif.setWhen(System.currentTimeMillis());
        notif.setAutoCancel(true);
        notif.setOnlyAlertOnce(true);
        notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        notif.setDefaults(Notification.DEFAULT_VIBRATE);
        notif.setContentTitle(title);
        if (message != null) { // TODO check if null
            notif.setContentText(message);
        }
        return notif.build();
    }
}
