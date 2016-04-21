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

package com.gsma.rcs.core.cms.service;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.cms.event.XmsEventHandler;
import com.gsma.rcs.core.cms.event.framework.TerminatingEventFrameworkSession;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncScheduler;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.UpdateMmsStateAfterUngracefulTerminationTask;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.service.api.ServerApiUtils;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.List;
import java.util.Set;

import javax2.sip.message.Response;

/**
 * Created by Philippe LEMORDANT on 12/11/2015.
 */
public class CmsService extends ImsService {
    private static final String CMS_OPERATION_THREAD_NAME = "CmsOperations";
    private final static Logger sLogger = Logger.getLogger(CmsService.class.getSimpleName());
    private final Handler mOperationHandler;
    private final XmsLog mXmsLog;
    private final CmsLog mCmsLog;
    private CmsServiceImpl mCmsServiceImpl;
    private FileTransferServiceImpl mFileTransferServiceImpl;
    private ChatServiceImpl mChatServiceImpl;
    private final Core mCore;
    private final Context mContext;
    private final CmsManager mCmsManager;
    private final RcsSettings mRcsSettings;
    private final ImsModule mImsModule;
    private final MessagingLog mMessagingLog;
    private XmsEventHandler mXmsEventHandler;

    /**
     * Constructor
     *
     * @param core The core service
     * @param parent IMS module
     * @param context The context
     * @param rcsSettings The RCS settings accessor
     * @param xmsLog The XMS log accessor
     * @param messagingLog The Chat log accessor
     * @param cmsLog The Imap log accessor
     */
    public CmsService(Core core, ImsModule parent, Context context, RcsSettings rcsSettings,
            XmsLog xmsLog, MessagingLog messagingLog, CmsLog cmsLog) {
        super(parent, true);
        mImsModule = parent;
        mContext = context;
        mRcsSettings = rcsSettings;
        mXmsLog = xmsLog;
        mMessagingLog = messagingLog;
        mCmsLog = cmsLog;
        mCore = core;
        mOperationHandler = allocateBgHandler(CMS_OPERATION_THREAD_NAME);
        mCmsManager = new CmsManager(context, mImsModule, mCmsLog, xmsLog, mMessagingLog,
                mRcsSettings);
    }

    private Handler allocateBgHandler(String threadName) {
        HandlerThread thread = new HandlerThread(threadName);
        thread.start();
        return new Handler(thread.getLooper());
    }

    public void register(CmsServiceImpl cmsService, ChatServiceImpl chatService,
            FileTransferServiceImpl fileTransferService) {
        if (sLogger.isActivated()) {
            sLogger.debug(cmsService.getClass().getName() + " registered ok.");
            sLogger.debug(chatService.getClass().getName() + " registered ok.");
            sLogger.debug(fileTransferService.getClass().getName() + " registered ok.");
        }
        mCmsServiceImpl = cmsService;
        mChatServiceImpl = chatService;
        mFileTransferServiceImpl = fileTransferService;
    }

    public void initialize(XmsEventHandler xmsEventHandler) {
        mXmsEventHandler = xmsEventHandler;
    }

    @Override
    public void start() {
        if (isServiceStarted()) {
            return;
        }
        setServiceStarted(true);
        mCmsManager.start(mCmsServiceImpl, mChatServiceImpl, mFileTransferServiceImpl,
                mXmsEventHandler);
        // must be started before trying to dequeue MMS messages
        tryToDequeueMmsMessages();
    }

    @Override
    public void stop(ImsServiceSession.TerminationReason reasonCode) throws PayloadException {
        if (!isServiceStarted()) {
            return;
        }
        setServiceStarted(false);
        mCmsManager.stop();
        if (ImsServiceSession.TerminationReason.TERMINATION_BY_SYSTEM == reasonCode) {
            mOperationHandler.getLooper().quit();
        }
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
                try {
                    boolean logActivated = sLogger.isActivated();
                    if (logActivated) {
                        sLogger.debug("Synchronize CMS All");
                    }
                    /*
                     * Operations of sync with the message store are executed in a dedicated
                     * background handler. So it does not interact with other operations of this API
                     * impacting the calling UI.
                     */
                    CmsSyncScheduler scheduler = mCmsManager.getSyncScheduler();
                    if (scheduler != null && !scheduler.scheduleSync()) {
                        if (logActivated) {
                            sLogger.debug("Cannot schedule a syncAll operation");
                        }
                    }
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to sync all!", e);
                }
            }
        });
    }

    public void syncOneToOneConversation(final ContactId contact) {
        mOperationHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean logActivated = sLogger.isActivated();
                    String msisdn = contact.toString();
                    if (logActivated) {
                        sLogger.debug("Synchronize CMS for contact ".concat(msisdn));
                    }
                    /*
                     * Operations of sync with the message store are executed in a dedicated
                     * background handler. So it does not interact with other operations of this API
                     * impacting the calling UI.
                     */
                    CmsSyncScheduler scheduler = mCmsManager.getSyncScheduler();
                    if (scheduler != null
                            && !scheduler.scheduleSyncForOneToOneConversation(contact)) {
                        if (logActivated) {
                            sLogger.debug("Cannot schedule a syncOneToOneConversation operation: "
                                    .concat(msisdn));
                        }
                    }
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to sync 1-to-1 Conversation!", e);
                }
            }
        });
    }

    public void syncGroupConversation(final String chatId) {
        mOperationHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean logActivated = sLogger.isActivated();
                    if (logActivated) {
                        sLogger.debug("Synchronize CMS for chatId ".concat(chatId));
                    }
                    /*
                     * Operations of sync with the message store are executed in a dedicated
                     * background handler. So it does not interact with other operations of this API
                     * impacting the calling UI.
                     */
                    CmsSyncScheduler scheduler = mCmsManager.getSyncScheduler();
                    if (scheduler != null && !scheduler.scheduleSyncForGroupConversation(chatId)) {
                        if (logActivated) {
                            sLogger.debug("Cannot schedule a syncGroupConversation operation: "
                                    .concat(chatId));
                        }
                    }
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to sync Group Conversation!", e);
                }
            }
        });
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

    /**
     * Updates deleted flag in CMS provider and synchronizes
     *
     * @param contact the message ID
     * @param messageIds the set of message ID
     */
    public void updateDeletedFlag(ContactId contact, Set<String> messageIds) {
        mCmsManager.onDeleteXmsMessage(contact, messageIds);
    }

    public void deleteCmsData() {
        mCmsLog.removeFolders(true);
    }

    public CmsManager getCmsManager() {
        return mCmsManager;
    }

    /**
     * Receive event reporting system msg session invitation
     *
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onEventReportingSessionReceived(final SipRequest invite, final long timestamp) {
        scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    TerminatingEventFrameworkSession session = new TerminatingEventFrameworkSession(
                            mImsModule.getInstantMessagingService(), invite, mRcsSettings,
                            mMessagingLog, timestamp);
                    session.startSession();

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive o2o chat invitation! (" + e.getMessage()
                                + ")");
                    }
                    tryToSendErrorResponse(invite, Response.BUSY_HERE);

                } catch (PayloadException | RuntimeException e) {
                    sLogger.error("Failed to receive o2o chat invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);

                }
            }
        });
    }
}
