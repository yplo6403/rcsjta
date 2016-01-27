//package com.gsma.rcs.cms.storage;
//
//import com.gsma.rcs.cms.event.IRemoteEventHandler;
//import com.gsma.rcs.cms.provider.imap.FolderData;
//import com.gsma.rcs.cms.provider.imap.ImapLog;
//import com.gsma.rcs.cms.provider.imap.MessageData;
//import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
//import com.gsma.rcs.provider.LocalContentResolver;
//
//import java.util.List;
//
//public class LocalStorageImplMock extends LocalStorage {
//
//    private static final String NEW_LINE = "\n";
//    
//    public LocalStorageImplMock(ImapLog imapLog) {
//        super(imapLog);
//    }
//
//    public void setStorageHandler(MessageType messageType, IRemoteEventHandler storageHandler){
//        mStorageHandlers.put(messageType, storageHandler);
//    }
//    
//    public IRemoteEventHandler getStorageHandler(MessageType messageType){
//        return mStorageHandlers.get(messageType);
//    }
//    
//    public ImapLog getImapLog(){
//        return mImapLog;
//    }
//    
//    public String dumpData(){  
//        StringBuilder data = new StringBuilder(NEW_LINE);
//        data.append(">>> Imap Provider").append(NEW_LINE);
//        data.append(">>> >>> folderdata table").append(NEW_LINE);
//        for(FolderData folder : mImapLog.getFolders().values()){
//            data.append(folder.toString()).append(NEW_LINE);    
//        }        
//        data.append(">>> >>> messagedata table").append(NEW_LINE);
//        for(MessageData msg : mImapLog.getMessages().values()){
//            data.append(msg.toString()).append(NEW_LINE);    
//        }
//        data.append("<<< Imap Provider").append(NEW_LINE).append(NEW_LINE);
//        data.append(">>> Sms provider (memory implementation)").append(NEW_LINE);        
//        for(String msg : getSmsMessages()){
//            data.append(msg.toString()).append(NEW_LINE);
//        }
//        data.append("<<< Sms provider (memory implementation)").append(NEW_LINE);
//        
//        return data.toString();
//    }
//    
//    private List<String> getSmsMessages(){
//        return SmsStorageHandlerMock.getMessages();
//    }
//
// }
