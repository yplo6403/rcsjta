/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.cms.service;

import com.gsma.rcs.core.cms.event.ChatEventHandler;
import com.gsma.rcs.core.cms.event.CmsEventHandler;
import com.gsma.rcs.core.cms.event.GroupChatEventHandler;
import com.gsma.rcs.core.cms.event.MmsSessionHandler;
import com.gsma.rcs.core.cms.event.XmsEventHandler;
import com.gsma.rcs.core.cms.event.XmsMessageListener;
import com.gsma.rcs.core.cms.event.framework.EventFrameworkHandler;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncScheduler;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncSchedulerTaskType;
import com.gsma.rcs.core.cms.xms.XmsSynchronizer;
import com.gsma.rcs.core.cms.xms.observer.XmsObserver;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

public class CmsManager implements XmsMessageListener {

    private final Context mCtx;
    private final CmsLog mCmsLog;
    private final XmsLog mXmsLog;
    private final MessagingLog mMessagingLog;
    private final RcsSettings mRcsSettings;
    private XmsObserver mXmsObserver;
    private XmsEventHandler mXmsEventHandler;
    private ChatEventHandler mChatEventHandler;
    private GroupChatEventHandler mGroupChatEventHandler;
    private LocalStorage mLocalStorage;
    private EventFrameworkHandler mEventFrameworkHandler;
    private MmsSessionHandler mMmsSessionHandler;
    private CmsSyncScheduler mSyncScheduler;

    /**
     * Constructor of CmsManager
     *
     * @param ctx The context
     * @param cmsLog The IMAP log accessor
     * @param xmsLog The XMS log accessor
     * @param messagingLog The Messaging log accessor
     * @param rcsSettings The RCS settings accessor
     */
    public CmsManager(Context ctx, CmsLog cmsLog, XmsLog xmsLog, MessagingLog messagingLog,
            RcsSettings rcsSettings) {
        mCtx = ctx;
        mRcsSettings = rcsSettings;
        mCmsLog = cmsLog;
        mXmsLog = xmsLog;
        mMessagingLog = messagingLog;
    }

    /**
     * Starts the CmsManager
     *
     * @param cmsService the Cms service
     * @param chatService the RCS chat service
     */
    public void start(CmsServiceImpl cmsService, ChatServiceImpl chatService) {
        // execute sync between providers in a dedicated thread
        new Thread(new XmsSynchronizer(mCtx.getContentResolver(), mRcsSettings, mXmsLog, mCmsLog))
                .start();

        // instantiate Xms Observer on native SMS/MMS content provider
        mXmsObserver = new XmsObserver(mCtx);

        // instantiate XmsEventHandler in charge of handling xms events from XmsObserver
        mXmsEventHandler = new XmsEventHandler(mCmsLog, mXmsLog, mRcsSettings, cmsService);
        mXmsObserver.registerListener(mXmsEventHandler);

        // instantiate CmsEventHandler in charge of handling events from Cms
        CmsEventHandler cmsEventHandler = new CmsEventHandler(mCtx, mCmsLog, mXmsLog,
                mMessagingLog, chatService, cmsService, mRcsSettings);

        // instantiate LocalStorage in charge of handling events relatives to IMAP sync
        mLocalStorage = new LocalStorage(mCmsLog, cmsEventHandler);

        // start scheduler for sync
        if (mRcsSettings.getMessageStoreUri() != null) {
            mSyncScheduler = new CmsSyncScheduler(mCtx, mRcsSettings, mLocalStorage, mCmsLog, mXmsLog);
            mSyncScheduler.registerListener(CmsSyncSchedulerTaskType.SYNC_FOR_USER_ACTIVITY, cmsService);
            mSyncScheduler.start();

            /*
             * instantiate EventFrameworkHandler in charge of Pushing messages and updating flags on
             * the message store.
             */
            mEventFrameworkHandler = new EventFrameworkHandler(mCtx, mSyncScheduler, mRcsSettings);
            mXmsObserver.registerListener(mEventFrameworkHandler);
        }
        /*
         * instantiate ChatEventHandler in charge of handling events from ChatSession,read or
         * deletion of messages.
         */
        mChatEventHandler = new ChatEventHandler(mEventFrameworkHandler, mCmsLog, mMessagingLog,
                mRcsSettings);

        /*
         * instantiate GroupChatEventHandler in charge of handling events from ChatSession,read or
         * deletion of messages.
         */
        mGroupChatEventHandler = new GroupChatEventHandler(mCmsLog, mMessagingLog, mRcsSettings);

        mMmsSessionHandler = new MmsSessionHandler(mCmsLog, mXmsLog, mRcsSettings,
                mEventFrameworkHandler);

        // start content observer on native SMS/MMS content provider
        mXmsObserver.start();
    }

    /**
     * Stops the CmsManager
     */
    public void stop() throws PayloadException {
        if (mXmsObserver != null) {
            mXmsObserver.stop();
            mXmsObserver = null;
        }

        if (mSyncScheduler != null) {
            mSyncScheduler.stop();
            mSyncScheduler = null;
        }

        mLocalStorage = null;
        mXmsEventHandler = null;
        mEventFrameworkHandler = null;
    }

    @Override
    public void onReadXmsMessage(String messageId) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onReadXmsMessage(messageId);
        }
        if (mEventFrameworkHandler != null) {
            mEventFrameworkHandler.onReadXmsMessage(messageId);
        }
    }

    @Override
    public void onDeleteXmsMessage(String messageId) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onDeleteXmsMessage(messageId);
        }
        if (mEventFrameworkHandler != null) {
            mEventFrameworkHandler.onDeleteXmsMessage(messageId);
        }
    }

    @Override
    public void onReadXmsConversation(ContactId contact) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onReadXmsConversation(contact);
        }
        if (mEventFrameworkHandler != null) {
            mEventFrameworkHandler.onReadXmsConversation(contact);
        }
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

    public CmsSyncScheduler getSyncScheduler() {
        return mSyncScheduler;
    }

}
