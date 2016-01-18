// FG add copyrights + javadoc

package com.gsma.rcs.cms.sync;

import com.gsma.rcs.cms.provider.imap.FolderData;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.core.FileAccessException;

import com.sonymobile.rcs.imap.ImapMessage;

import java.util.List;
import java.util.Set;

/**
 * Local storage interface
 */
public interface ISyncProcessorHandler {

    /**
     * Update local IMAP data for a folder. Take into account IMAP counters retrieved from CMS
     * server (NEXTUID, HIGHESTMODSEQ, UIDVALIDITY, ...)
     * 
     * @param folder the folder
     */
    void updateLocalFolder(FolderData folder);

    /**
     * Apply flag changes from a remote folder
     * 
     * @param flagchanges
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
     * @param messages
     * @return uids of new messages
     */
    Set<Integer> filterNewMessages(List<ImapMessage> messages);

    /**
     * Create new messages in local storage.
     * 
     * @param messages
     */
    void createMessages(List<ImapMessage> messages) throws FileAccessException;

    /**
     * Get flag changes from local storage for a folder
     * 
     * @param folder the folder
     * @return flagChanges the set of flag changes
     */
    Set<FlagChange> getLocalFlagChanges(String folder);
}
