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
import com.gsma.rcs.core.cms.event.FileTransferEventHandler;
import com.gsma.rcs.core.cms.event.GroupChatEventHandler;
import com.gsma.rcs.core.cms.event.ImdnDeliveryReportHandler;
import com.gsma.rcs.core.cms.event.ImdnDeliveryReportListener;
import com.gsma.rcs.core.cms.event.MmsSessionHandler;
import com.gsma.rcs.core.cms.event.XmsEventHandler;
import com.gsma.rcs.core.cms.event.XmsMessageListener;
import com.gsma.rcs.core.cms.event.framework.EventFrameworkManager;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncScheduler;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncSchedulerTaskType;
import com.gsma.rcs.core.cms.xms.XmsSynchronizer;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.Set;

public class CmsManager implements XmsMessageListener {

    private final Context mCtx;
    private final ImsModule mImsModule;
    private final CmsLog mCmsLog;
    private final XmsLog mXmsLog;
    private final MessagingLog mMessagingLog;
    private final RcsSettings mRcsSettings;
    private XmsEventHandler mXmsEventHandler;
    private ChatEventHandler mChatEventHandler;
    private FileTransferEventHandler mFileTransferEventHandler;
    private GroupChatEventHandler mGroupChatEventHandler;
    private LocalStorage mLocalStorage;
    private EventFrameworkManager mEventFrameworkManager;
    private MmsSessionHandler mMmsSessionHandler;
    private CmsSyncScheduler mSyncScheduler;
    private ImdnDeliveryReportHandler mImdnDeliveryReportHandler;

    /**
     * Constructor of CmsManager
     *
     * @param ctx The context
     * @param imsModule The IMS module
     * @param cmsLog The IMAP log accessor
     * @param xmsLog The XMS log accessor
     * @param messagingLog The Messaging log accessor
     * @param rcsSettings The RCS settings accessor
     */
    public CmsManager(Context ctx, ImsModule imsModule, CmsLog cmsLog, XmsLog xmsLog,
            MessagingLog messagingLog, RcsSettings rcsSettings) {
        mCtx = ctx;
        mImsModule = imsModule;
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
     * @param xmsEventHandler XMS event handler
     */
    public void start(CmsServiceImpl cmsService, ChatServiceImpl chatService, FileTransferServiceImpl fileTransferService,
            XmsEventHandler xmsEventHandler) {
        // execute sync between providers in a dedicated thread
        new Thread(new XmsSynchronizer(mCtx.getContentResolver(), mRcsSettings, mXmsLog, mCmsLog))
                .start();

        // instantiate CmsEventHandler in charge of handling events from Cms
        CmsEventHandler cmsEventHandler = new CmsEventHandler(mCtx, mCmsLog, mXmsLog,
                mMessagingLog, chatService, fileTransferService, cmsService, mRcsSettings, mImsModule.getInstantMessagingService());

        // instantiate LocalStorage in charge of handling events relatives to IMAP sync
        mLocalStorage = new LocalStorage(mRcsSettings, mCmsLog, cmsEventHandler);

        // start scheduler for sync
        if (mRcsSettings.getMessageStoreUri() != null) {
            mSyncScheduler = new CmsSyncScheduler(mCtx, mRcsSettings, mLocalStorage, mCmsLog,
                    mXmsLog);
            mSyncScheduler.registerListener(CmsSyncSchedulerTaskType.SYNC_FOR_USER_ACTIVITY,
                    cmsService);
            mXmsEventHandler = xmsEventHandler;
            mXmsEventHandler.setCmsSyncScheduler(mSyncScheduler);
            mSyncScheduler.start();
            /*
             * instantiate EventReportingFrameworkManager in charge of Pushing messages and updating
             * flags on the message store.
             */
            mEventFrameworkManager = new EventFrameworkManager(mSyncScheduler,
                    mImsModule.getInstantMessagingService(), mCmsLog, mRcsSettings);
            mXmsEventHandler.setEventFrameworkManager(mEventFrameworkManager);
        }

        /*
         * Instantiate ImdnDeliveryReportHanlder
         */
        mImdnDeliveryReportHandler = new ImdnDeliveryReportHandler(mCmsLog);

        /*
         * instantiate ChatEventHandler in charge of handling events from ChatSession,read or
         * deletion of messages.
         */
        mChatEventHandler = new ChatEventHandler(mEventFrameworkManager, mCmsLog, mMessagingLog,
                mRcsSettings, mImdnDeliveryReportHandler);

        /*
         * instantiate GroupChatEventHandler in charge of handling events from ChatSession,read or
         * deletion of messages.
         */
        mGroupChatEventHandler = new GroupChatEventHandler(mEventFrameworkManager, mCmsLog,
                mMessagingLog, mRcsSettings, mImdnDeliveryReportHandler);


        mMmsSessionHandler = new MmsSessionHandler(mCmsLog, mXmsLog, mRcsSettings, mSyncScheduler);

        /*
         * instantiate FileTransferEventHandler in charge of handling events from FileSharingSession, read or
         * deletion of file transfer.
         */
        mFileTransferEventHandler = new FileTransferEventHandler(mEventFrameworkManager, mCmsLog,
                mMessagingLog, mRcsSettings, mImdnDeliveryReportHandler);
    }

    /**
     * Stops the CmsManager
     */
    public void stop() throws PayloadException {
        if (mSyncScheduler != null) {
            mSyncScheduler.stop();
            mSyncScheduler = null;
        }
        mLocalStorage = null;
        mXmsEventHandler = null;
    }

    @Override
    public void onReadXmsMessage(String messageId) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onReadXmsMessage(messageId);
        }
    }

    @Override
    public void onDeleteXmsMessage(ContactId contact, Set<String> messageIds) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onDeleteXmsMessage(contact, messageIds);
        }
    }

    @Override
    public void onReadXmsConversation(ContactId contact) {
        if (mXmsEventHandler != null) {
            mXmsEventHandler.onReadXmsConversation(contact);
        }
    }

    public ChatEventHandler getChatEventHandler() {
        return mChatEventHandler;
    }

    public ImdnDeliveryReportListener getImdnDeliveryReportListener() {
        return mImdnDeliveryReportHandler;
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

    public FileTransferEventHandler getFileTransferEventHandler() {
        return mFileTransferEventHandler;
    }
}
