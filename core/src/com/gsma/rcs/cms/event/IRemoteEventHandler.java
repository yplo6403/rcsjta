package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.imap.message.IImapMessage;

/**
 * LocalStorageMessageHandler interface
 *
 */
public interface IRemoteEventHandler {

    /**
     * Take into account a read flag event from CMS server
     * @param messageId
     */
    public void onRemoteReadEvent(String messageId);
    
    /**
     * Take into account a deleted flag event from CMS server
     * @param messageId
     */    
    public void onRemoteDeleteEvent(String messageId);
        
    /**
     * Create new message in local storage
     * @param message
     * @return messageId
     */
    public String onRemoteNewMessage(IImapMessage message);
    
    /**
     * This method checks if the message is already present in local storage.
     * Comparison between local and remote storage is based on message headers 
     * @param message
     * @return messageId or null if the message is not present in local storage
     */
    public String getMessageId(IImapMessage message);
}
