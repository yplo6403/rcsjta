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

import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.provider.imap.FolderData;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.ImapMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Process a sync
 */
public class SyncProcessorImpl implements ISyncProcessor {

    private static Logger sLogger = Logger.getLogger(SyncProcessorImpl.class.getSimpleName());
    
    private BasicImapService mImapService;

    /**
     * Constructor
     * @param imapService
     */
    public SyncProcessorImpl(BasicImapService imapService) {
        mImapService = imapService;
    }

    @Override
    public List<FlagChange> syncRemoteFlags(FolderData localFolder, ImapFolder remoteFolder)
            throws IOException, ImapException {
        return mImapService.fetchFlags(remoteFolder.getName(), localFolder.getMaxUid(), localFolder.getModseq());
    }

    @Override
    public List<ImapMessage> syncRemoteHeaders(FolderData localFolder, ImapFolder remoteFolder)
            throws ImapException, IOException {
        List<ImapMessage> messages = mImapService.fetchHeaders(localFolder.getMaxUid() + 1, remoteFolder.getUidNext());
        for (ImapMessage imapMessage : messages) {
            imapMessage.setFolderPath(remoteFolder.getName());
        }
        return messages;
    }

    @Override
    public List<ImapMessage> syncRemoteMessages(String folderName, Set<Integer> uids)
            throws IOException, ImapException {

        List<ImapMessage> messages = new ArrayList<ImapMessage>();
        for (Integer uid : uids) {
            ImapMessage msg = mImapService.fetchMessage(uid);
            msg.setFolderPath(folderName);
            messages.add(msg);
        }
        return messages;
    }

    @Override
    public void selectFolder(String folderName) throws IOException, ImapException {
        mImapService.selectCondstore(folderName);
    }

    @Override
    public void syncLocalFlags(List<FlagChange> flagChanges) {
        String prevFolder = null;
        List<String> deletedMailboxes = new ArrayList<String>();
        Set<FlagChange> flagChangesToKeep = new HashSet<FlagChange>();
       
        try {
            for (FlagChange flagChange : flagChanges) {
                String folder = flagChange.getFolder();
                if (!folder.equals(prevFolder)) {
                    prevFolder = folder;
                    try {
                        mImapService.selectCondstore(folder);
                    } catch (ImapException e) {
                        if(sLogger.isActivated()){
                            sLogger.debug("The mailbox has been deleted on CMS : ".concat(folder));
                        }
                        // It does not matter if the mailbox does not exist anymore on CMS
                        deletedMailboxes.add(folder);
                    }
                }

                if (deletedMailboxes.contains(folder)) {
                    continue;
                }

                try {   
                    sLogger.warn(new StringBuilder(flagChange.getFolder()).append("/").append(flagChange.getJoinedUids()).toString());
                    mImapService.addFlags(flagChange.getJoinedUids(), flagChange.getFlags());
                } catch (ImapException e) {
                    // It does not matter if the message does not exist anymore on CMS
                    if(sLogger.isActivated()){
                        sLogger.debug(new StringBuilder("The message has been deleted on CMS : ").append(folder).append(",").append(flagChange.getJoinedUids()).toString());
                    }

                }
                flagChangesToKeep.add(flagChange);
            }
        } catch (IOException ioe) { // we stop updating status flag on CMS in case of ioe
        }

        for (FlagChange flagChange : flagChanges) {
            if (deletedMailboxes.contains(flagChange.getFolder())) {
                flagChangesToKeep.add(flagChange);
            }
        }
        flagChanges.retainAll(flagChangesToKeep);
    }

}
