
package com.gsma.rcs.cms.provider.imap;

import com.gsma.rcs.cms.imap.ImapFolder;

/**
 * Utility class
 */
public class DataUtils {

    /**
     * @param imapFolder
     * @return FolderData
     */
    public static FolderData toFolderData(ImapFolder imapFolder) {
        return new FolderData(imapFolder.getName(), imapFolder.getUidNext(),
                imapFolder.getHighestModseq(), imapFolder.getUidValidity());
    }
}
