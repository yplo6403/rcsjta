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

package com.gsma.rcs.core.cms.sync.process;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.sync.scheduler.task.BasicSynchronizationTask;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.settings.RcsSettings;

import android.content.Context;

/**
 * Class used by the API to trigger a sync
 */
public class Synchronizer {

    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private final LocalStorage mLocalStorage;
    private final BasicImapService mBasicImapService;

    /**
     * Constructor
     * 
     * @param context the context
     * @param rcsSettings the RCS settings accessor
     * @param localStorage the local storage
     * @param basicImapService the basic imap service
     */
    public Synchronizer(Context context, RcsSettings rcsSettings, LocalStorage localStorage,
            BasicImapService basicImapService) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mLocalStorage = localStorage;
        mBasicImapService = basicImapService;
    }

    public void syncFolder(String folder) throws FileAccessException, PayloadException,
            NetworkException {
        BasicSynchronizationTask syncTask = new BasicSynchronizationTask(mContext, mRcsSettings,
                mBasicImapService, mLocalStorage, null);
        syncTask.syncFolder(folder);
    }

    public void syncAll() throws FileAccessException, NetworkException, PayloadException {
        BasicSynchronizationTask syncTask = new BasicSynchronizationTask(mContext, mRcsSettings,
                mBasicImapService, mLocalStorage, null);
        syncTask.syncAll();
    }
}
