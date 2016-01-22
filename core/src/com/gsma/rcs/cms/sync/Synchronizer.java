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

package com.gsma.rcs.cms.sync;

import com.gsma.rcs.cms.CmsManager;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.BasicSynchronizationTask;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.settings.RcsSettings;

import com.sonymobile.rcs.imap.ImapException;

import android.content.Context;

import java.io.IOException;

/**
 * Class used by the API to trigger a sync
 */
public class Synchronizer {

    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private final LocalStorage mLocalStorage;
    private final ImapServiceController mImapServiceController;

    /**
     * Constructor
     * 
     * @param context the context
     * @param rcsSettings the RCS settings accessor
     * @param cmsManager the CMS manager
     */
    public Synchronizer(Context context, RcsSettings rcsSettings, CmsManager cmsManager) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mLocalStorage = cmsManager.getLocalStorage();
        mImapServiceController = cmsManager.getImapServiceController();
    }

    public void syncFolder(String folder) throws ImapServiceNotAvailableException,
            FileAccessException, PayloadException, NetworkException {
        try {
            mImapServiceController.createService().init();
            BasicSynchronizationTask syncTask = new BasicSynchronizationTask(mContext,
                    mRcsSettings, mImapServiceController, mLocalStorage, null);
            syncTask.syncFolder(folder);

        } catch (ImapException e) {
            throw new PayloadException("Failed to sync folder: " + folder, e);

        } catch (IOException e) {
            throw new NetworkException("Failed to sync folder: " + folder, e);

        } finally {
            mImapServiceController.closeService();
        }
    }

    public void syncAll() throws ImapServiceNotAvailableException, FileAccessException,
            NetworkException, PayloadException {
        try {
            mImapServiceController.createService().init();
            BasicSynchronizationTask syncTask = new BasicSynchronizationTask(mContext,
                    mRcsSettings, mImapServiceController, mLocalStorage, null);
            syncTask.syncAll();
        } catch (ImapException e) {
            throw new PayloadException("Failed to sync", e);

        } catch (IOException e) {
            throw new NetworkException("Failed to sync", e);

        } finally {
            mImapServiceController.closeService();
        }
    }
}
