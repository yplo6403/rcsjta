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

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.gsma.rcs.cms.CmsManager;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.Synchronizer;
import com.gsma.rcs.cms.utils.CmsUtils;

/**
 * Created by Philippe LEMORDANT on 12/11/2015.
 */
public class CmsService extends ImsService {
    private static final String CMS_OPERATION_THREAD_NAME = "CmsOperations";
    private final static Logger sLogger = Logger.getLogger(CmsService.class.getSimpleName());
    private final Handler mOperationHandler;
    private final XmsLog mXmsLog;
    private CmsServiceImpl mCmsService;

    private final Context mContext;
    private final CmsManager mCmsManager;
    private final RcsSettings mRcsSettings;
    private final LocalStorage mLocalStorage;

    /**
     * Constructor
     *
     * @param parent IMS module
     * @param xmsLog The XMS log accessor
     */
    public CmsService(ImsModule parent, Context context, RcsSettings rcsSettings, XmsLog xmsLog) {
        super(parent, true);
        mContext = context;
        mOperationHandler = allocateBgHandler(CMS_OPERATION_THREAD_NAME);
        mXmsLog = xmsLog;
        mCmsManager = parent.getCore().getCmsManager();
        mRcsSettings = rcsSettings;
        mLocalStorage = mCmsManager.getLocalStorage();
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
        mCmsService = service;
    }

    @Override
    public void start() {
        setServiceStarted(true);
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

    public void syncAll() {
        mOperationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.debug("Synchronize CMS");
                }
                //TODO catch exception at this level
                new Synchronizer(mContext, mRcsSettings, mLocalStorage).syncAll();
                mCmsService.broadcastAllSynchronized();
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
                //TODO catch exception at this level
                new Synchronizer(mContext, mRcsSettings, mLocalStorage).syncFolder(CmsUtils.contactToCmsFolder(mRcsSettings, contact));
                mCmsService.broadcastOneToOneConversationSynchronized(contact);
            }
        });
    }

    public void syncGroupConversation(final String chatId) {
        // TODO
    }

}
