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

package com.gsma.rcs.cms;

import com.gsma.rcs.cms.event.ChatEventHandler;
import com.gsma.rcs.cms.event.CmsEventHandler;
import com.gsma.rcs.cms.event.GroupChatEventHandler;
import com.gsma.rcs.cms.event.ImapEventFrameworkHandler;
import com.gsma.rcs.cms.event.MmsSessionHandler;
import com.gsma.rcs.cms.event.XmsEventHandler;
import com.gsma.rcs.cms.event.XmsMessageListener;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.observer.XmsObserver;
import com.gsma.rcs.cms.observer.XmsObserverListener;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.broadcaster.XmsMessageEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
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
    private ImapEventFrameworkHandler mImapEventFrameworkHandler;
    private ImapServiceController mImapServiceController;
    private MmsSessionHandler mMmsSessionHandler;
    private final static Logger sLogger = Logger.getLogger(CmsManager.class.getSimpleName());

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

        // instantiate ImapEventFrameworkHandler in charge of Pushing messages and updating flags with
        // Imap command
        mImapEventFrameworkHandler = new ImapEventFrameworkHandler(operationHandler, mContext,
                mRcsSettings, mImapLog, mXmsLog, mImapServiceController);
        mXmsObserver.registerListener(mImapEventFrameworkHandler);

        mMmsSessionHandler = new MmsSessionHandler(mImapLog, mXmsLog, mRcsSettings,
                mImapEventFrameworkHandler);

        // start content observer on native SMS/MMS content provider
        mXmsObserver.start();

    }

    /**
     * Stop the CmsManager
     */
    public void stop() throws PayloadException {
        if (mXmsObserver != null) {
            mXmsObserver.stop();
            mXmsObserver = null;
        }

        if (mImapServiceController != null) {
            try {
                mImapServiceController.stop();

            } catch (NetworkException e) {
                if (sLogger.isActivated()) {
                    sLogger.warn("Failed to close IMAP controller ", e);
                }
            }
            mImapServiceController = null;
        }

        mLocalStorage = null;
        mXmsEventHandler = null;
        mImapEventFrameworkHandler = null;
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
        if (mImapEventFrameworkHandler != null) {
            mImapEventFrameworkHandler.onReadXmsMessage(messageId);
        }
    }

    @Override
    public void onDeleteXmsMessage(String messageId) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onDeleteXmsMessage(messageId);
        }
        if (mImapEventFrameworkHandler != null) {
            mImapEventFrameworkHandler.onDeleteXmsMessage(messageId);
        }
    }

    @Override
    public void onReadXmsConversation(ContactId contact) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onReadXmsConversation(contact);
        }
        if (mImapEventFrameworkHandler != null) {
            mImapEventFrameworkHandler.onReadXmsConversation(contact);
        }
    }

    @Override
    public void onDeleteXmsConversation(ContactId contact) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onDeleteXmsConversation(contact);
        }
        if (mImapEventFrameworkHandler != null) {
            mImapEventFrameworkHandler.onDeleteXmsConversation(contact);
        }
    }

    @Override
    public void onDeleteAllXmsMessage() {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onDeleteAllXmsMessage();
        }
        if (mImapEventFrameworkHandler != null) {
            mImapEventFrameworkHandler.onDeleteAllXmsMessage();
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
