
package com.gsma.rcs.cms.sync.strategy;

import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.storage.LocalStorage;

import com.sonymobile.rcs.cpm.ms.MessageStore;
import com.sonymobile.rcs.cpm.ms.impl.sync.DefaultSyncMediator;
import com.sonymobile.rcs.cpm.ms.sync.MutableReport;
import com.sonymobile.rcs.cpm.ms.sync.SyncMediator;
import com.sonymobile.rcs.cpm.ms.sync.SyncReport;
import com.sonymobile.rcs.cpm.ms.sync.SyncStrategy;
import com.sonymobile.rcs.cpm.ms.sync.SynchronizationListener;

import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Logger;

public class BasicSyncMediator implements SyncMediator {

    private static BasicSyncMediator sInstance = null;

    private Collection<SynchronizationListener> mSyncListeners = new LinkedList<SynchronizationListener>();

    private SyncStrategy mStrategy;

    private MutableReport mCurrentReport;

    private static Logger LOG = Logger.getLogger(DefaultSyncMediator.class.getName());

    /**
     * @param imapService
     * @param locaStorageHandler
     */
    public BasicSyncMediator(BasicImapService imapService, LocalStorage locaStorageHandler) {
        super();
        if (sInstance != null) {
            LOG.warning("DefaultSynchronizationManager Instance already exists");
        }
        sInstance = this;
        mStrategy = new BasicSyncStrategy(imapService, locaStorageHandler);
    }

    @Override
    public SyncReport getCurrentReport() {
        return mCurrentReport;
    }

    @Override
    public void setStrategy(SyncStrategy strategy) {
        this.mStrategy = strategy;
    }

    @Override
    public SyncStrategy getStrategy() {
        return mStrategy;
    }

    @Override
    public SyncReport execute() {
        mCurrentReport = new MutableReport();

        try {
            fireSyncStarted(mCurrentReport);
            ((BasicSyncStrategy) mStrategy).execute(mCurrentReport);

            mCurrentReport.setSuccess(true);

        } catch (Exception e) {

            mCurrentReport.setException(e);

        } finally {
            mCurrentReport.setStopped();
            fireSyncFinished(mCurrentReport);
        }

        return mCurrentReport;
    }

    /**
     * Same as execute() but will raise the exception. Should be used for testing.
     * 
     * @param itemType
     * @throws Exception
     */
    public void executeUnsafe() throws Exception {
        SyncReport report = execute();
        if (report.getException() != null) {
            throw report.getException();
        }
        if (!report.isSuccess()) {
            throw new Exception("Sync not successful, exception not raised");
        }
    }

    @Override
    public MessageStore getRemoteStore() {
        return null;
    }

    @Override
    public MessageStore getLocalStore() {
        return null;
    }

    @Override
    public void addSynchronizationListener(SynchronizationListener listener) {
        mSyncListeners.add(listener);
    }

    @Override
    public void removeSynchronizationListener(SynchronizationListener listener) {
        mSyncListeners.remove(listener);
    }

    private void fireSyncStarted(MutableReport r) {
        for (SynchronizationListener l : mSyncListeners) {
            l.onSyncStarted(r);
        }
    }

    private void fireSyncFinished(MutableReport report) {
        for (SynchronizationListener l : mSyncListeners) {
            l.onSyncStopped(report);
        }
    }
}
