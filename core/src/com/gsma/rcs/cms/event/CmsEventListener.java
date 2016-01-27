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

package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.event.exception.CmsSyncException;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.core.FileAccessException;

/**
 * Interface used to take into account remote changes from CMS server. These changes shall be apply
 * in local storage
 */
public interface CmsEventListener {

    /**
     * Take into account a read flag event from CMS server
     *
     * @param imapData
     */
    void onRemoteReadEvent(MessageData imapData);

    /**
     * Take into account a deleted flag event from CMS server
     *
     * @param imapData
     */
    void onRemoteDeleteEvent(MessageData imapData);

    /**
     * Create new message in local storage
     *
     * @param messageType
     * @param message
     * @return
     * @throws CmsSyncException
     * @throws FileAccessException
     */
    String onRemoteNewMessage(MessageType messageType, IImapMessage message)
            throws CmsSyncException, FileAccessException;

    /**
     * This method checks if the message is already present in local storage. Comparison between
     * local and remote storage is based on message headers
     *
     * @param messageType
     * @param message
     * @return
     * @throws CmsSyncException
     */
    MessageData searchLocalMessage(MessageType messageType, IImapMessage message)
            throws CmsSyncException;
}
