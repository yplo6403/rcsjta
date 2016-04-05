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

import com.gsma.rcs.core.cms.protocol.cmd.ImapFolder;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.provider.cms.CmsFolder;
import com.gsma.rcs.utils.logger.Logger;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Process a CMS synchronization
 */
public class CmsSyncHandler {

    private static Logger sLogger = Logger.getLogger(CmsSyncHandler.class.getSimpleName());

    private final BasicImapService mImapService;

    /**
     * Constructor
     * 
     * @param imapService the IMAP service
     */
    public CmsSyncHandler(BasicImapService imapService) {
        mImapService = imapService;
    }

    /**
     * Synchronize remote flags between local and remote folders
     *
     * @param localFolder the local folder
     * @param remoteFolder the the remote folder
     * @return Set<FlagChange>
     * @throws IOException
     * @throws ImapException
     */
    public List<FlagChangeOperation> syncRemoteFlags(CmsFolder localFolder, ImapFolder remoteFolder)
            throws IOException, ImapException {
        return mImapService.fetchFlags(remoteFolder.getName(), localFolder.getMaxUid(),
                localFolder.getModseq());
    }

    /**
     * Synchronize IMAP headers between local and remote folders
     *
     * @param localFolder the local folder
     * @param remoteFolder the remote folder
     * @return List<ImapMessage>
     * @throws IOException
     * @throws ImapException
     */
    public List<ImapMessage> syncRemoteHeaders(CmsFolder localFolder, ImapFolder remoteFolder)
            throws ImapException, IOException {
        List<ImapMessage> messages = mImapService.fetchHeaders(localFolder.getMaxUid() + 1,
                remoteFolder.getUidNext());
        for (ImapMessage imapMessage : messages) {
            imapMessage.setFolderPath(remoteFolder.getName());
        }
        return messages;
    }

    /**
     * Synchronize remote messages
     *
     * @param folderName the folder
     * @param uids the set of UIDs to synchronize
     * @return Set<ImapMessage>
     * @throws IOException
     * @throws ImapException
     */
    public Set<ImapMessage> syncRemoteMessages(String folderName, Set<Integer> uids)
            throws IOException, ImapException {
        Set<ImapMessage> messages = new HashSet<>();
        for (Integer uid : uids) {
            ImapMessage msg = mImapService.fetchMessage(uid);
            msg.setFolderPath(folderName);
            messages.add(msg);
        }
        return messages;
    }

    /**
     * Select folder
     *
     * @param folderName the folder
     * @throws IOException
     * @throws ImapException
     */
    public void selectFolder(String folderName) throws IOException, ImapException {
        mImapService.selectCondstore(folderName);
    }

    /**
     * Synchronize local flags
     *
     * @param flagChanges set of changed flags to synchronize
     */
    public void syncLocalFlags(String remoteFolder, Set<FlagChangeOperation> flagChanges) {
        Set<FlagChangeOperation> flagChangesToKeep = new HashSet<>();
        boolean logActivated = sLogger.isActivated();
        try {
            for (FlagChangeOperation flagChange : flagChanges) {
                try {
                    if (logActivated) {
                        sLogger.warn(flagChange.getFolder() + "/" + flagChange.getJoinedUids());
                    }
                    mImapService.addFlags(flagChange.getJoinedUids(), flagChange.getFlag());

                } catch (ImapException e) {
                    // It does not matter if the message does not exist anymore on CMS
                    if (logActivated) {
                        sLogger.debug("The message has been deleted on CMS : " + remoteFolder + ","
                                + flagChange.getJoinedUids());
                    }

                }
                flagChangesToKeep.add(flagChange);
            }
        } catch (IOException ioe) { // we stop updating status flag on CMS in case of ioe
            if (logActivated) {
                sLogger.debug("IOException during sync with CMS server");
                ioe.printStackTrace();
            }

        }
        flagChanges.retainAll(flagChangesToKeep);
    }

}
