/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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

package com.gsma.rcs.cms;

import com.gsma.rcs.cms.event.ChatEventHandler;
import com.gsma.rcs.cms.event.CmsEventHandler;
import com.gsma.rcs.cms.event.GroupChatEventHandler;
import com.gsma.rcs.cms.event.MmsSessionHandler;
import com.gsma.rcs.cms.event.XmsEventHandler;
import com.gsma.rcs.cms.event.XmsMessageListener;
import com.gsma.rcs.cms.fordemo.ImapCommandController;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.observer.XmsObserver;
import com.gsma.rcs.cms.observer.XmsObserverListener;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.broadcaster.XmsMessageEventBroadcaster;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.os.Handler;

public class CmsManager implements XmsMessageListener {

    private final Context mContext;
    private final ImapLog mImapLog;
    private final XmsLog mXmsLog;
    private final MessagingLog mMessagingLog;
    private final RcsSettings mRcsSettings;
    private XmsObserver mXmsObserver;
    private XmsEventHandler mXmsEventHandler;
    private ChatEventHandler mChatEventHandler;
    private GroupChatEventHandler mGroupChatEventHandler;
    private LocalStorage mLocalStorage;
    private ImapCommandController mImapCommandController;
    private ImapServiceController mImapServiceController;
    private MmsSessionHandler mMmsSessionHandler;

    /**
     * Constructor of CmsManager
     *
     * @param context The context
     * @param imapLog The IMAP log accessor
     * @param xmsLog The XMS log accessor
     * @param messagingLog The Messaging log accessor
     * @param rcsSettings THE RCS settings accessor
     */
    public CmsManager(Context context, ImapLog imapLog, XmsLog xmsLog, MessagingLog messagingLog,
            RcsSettings rcsSettings) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mImapLog = imapLog;
        mXmsLog = xmsLog;
        mMessagingLog = messagingLog;
    }

    /**
     * Start the CmsManager
     * 
     * @param operationHandler the operation handler
     * @param xmsMessageEventBroadcaster the XMS event broadcaster
     * @param chatService the RCS chat service
     */
    public void start(Handler operationHandler,
            XmsMessageEventBroadcaster xmsMessageEventBroadcaster, ChatServiceImpl chatService) {
        // execute sync between providers in handler
        operationHandler.post(new ProviderSynchronizer(mContext.getContentResolver(), mRcsSettings,
                mXmsLog, mImapLog));

        // instantiate Xms Observer on native SMS/MMS content provider
        mXmsObserver = new XmsObserver(mContext);

        // instantiate XmsEventHandler in charge of handling xms events from XmsObserver
        mXmsEventHandler = new XmsEventHandler(mImapLog, mXmsLog, mRcsSettings,
                xmsMessageEventBroadcaster);
        mXmsObserver.registerListener(mXmsEventHandler);

        // instantiate CmsEventHandler in charge of handling events from Cms
        CmsEventHandler cmsEventHandler = new CmsEventHandler(mContext, mImapLog, mXmsLog,
                mMessagingLog, chatService, mRcsSettings, xmsMessageEventBroadcaster);

        // instantiate ChatEventHandler in charge of handling events from ChatSession,read or
        // deletion of messages
        mChatEventHandler = new ChatEventHandler(mImapLog, mMessagingLog, mRcsSettings);

        // instantiate GroupChatEventHandler in charge of handling events from ChatSession,read or
        // deletion of messages
        mGroupChatEventHandler = new GroupChatEventHandler(mImapLog, mMessagingLog, mRcsSettings);

        // instantiate LocalStorage in charge of handling events relatives to IMAP sync
        mLocalStorage = new LocalStorage(mImapLog, cmsEventHandler);

        // handle imap connection
        mImapServiceController = new ImapServiceController(mRcsSettings);
        mImapServiceController.start();

        // instantiate ImapCommandController in charge of Pushing messages and updating flags with
        // Imap command
        mImapCommandController = new ImapCommandController(operationHandler, mContext,
                mRcsSettings, mImapLog, mXmsLog, mImapServiceController);
        mXmsObserver.registerListener(mImapCommandController);

        mMmsSessionHandler = new MmsSessionHandler(mImapLog, mXmsLog, mRcsSettings,
                mImapCommandController);

        // start content observer on native SMS/MMS content provider
        mXmsObserver.start();

    }

    /**
     * Stop the CmsManager
     */
    public void stop() {
        if (mXmsObserver != null) {
            mXmsObserver.stop();
            mXmsObserver = null;
        }

        if (mImapServiceController != null) {
            mImapServiceController.stop();
            mImapServiceController = null;
        }

        mLocalStorage = null;
        mXmsEventHandler = null;
        mImapCommandController = null;
    }

    /**
     * Register a NativeXmsMessageListener
     * 
     * @param listener The listener
     */
    public void registerSmsObserverListener(XmsObserverListener listener) {
        if (mXmsObserver != null) {
            mXmsObserver.registerListener(listener);
        }
    }

    /**
     * Unregister the listener
     * 
     * @param listener The listener
     */
    public void unregisterSmsObserverListener(XmsObserverListener listener) {
        if (mXmsObserver != null) {
            mXmsObserver.unregisterListener(listener);
        }
    }

    @Override
    public void onReadXmsMessage(String messageId) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onReadXmsMessage(messageId);
        }
        if (mImapCommandController != null) {
            mImapCommandController.onReadXmsMessage(messageId);
        }
    }

    @Override
    public void onDeleteXmsMessage(String messageId) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onDeleteXmsMessage(messageId);
        }
        if (mImapCommandController != null) {
            mImapCommandController.onDeleteXmsMessage(messageId);
        }
    }

    @Override
    public void onReadXmsConversation(ContactId contact) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onReadXmsConversation(contact);
        }
        if (mImapCommandController != null) {
            mImapCommandController.onReadXmsConversation(contact);
        }
    }

    @Override
    public void onDeleteXmsConversation(ContactId contact) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onDeleteXmsConversation(contact);
        }
        if (mImapCommandController != null) {
            mImapCommandController.onDeleteXmsConversation(contact);
        }
    }

    @Override
    public void onXmsMessageStateChanged(ContactId contact, String messageId, String mimeType,
            State state) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onXmsMessageStateChanged(contact, messageId, mimeType, state);
        }
    }

    @Override
    public void onDeleteAllXmsMessage() {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onDeleteAllXmsMessage();
        }
        if (mImapCommandController != null) {
            mImapCommandController.onDeleteAllXmsMessage();
        }
    }

    public Context getContext() {
        return mContext;
    }

    public XmsLog getXmsLog() {
        return mXmsLog;
    }

    public LocalStorage getLocalStorage() {
        return mLocalStorage;
    }

    public ImapServiceController getImapServiceController() {
        return mImapServiceController;
    }

    public ChatEventHandler getChatEventHandler() {
        return mChatEventHandler;
    }

    public GroupChatEventHandler getGroupChatEventHandler() {
        return mGroupChatEventHandler;
    }

    public MmsSessionHandler getMmsSessionHandler() {
        return mMmsSessionHandler;
    }
}
