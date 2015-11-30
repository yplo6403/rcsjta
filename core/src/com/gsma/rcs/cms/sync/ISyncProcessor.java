
package com.gsma.rcs.cms.sync;

import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.provider.imap.FolderData;
import com.gsma.rcs.cms.sync.strategy.FlagChange;

import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.ImapMessage;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ISyncProcessor {

    /**
     * @param folderName
     * @throws IOException
     * @throws ImapException
     */
    void selectFolder(String folderName) throws IOException, ImapException;

    /**
     * @param localFolder
     * @param remoteFolder
     * @return Set<FlagChange>
     * @throws IOException
     * @throws ImapException
     */
    List<FlagChange> syncRemoteFlags(FolderData localFolder, ImapFolder remoteFolder)
            throws IOException, ImapException;

    /**
     * @param localFolder
     * @param remoteFolder
     * @return Set<Integer>
     * @throws IOException
     * @throws ImapException
     */
    List<ImapMessage> syncRemoteHeaders(FolderData localFolder, ImapFolder remoteFolder)
            throws IOException, ImapException;

    /**
     * @param folderName
     * @param uids
     * @return List<ImapMessage>
     * @throws IOException
     * @throws ImapException
     */
    List<ImapMessage> syncRemoteMessages(String folderName, Set<Integer> uids)
            throws IOException, ImapException;
    
    /**
     * @param flagChanges 
     * @throws IOException
     * @throws ImapException
     */
    void syncLocalFlags(List<FlagChange> flagChanges);

}
