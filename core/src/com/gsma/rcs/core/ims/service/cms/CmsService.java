/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.Synchronizer;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.UpdateMmsStateAfterUngracefulTerminationTask;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.ServerApiUtils;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.Set;

/**
 * Created by Philippe LEMORDANT on 12/11/2015.
 */
public class CmsService extends ImsService {
    private static final String CMS_OPERATION_THREAD_NAME = "CmsOperations";
    private final static Logger sLogger = Logger.getLogger(CmsService.class.getSimpleName());
    private final Handler mOperationHandler;
    private final XmsLog mXmsLog;
    private CmsServiceImpl mCmsServiceImpl;
    private final Core mCore;
    private final Context mContext;
    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     *
     * @param core The core service
     * @param parent IMS module
     * @param context The context
     * @param rcsSettings The RCS settings accessor
     * @param xmsLog The XMS log accessor
     */
    public CmsService(Core core, ImsModule parent, Context context, RcsSettings rcsSettings,
            XmsLog xmsLog) {
        super(parent, true);
        mContext = context;
        mOperationHandler = allocateBgHandler(CMS_OPERATION_THREAD_NAME);
        mXmsLog = xmsLog;
        mRcsSettings = rcsSettings;
        mCore = core;
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

    @Override
    public void start() {
        setServiceStarted(true);
        tryToDequeueMmsMessages();
    }

    @Override
    public void stop() {
        setServiceStarted(false);
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
                // TODO catch and log exception at this level
                LocalStorage localStorage = mCore.getCmsManager().getLocalStorage();
                new Synchronizer(mContext, mRcsSettings, localStorage).syncAll();
                mCmsServiceImpl.broadcastAllSynchronized();
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
                // TODO catch and log exception at this level
                LocalStorage localStorage = mCore.getCmsManager().getLocalStorage();
                new Synchronizer(mContext, mRcsSettings, localStorage).syncFolder(CmsUtils
                        .contactToCmsFolder(mRcsSettings, contact));
                mCmsServiceImpl.broadcastOneToOneConversationSynchronized(contact);
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
                            Set<MmsDataObject.MmsPart> parts = mXmsLog.getParts(id);
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
}
