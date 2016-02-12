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
import com.gsma.rcs.core.cms.protocol.cmd.ImapFolder;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncPushMessageTask;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.imaplib.cpm.ms.impl.sync.AbstractSyncStrategy;
import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.provider.cms.CmsFolder;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsUtils;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * In charge of executing an IMAP sync with the CMS server
 */
public class BasicSyncStrategy extends AbstractSyncStrategy {

    private static final long serialVersionUID = 1L;

    private static Logger sLogger = Logger.getLogger(BasicSyncStrategy.class.getName());

    private boolean mExecutionResult;

    private final BasicImapService mBasicImapService;
    private final RcsSettings mRcsSettings;
    private final Context mContext;
    private final LocalStorage mLocalStorageHandler;
    private SyncProcessor mSynchronizer;

    /**
     * @param basicImapService IMAP service
     * @param localStorageHandler local storage handler
     */
    public BasicSyncStrategy(Context context, RcsSettings rcsSettings,
            BasicImapService basicImapService, LocalStorage localStorageHandler) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mBasicImapService = basicImapService;
        mLocalStorageHandler = localStorageHandler;
    }

    /**
     * Execute a full sync
     */
    public void execute() throws FileAccessException, NetworkException, PayloadException {
        execute(null);
    }

    /**
     * Execute a sync for only one folder or all is argument is null
     * 
     * @param folderName the folder to synchronize
     */
    public void execute(String folderName) throws FileAccessException, NetworkException,
            PayloadException {
        mExecutionResult = false;
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug(">>> BasicSyncStrategy.execute");
        }
        Map<String, CmsFolder> localFolders = mLocalStorageHandler.getLocalFolders();
        mSynchronizer = new SyncProcessorImpl(mBasicImapService);

        try {
            for (ImapFolder remoteFolder : mBasicImapService.listStatus()) {
                String remoteFolderName = remoteFolder.getName();
                if (folderName != null && !remoteFolderName.equals(folderName)) {
                    continue;
                }
                CmsFolder localFolder = localFolders.get(remoteFolderName);
                if (localFolder == null) {
                    /* Remote folder exists but not local */
                    localFolder = new CmsFolder(remoteFolderName);
                }
                boolean isMailboxSelected = false;
                if (shouldStartRemoteSynchronization(localFolder, remoteFolder)) {
                    startRemoteSynchro(localFolder, remoteFolder);
                    mLocalStorageHandler.applyFolderChange(CmsUtils.toCmsFolder(remoteFolder));
                    isMailboxSelected = true;
                }

                if (!isMailboxSelected) {
                    mSynchronizer.selectFolder(remoteFolderName);
                }
                /* sync CMS with local change */
                Set<FlagChange> flagChanges = mLocalStorageHandler
                        .getLocalFlagChanges(remoteFolderName);
                mSynchronizer.syncLocalFlags(remoteFolderName, flagChanges);
                mLocalStorageHandler.finalizeLocalFlagChanges(flagChanges);
            }

            pushLocalMessages(folderName);

            mExecutionResult = true;
            if (logActivated) {
                sLogger.debug("<<< BasicSyncStrategy.execute ");
            }
        } catch (IOException e) {
            throw new NetworkException("Sync failed", e);

        } catch (ImapException e) {
            throw new PayloadException("Sync failed", e);
        }
    }

    private void startRemoteSynchro(CmsFolder localFolder, ImapFolder remoteFolder)
            throws IOException, ImapException, FileAccessException {
        String folderName = remoteFolder.getName();

        mSynchronizer.selectFolder(folderName);

        if (localFolder.hasMessages()) {
            List<FlagChange> flagChanges = mSynchronizer.syncRemoteFlags(localFolder, remoteFolder);
            mLocalStorageHandler.applyFlagChange(flagChanges);
        }
        List<ImapMessage> messages = mSynchronizer.syncRemoteHeaders(localFolder, remoteFolder);
        Set<Integer> uids = mLocalStorageHandler.filterNewMessages(messages);

        Set<ImapMessage> newMessages = mSynchronizer.syncRemoteMessages(remoteFolder.getName(),
                uids);
        mLocalStorageHandler.createMessages(newMessages);
    }

    private boolean shouldStartRemoteSynchronization(CmsFolder local, ImapFolder remote) {
        boolean sync = false;
        if (local.isNewFolder()) {
            sync = !remote.isEmpty();

        } else if (!local.getUidValidity().equals(remote.getUidValidity())) {
            mLocalStorageHandler.removeLocalFolder(remote.getName());
            sync = true;

        } else if (!local.getModseq().equals(remote.getHighestModseq())) {
            sync = true;
        }
        if (sLogger.isActivated()) {
            sLogger.debug(">>> shouldStartSynchronization : ".concat(String.valueOf(sync)));
            sLogger.debug("local folder : ".concat(local.toString()));
            sLogger.debug("remote folder : ".concat(remote.toString()));
            sLogger.debug("<<< shouldStartSynchronization");
        }
        return sync;
    }

    private void pushLocalMessages(String localFolderName) throws NetworkException,
            PayloadException {
        XmsLog xmsLog = XmsLog.getInstance();
        CmsLog cmsLog = CmsLog.getInstance();
        if (xmsLog == null || cmsLog == null) {
            return;
        }
        Set<CmsObject> cmsObjects;
        if (localFolderName == null) { // get all messages
            cmsObjects = cmsLog.getXmsMessages(PushStatus.PUSH_REQUESTED);
        } else {
            cmsObjects = cmsLog.getXmsMessages(localFolderName, PushStatus.PUSH_REQUESTED);
        }
        List<XmsDataObject> messagesToPush = new ArrayList<>();
        for (CmsObject cmsObject : cmsObjects) {
            XmsDataObject xms = xmsLog.getXmsDataObject(cmsObject.getMessageId());
            if (xms != null) {
                messagesToPush.add(xms);
            }
        }
        if (!messagesToPush.isEmpty()) {
            CmsSyncPushMessageTask pushMessageTask = new CmsSyncPushMessageTask(mContext,
                    mRcsSettings, xmsLog, cmsLog);
            pushMessageTask.setBasicImapService(mBasicImapService);
            pushMessageTask.pushMessages(messagesToPush);
            for (Entry<String, Integer> entry : pushMessageTask.getCreatedUids().entrySet()) {
                String baseId = entry.getKey();
                Integer uid = entry.getValue();
                cmsLog.updateXmsPushStatus(uid, baseId, PushStatus.PUSHED);
            }
        }
    }

    /**
     * @return result True is synchronization is successful
     */
    public boolean getExecutionResult() {
        return mExecutionResult;
    }

}