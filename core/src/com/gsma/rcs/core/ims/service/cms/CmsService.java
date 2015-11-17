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

import android.os.Handler;
import android.os.HandlerThread;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Created by yplo6403 on 12/11/2015.
 */
public class CmsService extends ImsService {

    private static final String CMS_OPERATION_THREAD_NAME = "CmsOperations";
    private final static Logger sLogger = Logger.getLogger(CmsService.class.getSimpleName());
    private final Handler mOperationHandler;
    private CmsServiceImpl mCmsService;

    /**
     * Constructor
     *
     * @param parent IMS module
     */
    public CmsService(ImsModule parent) {
        super(parent, true);
        mOperationHandler = allocateBgHandler(CMS_OPERATION_THREAD_NAME);
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
        if (isServiceStarted()) {
            /* Already started */
            return;
        }
        setServiceStarted(true);
    }

    @Override
    public void stop() {
        if (!isServiceStarted()) {
            /* Already stopped */
            return;
        }
        setServiceStarted(false);
    }

    @Override
    public void check() {
    }

    public void syncAll() {
        mOperationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.debug("Synchronize CMS");
                }
                // TODO
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
                // TODO
                mCmsService.broadcastOneToOneConversationSynchronized(contact);
            }
        });
    }

    public void syncGroupConversation(final String chatId) {
        mOperationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.debug("Synchronize CMS for chat ID " + chatId);
                }
                // TODO
                mCmsService.broadcastGroupConversationSynchronized(chatId);
            }
        });
    }

    public void scheduleOperation(Runnable runnable) {
        mOperationHandler.post(runnable);
    }
}
