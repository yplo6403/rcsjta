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

package com.gsma.rcs.cms.storage;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.imap.message.ImapMmsMessage;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.ImapMessage;

public class ImapMessageResolver {

    private static final Logger sLogger = Logger.getLogger(ImapMessageResolver.class
            .getSimpleName());

    /**
     * Constructor
     */
    public ImapMessageResolver() {
    }

    public MessageType resolveType(ImapMessage imapMessage) {

        String value = imapMessage.getBody().getHeader(Constants.HEADER_MESSAGE_CONTEXT);
        MessageType messageType = null;
        if (value != null) {
            if (Constants.PAGER_MESSAGE.equals(value)) { // SMS legacy message
                messageType = MessageType.SMS;
            } else if (Constants.MULTIMEDIA_MESSAGE.equals(value)) { // MMS legacy message
                messageType = MessageType.MMS;
            }
        } else { // 1To1 GC
                 // TODO FGI TO be completed
            String conversationId = imapMessage.getBody().getHeader(
                    Constants.HEADER_CONVERSATION_ID);
            messageType = MessageType.ONETOONE;
        }
        return messageType;
    }

    public IImapMessage resolveMessage(MessageType messageType, ImapMessage imapMessage) {

        switch (messageType) {
            case SMS:
                return new ImapSmsMessage(imapMessage);
            case MMS:
                return new ImapMmsMessage(imapMessage);
        }
        if (sLogger.isActivated()) {
            sLogger.warn("unsupported message type : ".concat(messageType.toString()));
        }
        return null;
    }
}
