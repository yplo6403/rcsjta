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

package com.gsma.rcs.core.ims.service.cms;

import com.gsma.rcs.cms.CmsManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.sync.Synchronizer;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.UpdateMmsStateAfterUngracefulTerminationTask;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.ServerApiUtils;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.List;

/**
 * Created by Philippe LEMORDANT on 12/11/2015.
 */
public class CmsService extends ImsService {
    private static final String CMS_OPERATION_THREAD_NAME = "CmsOperations";
    private final static Logger sLogger = Logger.getLogger(CmsService.class.getSimpleName());
    private Handler mOperationHandler;
    private final XmsLog mXmsLog;
    private CmsServiceImpl mCmsServiceImpl;
    private ChatServiceImpl mChatServiceImpl;
    private final Core mCore;
    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private final CmsManager mCmsManager;

    /**
     * Constructor
     *
     * @param core The core service
     * @param parent IMS module
     * @param context The context
     * @param rcsSettings The RCS settings accessor
     * @param xmsLog The XMS log accessor
     * @param messagingLog The Chat log accessor
     * @param imapLog The Imap log accessor
     */
    public CmsService(Core core, ImsModule parent, Context context, RcsSettings rcsSettings,
            XmsLog xmsLog, MessagingLog messagingLog, ImapLog imapLog) {
        super(parent, true);
        mContext = context;
        mXmsLog = xmsLog;
        mRcsSettings = rcsSettings;
        mCore = core;
        mCmsManager = new CmsManager(context, imapLog, xmsLog, messagingLog, rcsSettings);
    }

    private Handler allocateBgHandler(String threadName) {
        HandlerThread thread = new HandlerThread(threadName);
        thread.start();
        return new Handler(thread.getLooper());
    }

    public void register(CmsServiceImpl service) {
        if (sLogger.isActivated()) {
            sLogger.debug(service.getClass().getName() + " registered ok.");
        }
        mCmsServiceImpl = service;
    }

    public void register(ChatServiceImpl chatService) {
        if (sLogger.isActivated()) {
            sLogger.debug(chatService.getClass().getName() + " registered ok.");
        }
        mChatServiceImpl = chatService;
    }

    @Override
    public void start() {
        if (isServiceStarted()) {
            return;
        }
        setServiceStarted(true);
        // TODO FGI : mOperationHandler is no more a final member, could be null!
        mOperationHandler = allocateBgHandler(CMS_OPERATION_THREAD_NAME);
        mCmsManager.start(mOperationHandler, mCmsServiceImpl.getXmsMessageBroadcaster(),
                mChatServiceImpl); // must be started before trying to dequeue MMS messages
        tryToDequeueMmsMessages();
    }

    @Override
    public void stop() throws PayloadException {
        if (!isServiceStarted()) {
            return;
        }
        setServiceStarted(false);
        mCmsManager.stop();
        mOperationHandler.getLooper().quit();
        mOperationHandler.getLooper().getThread().interrupt();
    }

    @Override
    public void check() {
    }

    public void scheduleImOperation(Runnable runnable) {
        mOperationHandler.post(runnable);
    }

    public void onCoreLayerStarted() {
        /* Update interrupted MMS transfer status */
        scheduleImOperation(new UpdateMmsStateAfterUngracefulTerminationTask(mXmsLog,
                mCmsServiceImpl));
    }

    public void syncAll() {
        mOperationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.debug("Synchronize CMS");
                }
                try {
                    new Synchronizer(mContext, mRcsSettings, mCmsManager).syncAll();
                    mCmsServiceImpl.broadcastAllSynchronized();

                } catch (ImapServiceNotAvailableException | NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.info("Failed to sync all conversations! error=" + e.getMessage());
                    }
                } catch (PayloadException | FileAccessException | RuntimeException e) {
                    sLogger.error("Failed to sync CMS", e);
                }
            }
        });
    }

    public void syncOneToOneConversation(final ContactId contact) {
        mOperationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.debug("Synchronize CMS for contact " + contact);
                }
                try {
                    new Synchronizer(mContext, mRcsSettings, mCmsManager).syncFolder(CmsUtils
                            .contactToCmsFolder(mRcsSettings, contact));
                    mCmsServiceImpl.broadcastOneToOneConversationSynchronized(contact);

                } catch (ImapServiceNotAvailableException e) {
                    if (sLogger.isActivated()) {
                        sLogger.info("Failed to sync CMS for contact " + contact
                                + ": IMAP service not available");
                    }
                } catch (PayloadException | NetworkException | FileAccessException
                        | RuntimeException e) {
                    sLogger.error("Failed to sync CMS for contact " + contact, e);
                }
            }
        });
    }

    public void syncGroupConversation(final String chatId) {
        // TODO
    }

    public void tryToDequeueMmsMessages() {
        mOperationHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean logActivated = sLogger.isActivated();
                    if (logActivated) {
                        sLogger.debug("Execute task to dequeue MMS");
                    }
                    if (!ServerApiUtils.isMmsConnectionAvailable(mContext)) {
                        if (logActivated) {
                            sLogger.debug("MMS mobile connection not available, exiting dequeue task to dequeue MMS");
                        }
                        return;
                    }
                    if (isShuttingDownOrStopped()) {
                        if (logActivated) {
                            sLogger.debug("Core service is shutting down/stopped, exiting MMS dequeue task");
                        }
                        return;
                    }
                    Cursor cursor = null;
                    String id;
                    ContactId contact;
                    String subject;
                    try {
                        cursor = mXmsLog.getQueuedMms();
                        int msgIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_MESSAGE_ID);
                        int contentIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTENT);
                        int contactIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
                        while (cursor.moveToNext()) {
                            id = cursor.getString(msgIdIdx);
                            String contactNumber = cursor.getString(contactIdx);
                            contact = ContactUtil.createContactIdFromTrustedData(contactNumber);
                            subject = cursor.getString(contentIdx);
                            if (logActivated) {
                                sLogger.debug("Dequeue MMS ID=" + id + " contact=" + contact
                                        + " subject=" + subject);
                            }
                            List<MmsDataObject.MmsPart> parts = mXmsLog.getParts(id);
                            mCmsServiceImpl.dequeueMmsMessage(id, contact, subject, parts);
                        }
                    } finally {
                        CursorUtil.close(cursor);
                    }
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to dequeue MMS!", e);
                }
            }
        });
    }

    /**
     * Is Core shutting down right now or already stopped
     *
     * @return boolean
     */
    private boolean isShuttingDownOrStopped() {
        return mCore.isStopping() || !mCore.isStarted();
    }

    public void markXmsMessageAsRead(final String messageId) {
        mCmsManager.onReadXmsMessage(messageId);
    }

    public void deleteXmsMessages() {
        mCmsManager.onDeleteAllXmsMessage();
    }

    public void deleteXmsMessages2(final ContactId contact) {
        mCmsManager.onDeleteXmsConversation(contact);
    }

    public void deleteXmsMessage(final String messageId) {
        mCmsManager.onDeleteXmsMessage(messageId);
    }

    public CmsManager getCmsManager() {
        return mCmsManager;
    }

    public boolean isAllowedToSync() {
        return mCmsManager.getImapServiceController().isSyncAvailable();
    }
}
