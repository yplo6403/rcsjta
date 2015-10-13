package com.gsma.rcs.cms.storage;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.imap.message.sms.ImapSmsMessage;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;

import com.sonymobile.rcs.imap.ImapMessage;

public class ImapMessageResolver {
        
    public ImapMessageResolver(){
        
    }
    
    public MessageType resolveType(ImapMessage imapMessage){
                
        String value = imapMessage.getBody().getHeader(Constants.HEADER_MESSAGE_CONTEXT);
        MessageType messageType = null;
        if (value != null) {
            if (Constants.PAGER_MESSAGE.equals(value)) { // SMS legacy message
                messageType =  MessageType.SMS;                
            } else if (Constants.MULTIMEDIA_MESSAGE.equals(value)) { // MMS legacy message
                messageType = MessageType.MMS;
            }
        } else { // 1To1 GC
                 // TODO TO be completed
            String conversationId = imapMessage.getBody().getHeader(Constants.HEADER_CONVERSATION_ID);
            messageType =  MessageType.ONETOONE;            
        }
        return messageType;    
    }
    
    public IImapMessage resolveMessage(MessageType messageType, ImapMessage imapMessage){
        
        switch(messageType){
            case SMS:
                return new ImapSmsMessage(imapMessage);
        }        
        throw new RuntimeException("unsupported message type : ".concat(messageType.toString()));        
    }
}
