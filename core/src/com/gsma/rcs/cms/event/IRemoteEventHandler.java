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

package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.core.FileAccessException;

/**
 * Interface used to take into account remote changes from CMS server.
 * These changes shall be apply in local storage
 */
public interface IRemoteEventHandler {

    /**
     * Take into account a read flag event from CMS server
     *
     * @param messageType
     * @param messageId
     */
    void onRemoteReadEvent(MessageType messageType, String messageId);

    /**
     * Take into account a deleted flag event from CMS server
     *
     * @param messageType
     * @param messageId
     */
    void onRemoteDeleteEvent(MessageType messageType, String messageId);

    /**
     * Create new message in local storage
     *
     * @param messageType
     * @param message
     * @return
     * @throws ImapHeaderFormatException
     * @throws MissingImapHeaderException
     * @throws FileAccessException
     */
    String onRemoteNewMessage(MessageType messageType, IImapMessage message)
            throws ImapHeaderFormatException, MissingImapHeaderException, FileAccessException;

    /**
     * This method checks if the message is already present in local storage. Comparison between
     * local and remote storage is based on message headers
     *
     * @param messageType
     * @param message
     * @return
     * @throws ImapHeaderFormatException
     * @throws MissingImapHeaderException
     */
    String getMessageId(MessageType messageType, IImapMessage message)
            throws ImapHeaderFormatException, MissingImapHeaderException;
}
