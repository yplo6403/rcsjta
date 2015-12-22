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

package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.strategy.BasicSyncStrategy;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;

/**
 * Task executed for a complete sync with the CMS server.
 * In a first step, change from CMS server are applied in local storage.
 * In a second step, change from local storage are applied on the CMS server
 */
public class BasicSynchronizationTask implements Runnable {
        

    private static final Logger sLogger = Logger.getLogger(BasicSynchronizationTask.class
            .getSimpleName());

    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private final BasicSynchronizationTaskListener mListener;
    private final LocalStorage mLocalStorageHandler;
    private final ImapServiceController mImapServiceController;
    private final String mFolderName;

    /**
     * Constructor used to start a sync on a specific conversation
     * @param context
     * @param rcsSettings
     * @param imapServiceController
     * @param localStorageHandler
     * @param folderName
     * @param listener
     */
    public BasicSynchronizationTask(
            Context context,
            RcsSettings rcsSettings,
            ImapServiceController imapServiceController,
            LocalStorage localStorageHandler,
            String folderName,
            BasicSynchronizationTaskListener listener) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mImapServiceController = imapServiceController;
        mLocalStorageHandler = localStorageHandler;
        mListener = listener;
        mFolderName = folderName;
    }

    /**
     * Constructor used to start a sync on all conversations
     * @param context
     * @param rcsSettings
     * @param imapServiceController
     * @param localStorageHandler
     * @param listener
     */
    public BasicSynchronizationTask(
            Context context,
            RcsSettings rcsSettings,
            ImapServiceController imapServiceController,
            LocalStorage localStorageHandler,
            BasicSynchronizationTaskListener listener) {
        this(context, rcsSettings, imapServiceController, localStorageHandler, null, listener);
    }

    @Override
    public void run() {
        boolean result = false;
            try {
                mImapServiceController.createService();
                if(mFolderName == null){
                    result = syncFolder(mFolderName);
                } else{
                    result = syncAll();
                }
            } catch (Exception e) {
                if(sLogger.isActivated()){
                    sLogger.debug(e.getMessage());
                    e.printStackTrace(); // TODO FIX ME : debug purpose
                }
            }
            finally {
                mImapServiceController.closeService();
                if(mListener!=null){
                    mListener.onBasicSynchronizationTaskExecuted(result);
                }
            }
    }

    /**
     * Method used to start a sync on a conversation in a synchronous way
     * @param folder
     * @return
     * @throws ImapServiceNotAvailableException
     */
    public boolean syncFolder(String folder) throws ImapServiceNotAvailableException {
        if (sLogger.isActivated()) {
            sLogger.info("Sync folder : " + folder);
        }
        BasicSyncStrategy strategy = new BasicSyncStrategy(mContext, mRcsSettings, mImapServiceController,
                mLocalStorageHandler);
        strategy.execute(folder);
        return strategy.getExecutionResult();
    }

    /**
     * Method used to start a sync on all conversations in a synchronous way
     * @return
     * @throws ImapServiceNotAvailableException
     */
    public boolean syncAll() throws ImapServiceNotAvailableException {
        if (sLogger.isActivated()) {
            sLogger.info("Sync all");
        }
        BasicSyncStrategy strategy = new BasicSyncStrategy(mContext, mRcsSettings, mImapServiceController,
                mLocalStorageHandler);
        strategy.execute();
        return strategy.getExecutionResult();
    }

    /**
     * Interface to be notified of the end of a sync (when used in an asynchronous way)
     */
   public interface BasicSynchronizationTaskListener {
       
       /**
        * Callback method
        * @param result
        */
       void onBasicSynchronizationTaskExecuted(Boolean result);
   }
}
