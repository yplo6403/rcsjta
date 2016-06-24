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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncException;
import com.gsma.rcs.core.cms.protocol.message.IImapMessage;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Interface used to take into account remote changes from CMS server. These changes shall be apply
 * in local storage
 */
public interface CmsEventListener {

    /**
     * Take into account a read flag event from CMS server
     *
     * @param cmsObject the CMS object
     */
    void onRemoteReadEvent(CmsObject cmsObject);

    /**
     * Take into account a deleted flag event from CMS server
     *
     * @param cmsObject the CMS object
     */
    void onRemoteDeleteEvent(CmsObject cmsObject);

    /**
     * Create new message in local storage
     *
     * @param messageType the message type
     * @param message the IMAP message
     * @param remote the remote contact or null if group conversation
     * @return an array of strings with first element set to the message ID and optional second
     *         element set to the chat ID
     * @throws CmsSyncException
     * @throws FileAccessException
     */
    String[] onRemoteNewMessage(MessageType messageType, IImapMessage message, ContactId remote)
            throws CmsSyncException, FileAccessException;

    /**
     * This method checks if the message is already present in local storage. Comparison between
     * local and remote storage is based on message headers
     *
     * @param messageType the message type
     * @param message the IMAP message
     * @return the CMS object
     * @throws CmsSyncException
     */
    CmsObject searchLocalMessage(MessageType messageType, IImapMessage message)
            throws CmsSyncException;
}
