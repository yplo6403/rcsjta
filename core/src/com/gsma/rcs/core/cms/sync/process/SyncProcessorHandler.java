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
import com.gsma.rcs.provider.cms.CmsFolder;

import com.gsma.rcs.imaplib.imap.ImapMessage;

import java.util.List;
import java.util.Set;

/**
 * Local storage interface
 */
public interface SyncProcessorHandler {

    /**
     * Update local IMAP data for a folder. Take into account IMAP counters retrieved from CMS
     * server (NEXTUID, HIGHESTMODSEQ, UIDVALIDITY, ...)
     * 
     * @param folder the folder
     */
    void updateLocalFolder(CmsFolder folder);

    /**
     * Apply flag changes from a remote folder
     * 
     * @param flagchanges the list of flags
     */
    void applyFlagChange(List<FlagChange> flagchanges);

    /**
     * Delete local IMAP folder when it is no more valid by checking the UIDVALIDITY value retrieved
     * from CMS.
     * 
     * @param folderName the folder
     */
    void removeLocalFolder(String folderName);

    /**
     * Return messages that are not already present in local storage. The content of these messages
     * should be downloaded from CMS server
     * 
     * @param messages the list of messages
     * @return uids of new messages
     */
    Set<Integer> filterNewMessages(List<ImapMessage> messages);

    /**
     * Create new messages in local storage.
     * 
     * @param messages the set of messages
     */
    void createMessages(Set<ImapMessage> messages) throws FileAccessException;

    /**
     * Get flag changes from local storage for a folder
     * 
     * @param folder the folder
     * @return flagChanges the set of flag changes
     */
    Set<FlagChange> getLocalFlagChanges(String folder);
}
