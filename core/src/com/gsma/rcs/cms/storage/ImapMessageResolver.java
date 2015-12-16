
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
