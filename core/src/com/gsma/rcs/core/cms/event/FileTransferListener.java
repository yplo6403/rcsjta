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

package com.gsma.rcs.core.cms.event;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

/**
 * Interface used to take into account changes for file transfer.
 */
public interface FileTransferListener {

    /**
     * Take into account a new file transfer (incoming or outgoing)
     *
     * @param contact the remote contact
     * @param direction the direction
     * @param transferId the message ID
     */
    void onNewFileTransfer(ContactId contact, Direction direction, String transferId);

    /**
     * Take into account a new group file transfer (incoming or outgoing)
     *
     * @param chatId the chatId
     * @param direction the direction
     * @param transferId the message ID
     */
    void onNewGroupFileTransfer(String chatId, Direction direction, String transferId);

    /**
     * Take into account that a file transfer is read
     *
     * @param transferId the message ID
     */
    void onReadFileTransfer(String transferId);

    /**
     * Take into account deletion of file transfer
     *
     * @param contact
     * @param transferIds the messages ID
     */
    void onDeleteFileTransfer(ContactId contact, Set<String> transferIds);

    /**
     * Take into account deletion of group file transfer
     *
     * @param chatId
     * @param transferIds the messages ID
     */
    void onDeleteGroupFileTransfer(String chatId, Set<String> transferIds);
}
