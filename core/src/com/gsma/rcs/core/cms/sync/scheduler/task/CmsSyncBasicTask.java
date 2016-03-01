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
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncSchedulerTask;
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
public class CmsSyncBasicTask extends CmsSyncSchedulerTask {

    private static final Logger sLogger = Logger.getLogger(CmsSyncBasicTask.class.getSimpleName());

    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private final LocalStorage mLocalStorageHandler;
    private final String mFolderName;

    /**
     * Constructor used to start a sync on a specific conversation
     * 
     * @param context the context
     * @param rcsSettings the RCS settings accessor
     * @param localStorageHandler the local storage accessor
     * @param folderName the folder name
     */
    public CmsSyncBasicTask(Context context, RcsSettings rcsSettings,
            LocalStorage localStorageHandler, String folderName) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mLocalStorageHandler = localStorageHandler;
        mFolderName = folderName;
    }

    /**
     * Constructor used to start a sync on all conversations
     * 
     * @param context the context
     * @param rcsSettings the RCS settings accessor
     * @param localStorageHandler the local storage accessor
     */
    public CmsSyncBasicTask(Context context, RcsSettings rcsSettings,
            LocalStorage localStorageHandler) {
        this(context, rcsSettings, localStorageHandler, null);
    }

    @Override
    public void execute(BasicImapService basicImapService) throws NetworkException,
            PayloadException, FileAccessException {
        if (mFolderName != null) {
            syncFolder(basicImapService, mFolderName);
        } else {
            syncAll(basicImapService);
        }
    }

    /**
     * Method used to start a sync on a conversation in a synchronous way
     * 
     * @param folder the folder
     * @return True if sync is successful
     * @throws FileAccessException, PayloadException, NetworkException
     */
    protected boolean syncFolder(BasicImapService basicImapService, String folder)
            throws FileAccessException, PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Sync folder: ".concat(folder));
        }
        BasicSyncStrategy strategy = new BasicSyncStrategy(mContext, mRcsSettings,
                basicImapService, mLocalStorageHandler);
        strategy.execute(folder);
        return strategy.getExecutionResult();
    }

    /**
     * Method used to start a sync on all conversations in a synchronous way
     * 
     * @return True if sync is successful
     * @throws PayloadException, NetworkException, FileAccessException
     */
    protected boolean syncAll(BasicImapService basicImapService) throws PayloadException,
            NetworkException, FileAccessException {
        if (sLogger.isActivated()) {
            sLogger.info("Sync all");
        }
        BasicSyncStrategy strategy = new BasicSyncStrategy(mContext, mRcsSettings,
                basicImapService, mLocalStorageHandler);
        strategy.execute();
        return strategy.getExecutionResult();
    }
}
