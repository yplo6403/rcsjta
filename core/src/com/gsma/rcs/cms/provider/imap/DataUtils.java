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
