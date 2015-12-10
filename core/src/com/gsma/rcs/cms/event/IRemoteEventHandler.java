package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;

/**
 * LocalStorageMessageHandler interface
 *
 */
public interface IRemoteEventHandler {

    /**
     * Take into account a read flag event from CMS server
     * @param messageId
     */
    void onRemoteReadEvent(MessageType messageType, String messageId);
    
    /**
     * Take into account a deleted flag event from CMS server
     * @param messageId
     */    
    void onRemoteDeleteEvent(MessageType messageType, String messageId);
        
    /**
     * Create new message in local storage
     * @param message
     * @return messageId
     */
    String onRemoteNewMessage(MessageType messageType, IImapMessage message) throws ImapHeaderFormatException;
    
    /**
     * This method checks if the message is already present in local storage.
     * Comparison between local and remote storage is based on message headers 
     * @param message
     * @return messageId or null if the message is not present in local storage
     */
    String getMessageId(MessageType messageType, IImapMessage message) throws ImapHeaderFormatException;
}
