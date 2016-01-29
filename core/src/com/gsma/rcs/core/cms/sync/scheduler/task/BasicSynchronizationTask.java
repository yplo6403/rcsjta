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

package com.gsma.rcs.core.cms.sync.scheduler.task;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.sync.process.BasicSyncStrategy;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;

/**
 * Task executed for a complete sync with the CMS server. In a first step, change from CMS server
 * are applied in local storage. In a second step, change from local storage are applied on the CMS
 * server
 */
public class BasicSynchronizationTask implements Runnable {

    private static final Logger sLogger = Logger.getLogger(BasicSynchronizationTask.class
            .getSimpleName());

    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private final BasicSynchronizationTaskListener mListener;
    private final LocalStorage mLocalStorageHandler;
    private final BasicImapService mBasicImapService;
    private final String mFolderName;

    /**
     * Constructor used to start a sync on a specific conversation
     * 
     * @param context the context
     * @param rcsSettings the RCS settings accessor
     * @param basicImapService the IMAP service
     * @param localStorageHandler the local storage accessor
     * @param folderName the folder name
     * @param listener the sync listener
     */
    public BasicSynchronizationTask(Context context, RcsSettings rcsSettings,
            BasicImapService basicImapService, LocalStorage localStorageHandler, String folderName,
            BasicSynchronizationTaskListener listener) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mBasicImapService = basicImapService;
        mLocalStorageHandler = localStorageHandler;
        mListener = listener;
        mFolderName = folderName;
    }

    /**
     * Constructor used to start a sync on all conversations
     * 
     * @param context the context
     * @param rcsSettings the RCS settings accessor
     * @param basicImapService the IMAP service
     * @param localStorageHandler the local storage accessor
     * @param listener the sync listener
     */
    public BasicSynchronizationTask(Context context, RcsSettings rcsSettings,
            BasicImapService basicImapService, LocalStorage localStorageHandler,
            BasicSynchronizationTaskListener listener) {
        this(context, rcsSettings, basicImapService, localStorageHandler, null, listener);
    }

    @Override
    public void run() {
        boolean result = false;
        try {
            if (mFolderName != null) {
                result = syncFolder(mFolderName);
            } else {
                result = syncAll();
            }
        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                if (mFolderName != null) {
                    sLogger.info("Failed to sync CMS for folder=" + mFolderName + ": "
                            + e.getMessage());
                } else {
                    sLogger.info("Failed to sync CMS: " + e.getMessage());
                }
            }
        } catch (RuntimeException | PayloadException | FileAccessException e) {
            if (mFolderName != null) {
                sLogger.error("Failed to sync CMS for folder=" + mFolderName, e);
            } else {
                sLogger.error("Failed to sync CMS", e);
            }
        } finally {
            if (mListener != null) {
                mListener.onBasicSynchronizationTaskExecuted(result);
            }
        }
    }

    /**
     * Method used to start a sync on a conversation in a synchronous way
     * 
     * @param folder the folder
     * @return True if sync is successful
     * @throws FileAccessException, PayloadException, NetworkException
     */
    public boolean syncFolder(String folder) throws FileAccessException, PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Sync folder: ".concat(folder));
        }
        BasicSyncStrategy strategy = new BasicSyncStrategy(mContext, mRcsSettings,
                mBasicImapService, mLocalStorageHandler);
        strategy.execute(folder);
        return strategy.getExecutionResult();
    }

    /**
     * Method used to start a sync on all conversations in a synchronous way
     * 
     * @return True if sync is successful
     * @throws PayloadException, NetworkException, FileAccessException
     */
    public boolean syncAll() throws PayloadException, NetworkException, FileAccessException {
        if (sLogger.isActivated()) {
            sLogger.info("Sync all");
        }
        BasicSyncStrategy strategy = new BasicSyncStrategy(mContext, mRcsSettings,
                mBasicImapService, mLocalStorageHandler);
        strategy.execute();
        return strategy.getExecutionResult();
    }

    /**
     * Interface to be notified of the end of a sync (when used in an asynchronous way)
     */
    public interface BasicSynchronizationTaskListener {

        /**
         * Callback method
         * 
         * @param result True if result is successful
         */
        void onBasicSynchronizationTaskExecuted(Boolean result);
    }
}
