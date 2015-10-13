package com.gsma.rcs.cms.event;

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
    public Set<FlagChange> getLocalEvents(String folder);
    
    public void finalizeLocalEvents(String messageId, boolean seenEvent, boolean deleteEvent);
}
