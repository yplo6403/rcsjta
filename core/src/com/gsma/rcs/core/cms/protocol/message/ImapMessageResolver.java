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

package com.gsma.rcs.core.cms.protocol.message;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.exception.CmsSyncException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMessageNotSupportedException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.protocol.message.cpim.CpimMessage;
import com.gsma.rcs.core.cms.protocol.message.cpim.text.TextCpimBody;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

public class ImapMessageResolver {

    private static final Logger sLogger = Logger.getLogger(ImapMessageResolver.class
            .getSimpleName());

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     */
    public ImapMessageResolver(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
    }

    public MessageType resolveType(com.gsma.rcs.imaplib.imap.ImapMessage imapMessage)
            throws CmsSyncMissingHeaderException, CmsSyncMessageNotSupportedException {

        String messageContext = imapMessage.getBody().getHeader(Constants.HEADER_MESSAGE_CONTEXT);
        if (messageContext != null) {
            messageContext = messageContext.toLowerCase();
            if (Constants.PAGER_MESSAGE.toLowerCase().equals(messageContext)) { // SMS legacy
                                                                                // message
                return MessageType.SMS;
            } else if (Constants.MULTIMEDIA_MESSAGE.toLowerCase().equals(messageContext)) { // MMS
                                                                                            // legacy
                                                                                            // message
                return MessageType.MMS;
            }
        }

        String imapContentType = imapMessage.getBody().getHeader(Constants.HEADER_CONTENT_TYPE);
        if (imapContentType == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTENT_TYPE
                    + " IMAP header is missing");
        }
        imapContentType = imapContentType.toLowerCase();
        if (Constants.MESSAGE_CPIM.toLowerCase().equals(imapContentType)) {

            String rawBody = imapMessage.getTextBody();
            if (rawBody.isEmpty()) { // we have only imap headers -->not able to determine more
                                     // precisely the type of the message
                return MessageType.MESSAGE_CPIM;
            }

            CpimMessage cpimMessage = new CpimMessage(new HeaderPart(), new TextCpimBody());
            cpimMessage.parsePayload(rawBody);
            String contentType = cpimMessage.getContentType();
            if (contentType == null) {
                throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTENT_TYPE
                        + " Cpim header is missing");
            }

            if (contentType.toLowerCase().contains(Constants.CONTENT_TYPE_TEXT_PLAIN.toLowerCase())) {
                return MessageType.CHAT_MESSAGE;
            } else if (contentType.toLowerCase().contains(
                    Constants.CONTENT_TYPE_MESSAGE_IMDN_XML.toLowerCase())) {
                return MessageType.IMDN;
            } else if (contentType.toLowerCase().contains(
                    FileTransferHttpInfoDocument.MIME_TYPE.toLowerCase())) {
                return MessageType.FILE_TRANSFER;
            }
            throw new CmsSyncMessageNotSupportedException(
                    "unsupported cpim message type : ".concat(contentType));

        } else if (imapContentType.contains(Constants.APPLICATION_CPM_SESSION.toLowerCase())) {
            return MessageType.CPM_SESSION;
        } else if (imapContentType.contains(Constants.APPLICATION_GROUP_STATE.toLowerCase())) {
            return MessageType.GROUP_STATE;
        }
        // } else if (imapContentType.contains(Constants.APPLICATION_FILE_TRANSFER.toLowerCase())) {
        // return MessageType.FILE_TRANSFER;
        // }

        StringBuilder msg = new StringBuilder("Can not determine the type of the message").append(
                Constants.CRLF).append(imapMessage.getPayload());
        throw new CmsSyncMessageNotSupportedException(msg.toString());
    }

    public IImapMessage resolveMessage(MessageType messageType,
            com.gsma.rcs.imaplib.imap.ImapMessage imapMessage) throws CmsSyncException {

        switch (messageType) {
            case SMS:
                return new ImapSmsMessage(imapMessage);
            case MMS:
                return new ImapMmsMessage(imapMessage);
            case MESSAGE_CPIM:
                return new ImapCpimMessage(imapMessage);
            case CHAT_MESSAGE:
                return new ImapChatMessage(imapMessage);
            case IMDN:
                return new ImapImdnMessage(imapMessage);
            case FILE_TRANSFER:
                return new ImapFileTransferMessage(mRcsSettings, imapMessage);
            case CPM_SESSION:
                return new ImapCpmSessionMessage(mRcsSettings, imapMessage);
            case GROUP_STATE:
                return new ImapGroupStateMessage(mRcsSettings, imapMessage);

        }
        throw new CmsSyncMessageNotSupportedException(
                "unsupported message type : ".concat(messageType.toString()));
    }
}
