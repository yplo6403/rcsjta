/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.cms.sync.adapter;

import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.strategy.BasicSyncMediator;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.cpm.ms.sync.SyncMediator;
import com.sonymobile.rcs.cpm.ms.sync.SyncReport;
import com.sonymobile.rcs.cpm.ms.sync.SynchronizationListener;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

public class CmsSyncAdapter extends AbstractThreadedSyncAdapter implements SynchronizationListener {

    private static final Logger sLogger = Logger.getLogger(CmsSyncAdapter.class.getSimpleName());

    // Storage for an instance of the sync adapter
    private static CmsSyncAdapter sSyncAdapter = null;
    // Object to use as a thread-safe lock
    private static final Object sSyncAdapterLock = new Object();

    private SyncMediator mSyncManager;

    private Context mContext;
    
    public static final CmsSyncAdapter getInstance(Context context) {
        /*
         * Create the sync adapter as a singleton. Set the sync adapter as syncable Disallow
         * parallel syncs
         */
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                try {
                    sSyncAdapter = new CmsSyncAdapter(context, true);
                } catch (Exception e) {
                    // Unable to create adapter
                    e.printStackTrace();
                    throw new RuntimeException("Unable to create adapter", e);
                }

            }
            return sSyncAdapter;
        }
    }

    private CmsSyncAdapter(Context context, boolean autoInitialize) throws Exception {
        super(context, autoInitialize);
        mContext = context;
    }

    @Override
    public void onSyncStopped(SyncReport job) {
        boolean loggerActivated = sLogger.isActivated();
        if (loggerActivated) {
            sLogger.info(" >>> onSyncStopped : ".concat(job.toString()));
        }

        Exception error = job.getException();
        if (error != null) {
            sLogger.error("SyncAdapter finished with error ", error);
            return;
        }

        if (loggerActivated) {
            sLogger.info("SyncAdapter finished : ".concat(job.toString()));
            return;
        }
    }

    @Override
    public void onSyncStarted(SyncReport job) {
        if (sLogger.isActivated()) {
            sLogger.info(" >>> onSyncStarted : ".concat(job.toString()));
        }
    }

    @Override
    public synchronized void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {

        boolean loggerActivated = sLogger.isActivated();
        if (loggerActivated) {
            sLogger.info(" >>> onPerformSync : ");
        }

        CmsSettings settings = CmsSettings.getInstance();
        BasicImapService imapService = null;
        try {
            imapService = ImapServiceManager.getService(settings);
            LocalStorage localStorageHandler = LocalStorage.createInstance(ImapLog.getInstance(mContext));
            mSyncManager = new BasicSyncMediator(imapService, localStorageHandler);
            mSyncManager.addSynchronizationListener(this);
            mSyncManager.execute();
        } catch (Exception e) {
            if (loggerActivated) {
                sLogger.error("Exception occurred while synchronization", e);
                e.printStackTrace();
            }            
        }
        finally{
            ImapServiceManager.releaseService(imapService);            
        }
    }
}
