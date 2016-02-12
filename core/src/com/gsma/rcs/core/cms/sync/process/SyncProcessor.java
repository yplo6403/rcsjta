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
import com.gsma.rcs.provider.cms.CmsFolder;

import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.imaplib.imap.ImapMessage;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Interface used to defined the different steps of a sync
 */
public interface SyncProcessor {

    /**
     * Select folder
     * 
     * @param folderName the folder
     * @throws IOException
     * @throws ImapException
     */
    void selectFolder(String folderName) throws IOException, ImapException;

    /**
     * Synchronize remote flags between local and remote folders
     * 
     * @param localFolder the local folder
     * @param remoteFolder the the remote folder
     * @return Set<FlagChange>
     * @throws IOException
     * @throws ImapException
     */
    List<FlagChange> syncRemoteFlags(CmsFolder localFolder, ImapFolder remoteFolder)
            throws IOException, ImapException;

    /**
     * Synchronize IMAP headers between local and remote folders
     * 
     * @param localFolder the local folder
     * @param remoteFolder the remote folder
     * @return List<ImapMessage>
     * @throws IOException
     * @throws ImapException
     */
    List<ImapMessage> syncRemoteHeaders(CmsFolder localFolder, ImapFolder remoteFolder)
            throws IOException, ImapException;

    /**
     * Synchronize remote messages
     * 
     * @param folderName the folder
     * @param uids the set of UIDs to synchronize
     * @return Set<ImapMessage>
     * @throws IOException
     * @throws ImapException
     */
    Set<ImapMessage> syncRemoteMessages(String folderName, Set<Integer> uids) throws IOException,
            ImapException;

    /**
     * Synchronize local flags
     * 
     * @param flagChanges set of changed flags to synchronize
     */
    void syncLocalFlags(String remoteFolder, Set<FlagChange> flagChanges);

}
