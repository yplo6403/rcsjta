package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.sync.strategy.FlagChange;

import java.util.Set;

/**
 * LocalStorageMessageHandler interface
 *
 */
public interface ILocalEventHandler {
    
    /**
     * @param folder
     * @return Set<FlagChange>
     */
    Set<FlagChange> getLocalEvents(String folder);
    
    void finalizeLocalEvents(MessageType messageType, String messageId, boolean seenEvent, boolean deleteEvent);
}
