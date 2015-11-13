
package com.gsma.rcs.cms.storage;

import com.gsma.rcs.cms.event.ILocalEventHandler;
import com.gsma.rcs.cms.event.IRemoteEventHandler;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.provider.imap.FolderData;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.sync.ISynchronizationHandler;
import com.gsma.rcs.cms.sync.strategy.FlagChange;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * LocalStorage implementation
 *
 */
public class LocalStorage implements ISynchronizationHandler{

    private static LocalStorage sInstance;
    
    protected ImapLog mImapLog;
    //protected XmsLog mXmsLog;
    private ImapMessageResolver mMessageResolver;

    /*package private*/ Map<MessageType, IRemoteEventHandler> mRemoteEventHandlers = new HashMap<MessageType, IRemoteEventHandler>();
    /*package private*/ Map<MessageType, ILocalEventHandler> mLocalEventHandlers = new HashMap<MessageType, ILocalEventHandler>();
    
    private LocalStorage(ImapLog imapLog){
        mImapLog = imapLog;  
        mMessageResolver = new ImapMessageResolver();         
    }
    
    /**
     * @param imapLog
     * @return LocalStorage
     */
    public static LocalStorage createInstance(ImapLog imapLog) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (LocalStorage.class) {
            if (sInstance == null) {
                sInstance = new LocalStorage(imapLog);
            }
        }
        return sInstance;
    }
    
    /**
     * @param messageType
     * @param handler
     */
    public void registerRemoteEventHandler(MessageType messageType, IRemoteEventHandler handler){
        mRemoteEventHandlers.put(messageType, handler);
    }
    
    /**
     * @param messageType
     */
    public void unregisterRemoteEventHandler(MessageType messageType){
        mRemoteEventHandlers.remove(messageType);
    }
    
    /**
     * @param messageType
     * @param handler
     */
    public void addLocalEventHandler(MessageType messageType, ILocalEventHandler handler){
        mLocalEventHandlers.put(messageType, handler);
    }

    /**
     * Return the local folders from content provider
     * 
     * @return map of folder
     */
    public Map<String, FolderData> getLocalFolders() {
        Map<String, FolderData> localFolders = mImapLog.getFolders();
        Iterator<Entry<String, FolderData>> iter = localFolders.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, FolderData> entry = iter.next();
            Integer maxUid = mImapLog.getMaxUidForMessages(entry.getKey());
            entry.getValue().setMaxUid(maxUid);
        }
        return localFolders;
    }

    /**
     * Apply folder change
     * 
     * @param folder
     */
    public void applyFolderChange(FolderData folder) {
        mImapLog.addFolder(folder);
    }

    @Override
    public void applyFlagChange(List<FlagChange> flagchanges) {
        Iterator<FlagChange> iter = flagchanges.iterator();
        while (iter.hasNext()) {
            FlagChange fg = iter.next();
            String folder = fg.getFolder();
            boolean deleteFlag = fg.addDeletedFlag();
            boolean seenFlag = fg.addSeenFlag();
            for(Integer uid : fg.getUids() ){
                MessageData msg = mImapLog.getMessage(folder, uid);
                MessageType messageType = msg.getMessageType();
                IRemoteEventHandler storageHandler = mRemoteEventHandlers.get(messageType);
                if (deleteFlag) {
                    msg.setDeleted(true);
                    storageHandler.onRemoteDeleteEvent(messageType, msg.getMessageId());
                }
                if (seenFlag) {
                    msg.setSeen(true);
                    storageHandler.onRemoteReadEvent(messageType, msg.getMessageId());
                }
                mImapLog.addMessage(msg);
            }
        }
    }
   
    @Override
    public void removeLocalFolder(String folderName) {
        mImapLog.removeFolder(folderName, true);
    }

    @Override
    public void updateLocalFolder(FolderData folder) {
        mImapLog.addFolder(folder);        
    }

    @Override
    public Set<Integer> filterNewMessages(List<ImapMessage> messages) {
        Set<Integer> uids = new TreeSet<Integer>();        
        for(ImapMessage msg : messages){
            MessageType messageType = mMessageResolver.resolveType(msg);
            IImapMessage resolvedMessage = mMessageResolver.resolveMessage(messageType,msg);
            if(resolvedMessage == null){
                continue;
            }
            IRemoteEventHandler remoteEventHandler = mRemoteEventHandlers.get(messageType); 
            String messageId = remoteEventHandler.getMessageId(messageType, resolvedMessage);
            if(messageId == null){ // message not present in local storage
                uids.add(msg.getUid());    
            } 
            else{
                
                // update flag for local message
                boolean isSeen = msg.getMetadata().getFlags().contains(Flag.Seen);
                boolean isDeleted = msg.getMetadata().getFlags().contains(Flag.Deleted);
                if(isDeleted){
                    remoteEventHandler.onRemoteDeleteEvent(messageType, messageId);
                }
                if(isSeen){
                    remoteEventHandler.onRemoteReadEvent(messageType, messageId);
                }
                
                MessageData messageData = new MessageData(msg.getFolderPath(), 0, msg.getUid(),
                        isSeen, isDeleted, messageType, messageId);
                mImapLog.addMessage(messageData);
            }
        }
     return uids;   
    }

    @Override
    public void createMessages(List<ImapMessage> messages) {
        for (ImapMessage msg : messages) {

            MessageType messageType = mMessageResolver.resolveType(msg);
            IImapMessage resolvedMessage = mMessageResolver.resolveMessage(messageType,msg);
            if(resolvedMessage == null){
                continue;
            }
            String messageId = mRemoteEventHandlers.get(messageType).onRemoteNewMessage(messageType, resolvedMessage);
            
            MessageData messageData = new MessageData(resolvedMessage.getFolder(), 0, resolvedMessage.getUid(),
                    resolvedMessage.isSeen(),resolvedMessage.isDeleted(),
                    messageType, messageId);
            mImapLog.addMessage(messageData);
        }
    }

    @Override
    public List<FlagChange> getLocalFlagChanges(String folder) {
        List<FlagChange> changes = new ArrayList<>();
        for (ILocalEventHandler handler : mLocalEventHandlers.values()) {
            changes.addAll(handler.getLocalEvents(folder));
        }
        return changes;
    }
    
    /**
     * @param flagChanges
     */
    public void finalizeLocalFlagChanges(List<FlagChange> flagChanges){
        Iterator<FlagChange> iter = flagChanges.iterator();
        while (iter.hasNext()) {
            FlagChange fg = iter.next();
            boolean seenFlag = fg.addSeenFlag();
            boolean deleteFlag= fg.addDeletedFlag();
            for(Integer uid : fg.getUids()){
                MessageData msg = mImapLog.getMessage(fg.getFolder(), uid);
                MessageType messageType = msg.getMessageType();
                ILocalEventHandler storageHandler = mLocalEventHandlers.get(messageType);
                storageHandler.finalizeLocalEvents(messageType, msg.getMessageId(), seenFlag, deleteFlag);
            }
        }
    }
}
