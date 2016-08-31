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
import com.gsma.rcs.core.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMessageNotSupportedException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.protocol.message.cpim.CpimMessage;
import com.gsma.rcs.core.cms.protocol.message.cpim.text.TextCpimBody;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.imaplib.imap.Part;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.contact.ContactId;

public class ImapMessageResolver {

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     */
    public ImapMessageResolver(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
    }

    public MessageType resolveType(com.gsma.rcs.imaplib.imap.ImapMessage imapMessage)
            throws CmsSyncMissingHeaderException, CmsSyncMessageNotSupportedException,
            CmsSyncHeaderFormatException {
        Part imapHeaders = imapMessage.getBody();
        String messageContext = imapHeaders.getHeader(Constants.HEADER_MESSAGE_CONTEXT);
        if (messageContext != null) {
            messageContext = messageContext.toLowerCase();
            if (Constants.PAGER_MESSAGE.equals(messageContext)) {
                // SMS legacy message
                return MessageType.SMS;
            }
        }
        String imapContentType = imapHeaders.getHeader(Constants.HEADER_CONTENT_TYPE);
        if (imapContentType == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTENT_TYPE
                    + " IMAP header is missing");
        }
        imapContentType = imapContentType.toLowerCase();
        if (Constants.MESSAGE_CPIM.equals(imapContentType)) {
            String conversationId = imapHeaders.getHeader(Constants.HEADER_CONVERSATION_ID);
            if (Constants.MULTIMEDIA_MESSAGE.equals(conversationId)) {
                // MMS legacy message
                return MessageType.MMS;
            }
            String rawBody = imapMessage.getTextBody();
            if (rawBody.isEmpty()) {
                /*
                 * we have only IMAP headers -->not able to determine more precisely the type of the
                 * message.
                 */
                return MessageType.MESSAGE_CPIM;
            }
            CpimMessage cpimMessage = new CpimMessage(new HeaderPart(), new TextCpimBody());
            cpimMessage.parsePayload(rawBody);
            String contentType = cpimMessage.getContentType();
            if (contentType == null) {
                throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTENT_TYPE
                        + " CPIM header is missing");
            }
            if (contentType.toLowerCase().contains(Constants.CONTENT_TYPE_TEXT_PLAIN)) {
                return MessageType.CHAT_MESSAGE;

            } else if (contentType.toLowerCase().contains(Constants.CONTENT_TYPE_MESSAGE_IMDN_XML)) {
                return MessageType.IMDN;

            } else if (contentType.toLowerCase().contains(FileTransferHttpInfoDocument.MIME_TYPE)) {
                return MessageType.FILE_TRANSFER;
            }
            throw new CmsSyncMessageNotSupportedException(
                    "unsupported CPIM message type : ".concat(contentType));

        } else if (imapContentType.contains(Constants.APPLICATION_CPM_SESSION)) {
            return MessageType.CPM_SESSION;

        } else if (imapContentType.contains(Constants.APPLICATION_GROUP_STATE)) {
            return MessageType.GROUP_STATE;
        }
        // } else if (imapContentType.contains(Constants.APPLICATION_FILE_TRANSFER.toLowerCase())) {
        // return MessageType.FILE_TRANSFER;
        // }
        throw new CmsSyncMessageNotSupportedException("Can not determine the type of the message"
                + Constants.CRLF + imapMessage.getPayload());
    }

    /**
     * Create a terminal IMAP message from raw IMAP message
     * 
     * @param messageType the message type
     * @param imapMessage the raw IMAP message
     * @param remote the remote contact or null for group conversation
     * @param headerOnly true if imapMessage only contains IMAP header
     * @return the terminal IMAP message instance
     * @throws CmsSyncException
     */
    public IImapMessage resolveMessage(MessageType messageType,
            com.gsma.rcs.imaplib.imap.ImapMessage imapMessage, ContactId remote, boolean headerOnly)
            throws CmsSyncException {
        switch (messageType) {
            case SMS:
                ImapSmsMessage imapSmsMessage = new ImapSmsMessage(imapMessage, remote);
                if (!headerOnly) {
                    imapSmsMessage.parseBody();
                }
                return imapSmsMessage;

            case MMS:
                ImapMmsMessage imapMmsMessage = new ImapMmsMessage(imapMessage, remote);
                if (!headerOnly) {
                    imapMmsMessage.parseBody();
                }
                return imapMmsMessage;

            case MESSAGE_CPIM:
                ImapCpimMessage imapCpimMessage = new ImapCpimMessage(imapMessage, remote);
                imapCpimMessage.parseBody();
                return imapCpimMessage;

            case CHAT_MESSAGE:
                ImapChatMessage imapChatMessage = new ImapChatMessage(imapMessage, remote);
                imapChatMessage.parseBody();
                return imapChatMessage;

            case IMDN:
                ImapImdnMessage imapImdnMessage = new ImapImdnMessage(imapMessage, remote);
                imapImdnMessage.parseBody();
                return imapImdnMessage;

            case FILE_TRANSFER:
                ImapFileTransferMessage imapFileTransferMessage = new ImapFileTransferMessage(
                        mRcsSettings, imapMessage, remote);
                imapFileTransferMessage.parseBody();
                return imapFileTransferMessage;

            case CPM_SESSION:
                ImapCpmSessionMessage imapCpmSessionMessage = new ImapCpmSessionMessage(
                        mRcsSettings, imapMessage);
                if (!headerOnly) {
                    imapCpmSessionMessage.parseBody();
                }
                return imapCpmSessionMessage;

            case GROUP_STATE:
                ImapGroupStateMessage imapGroupStateMessage = new ImapGroupStateMessage(
                        mRcsSettings, imapMessage);
                if (!headerOnly) {
                    imapGroupStateMessage.parseBody();
                }
                return imapGroupStateMessage;
        }
        throw new CmsSyncMessageNotSupportedException(
                "unsupported message type : ".concat(messageType.toString()));
    }
}
