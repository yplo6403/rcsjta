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

package com.gsma.rcs.cms.sync.strategy;

import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.PushMessageTask;
import com.gsma.rcs.cms.provider.imap.DataUtils;
import com.gsma.rcs.cms.provider.imap.FolderData;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.ISyncProcessor;
import com.gsma.rcs.cms.sync.SyncProcessorImpl;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.cpm.ms.impl.sync.AbstractSyncStrategy;
import com.sonymobile.rcs.cpm.ms.sync.MutableReport;
import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.ImapMessage;

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

    private final ImapServiceController mImapServiceController;
    private final RcsSettings mRcsSettings;
    private final Context mContext;
    private final LocalStorage mLocalStorageHandler;
    private ISyncProcessor mSynchronizer;

    /**
     * @param imapServiceController
     * @param localStorageHandler
     */
    public BasicSyncStrategy(Context context, RcsSettings rcsSettings,
                             ImapServiceController imapServiceController, LocalStorage localStorageHandler) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mImapServiceController = imapServiceController;
        mLocalStorageHandler = localStorageHandler;
    }

    /**
     * Execute a full sync
     */
    public void execute() {
        execute((String) null);
    }

    /**
     * Execute a sync for only one folder
     * 
     * @param folderName
     */
    public void execute(String folderName) {

        mExecutionResult = false;
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug(">>> BasicSyncStrategy.execute");
        }

        try {
            Map<String, FolderData> localFolders = mLocalStorageHandler.getLocalFolders();
            BasicImapService imapService = mImapServiceController.getService();
            mSynchronizer = new SyncProcessorImpl(imapService);

            for (ImapFolder remoteFolder : imapService.listStatus()) {
                String remoteFolderName = remoteFolder.getName();
                if (folderName != null && !remoteFolderName.equals(folderName)) {
                    continue;
                }
                FolderData localFolder = localFolders.get(remoteFolderName);
                if (localFolder == null) {
                    localFolder = new FolderData(remoteFolderName);
                }
                if (shouldStartRemoteSynchronization(localFolder, remoteFolder)) {
                    startRemoteSynchro(localFolder, remoteFolder);
                    mLocalStorageHandler.applyFolderChange(DataUtils.toFolderData(remoteFolder));
                }

                // sync CMS with local change
                List<FlagChange> flagChanges = mLocalStorageHandler
                        .getLocalFlagChanges(remoteFolderName);
                mSynchronizer.syncLocalFlags(flagChanges);
                mLocalStorageHandler.finalizeLocalFlagChanges(flagChanges);
            }

            // TODO FGI
            // Demo purpose only
            // push on CMS server, messages that are marked as PUSH_requested in database
            // try to get an instance of XmsLog
            XmsLog xmsLog = XmsLog.getInstance();
            ImapLog imapLog = ImapLog.getInstance();
            if (xmsLog != null && imapLog != null) {
                List<XmsDataObject> messagesToPush = new ArrayList<>();
                for (MessageData messageData : imapLog.getXmsMessages(PushStatus.PUSH_REQUESTED)) {
                    XmsDataObject xms = xmsLog.getXmsDataObject(messageData.getMessageId());
                    if (xms != null) {
                        messagesToPush.add(xms);
                    }
                }
                if (!messagesToPush.isEmpty()) {
                    PushMessageTask pushMessageTask = new PushMessageTask(mContext, mRcsSettings,
                            mImapServiceController, xmsLog, imapLog, null);
                    pushMessageTask.pushMessages(messagesToPush);
                    for (Entry<String, Integer> entry : pushMessageTask.getCreatedUids().entrySet()) {
                        String baseId = entry.getKey();
                        Integer uid = entry.getValue();
                        imapLog.updateXmsPushStatus(uid, baseId, PushStatus.PUSHED);
                    }
                }
            }

            mExecutionResult = true;
        } catch (IOException | ImapException | ImapServiceNotAvailableException | FileAccessException e) {
            sLogger.error(e.getMessage(), e);
        }

        if (logActivated) {
            sLogger.debug("<<< BasicSyncStrategy.execute ");
        }
    }

    private void startRemoteSynchro(FolderData localFolder, ImapFolder remoteFolder)
            throws IOException, ImapException, FileAccessException {
        String folderName = remoteFolder.getName();

        mSynchronizer.selectFolder(folderName);

        if (localFolder.hasMessages()) {
            List<FlagChange> flagChanges = mSynchronizer.syncRemoteFlags(localFolder, remoteFolder);
            mLocalStorageHandler.applyFlagChange(flagChanges);
        }

        List<ImapMessage> messages = mSynchronizer.syncRemoteHeaders(localFolder, remoteFolder);
        Set<Integer> uids = mLocalStorageHandler.filterNewMessages(messages);

        List<ImapMessage> newMessages = mSynchronizer.syncRemoteMessages(remoteFolder.getName(),
                uids);
        mLocalStorageHandler.createMessages(newMessages);

    }

    private boolean shouldStartRemoteSynchronization(FolderData local, ImapFolder remote) {
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

    /**
     * @return result for execution of sync strategy
     */
    public boolean getExecutionResult() {
        return mExecutionResult;
    }

}
