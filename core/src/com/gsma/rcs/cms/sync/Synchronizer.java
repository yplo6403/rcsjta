
package com.gsma.rcs.cms.sync;

import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.BasicSynchronizationTask;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.ImapException;

import android.content.Context;

import java.io.IOException;

public class Synchronizer {

    private static Logger sLogger = Logger.getLogger(Synchronizer.class.getSimpleName());

    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private final LocalStorage mLocalStorage;

    public Synchronizer(Context context, RcsSettings rcsSettings, LocalStorage localStorage) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mLocalStorage = localStorage;
    }

    private BasicImapService getService() {
        try {
            return ImapServiceManager.getService(mRcsSettings);

        } catch (ImapServiceNotAvailableException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            return null;
        }
    }

    public void syncFolder(String folder) throws IOException, ImapException {
        BasicImapService imapService = getService();
        if (imapService == null) {
            return;
        }
        try {
            imapService.init();
            BasicSynchronizationTask syncTask = new BasicSynchronizationTask(mContext,
                    mRcsSettings, imapService, mLocalStorage, null);
            syncTask.syncFolder(folder);
        } finally {
            ImapServiceManager.releaseService(imapService);
        }
    }

    public void syncAll() throws IOException, ImapException {
        BasicImapService imapService = getService();
        if (imapService == null) {
            return;
        }
        try {
            imapService.init();
            // TODO FG do not use async task
            BasicSynchronizationTask syncTask = new BasicSynchronizationTask(mContext,
                    mRcsSettings, imapService, mLocalStorage, null);
            syncTask.syncAll();
        } finally {
            ImapServiceManager.releaseService(imapService);
        }
    }
}
