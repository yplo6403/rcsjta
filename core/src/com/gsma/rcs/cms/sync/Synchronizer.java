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

package com.gsma.rcs.cms.sync;

import com.gsma.rcs.cms.CmsManager;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.BasicSynchronizationTask;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.ImapException;

import android.content.Context;

import java.io.IOException;

/**
 * Class used by the API to trigger a sync
 */
public class Synchronizer {

    private static Logger sLogger = Logger.getLogger(Synchronizer.class.getSimpleName());

    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private final LocalStorage mLocalStorage;
    private final ImapServiceController mImapServiceController;

    /**
     * Constructor
     * @param context
     * @param rcsSettings
     * @param cmsManager
     */
    public Synchronizer(Context context, RcsSettings rcsSettings, CmsManager cmsManager) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mLocalStorage = cmsManager.getLocalStorage();
        mImapServiceController = cmsManager.getImapServiceController();
    }

    public void syncFolder(String folder) throws IOException, ImapException, ImapServiceNotAvailableException {
        try {
            mImapServiceController.createService().init();
            BasicSynchronizationTask syncTask = new BasicSynchronizationTask(mContext,
                    mRcsSettings, mImapServiceController, mLocalStorage, null);
            syncTask.syncFolder(folder);
        } finally {
            mImapServiceController.closeService();
        }
    }

    public void syncAll() throws IOException, ImapException, ImapServiceNotAvailableException {
        try {
            mImapServiceController.createService().init();
            BasicSynchronizationTask syncTask = new BasicSynchronizationTask(mContext,
                    mRcsSettings, mImapServiceController, mLocalStorage, null);
            syncTask.syncAll();
        } finally {
            mImapServiceController.closeService();
        }
    }
}
