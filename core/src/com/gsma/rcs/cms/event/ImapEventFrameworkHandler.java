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

package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.observer.XmsObserverListener;
import com.gsma.rcs.cms.scheduler.CmsScheduler;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

/**
 * This class should be removed. Should be used for testing purpose only It creates SMS messages on
 * the CMS server with IMAP command.
 */
public class ImapEventFrameworkHandler implements XmsObserverListener, XmsMessageListener {

    private static final Logger sLogger = Logger.getLogger(ImapEventFrameworkHandler.class
            .getSimpleName());

    private final Context mContext;
    private final RcsSettings mSettings;
    private final CmsScheduler mScheduler;

    public ImapEventFrameworkHandler(Context context, CmsScheduler scheduler, RcsSettings settings) {
        mContext = context;
        mScheduler = scheduler;
        mSettings = settings;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onIncomingSms(SmsDataObject message) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("onIncomingSms, schedule push messages operation");
        }
        if (!mScheduler.schedulePushMessages(message.getContact())) {
            if (logActivated) {
                sLogger.info("--> can not schedule operation");
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onOutgoingSms(SmsDataObject message) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("onOutgoingSms, schedule push messages operation");
        }
        if (!mScheduler.schedulePushMessages(message.getContact())) {
            if (logActivated) {
                sLogger.info("--> can not schedule operation");
            }
        }
    }

    @Override
    public void onDeleteSmsFromNativeApp(long nativeProviderId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onDeleteNativeSms");
        }
        if (!mSettings.getMessageStoreUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onIncomingMms(MmsDataObject message) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("onIncomingMms, schedule push messages operation");
        }
        if (!mScheduler.schedulePushMessages(message.getContact())) {
            if (logActivated) {
                sLogger.info("--> can not schedule operation");
            }
        }
    }

    @Override
    public void onOutgoingMms(MmsDataObject message) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("onOutgoingMms, schedule push messages operation");
        }
        if (!mScheduler.schedulePushMessages(message.getContact())) {
            if (logActivated) {
                sLogger.info("--> can not schedule operation");
            }
        }
    }

    @Override
    public void onDeleteMmsFromNativeApp(String mmsId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onDeleteNativeMms");
        }
        if (!mSettings.getMessageStoreUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onXmsMessageStateChanged(Long nativeProviderId, String mimeType, State state) {
    }

    @Override
    public void onReadXmsConversationFromNativeApp(long nativeThreadId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onReadNativeConversation");
        }
        if (!mSettings.getMessageStoreUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteXmsConversationFromNativeApp(long nativeThreadId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onDeleteNativeConversation: " + nativeThreadId);
        }
        if (!mSettings.getMessageStoreUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @SuppressWarnings("unchecked")
    private void updateFlags() {
        if (!mScheduler.scheduleUpdateFlags()) {
            if (sLogger.isActivated()) {
                sLogger.info("--> can not schedule operation");
            }
        }
    }

    @Override
    public void onReadXmsMessage(String messageId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onReadXmsMessage: ".concat(messageId));
        }
        if (!mSettings.getMessageStoreUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onReadXmsConversation(ContactId contactId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onReadXmsConversation: ".concat(contactId.toString()));
        }

        if (!mSettings.getMessageStoreUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteXmsMessage(String messageId) {
        boolean isLogActivated = sLogger.isActivated();
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteXmsMessage: ".concat(messageId));
        }
        if (!mSettings.getMessageStoreUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteXmsConversation(ContactId contactId) {
        boolean isLogActivated = sLogger.isActivated();
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteXmsConversation:".concat(contactId.toString()));
        }
        if (!mSettings.getMessageStoreUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteAllXmsMessage() {
        boolean isLogActivated = sLogger.isActivated();
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteAllXmsMessage");
        }
        if (!mSettings.getMessageStoreUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }
}
