package com.gsma.rcs.cms.sync;

import com.gsma.rcs.cms.provider.imap.FolderData;
import com.gsma.rcs.cms.sync.strategy.FlagChange;

import com.sonymobile.rcs.imap.ImapMessage;

import java.util.List;
import java.util.Set;

/**
 * Local storage interface
 */
public interface ISynchronizationHandler {

    /**
     * Update local IMAP data for a folder.
     * Take into account IMAP counters retrieved from CMS server
     * (NEXTUID, HIGHESTMODSEQ, UIDVALIDITY, ...)
     * @param folder
     */
    public void updateLocalFolder(FolderData folder);
    
    /**
     * Apply flag changes from a remote folder
     * @param flagchanges
     */
    public void applyFlagChange(List<FlagChange> flagchanges);
    
    /**
     * Delete local IMAP folder when it is no more valid
     * by checking the UIDVALIDITY value retrieved from CMS.
     * @param folderName
     */
    public void removeLocalFolder(String folderName);
    
    /**
     * Return messages that are not already present in local storage.
     * The content of these messages should be downloaded from CMS server  
     * @param messages
     * @return uids of new messages
     */
    public Set<Integer> filterNewMessages(List<ImapMessage> messages);
    
    /**
     * Create new messages in local storage.
     * @param messages
     */
    public void createMessages(List<ImapMessage> messages);
       
    /**
     * Get flag change from local storage for a folder
     * @param folder 
     * @return flagChanges
     */
    public List<FlagChange> getLocalFlagChanges(String folder);
}
