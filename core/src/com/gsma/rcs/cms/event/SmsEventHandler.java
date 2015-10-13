
package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.imap.message.sms.ImapSmsMessage;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.storage.MessageDataConverter;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.Flag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class SmsEventHandler implements IRemoteEventHandler, ILocalEventHandler, INativeSmsEventListener, IRcsSmsEventListener {

    private static final Logger sLogger = Logger.getLogger(SmsEventHandler.class.getSimpleName());
    
    private XmsLog mXmsLog;
    private ImapLog mImapLog;
    
    /**
     * Default constructor
     * @param imapLog 
     * @param xmsLog 
     */
    public SmsEventHandler(ImapLog imapLog, XmsLog xmsLog) {
        mXmsLog = xmsLog;
        mImapLog = imapLog;
    }

    @Override
    public void onRemoteReadEvent(String messageId) {
        if(sLogger.isActivated()){
            sLogger.debug("onRemoteReadEvent"); 
         }
        mXmsLog.updateReadStatusdWithBaseId(messageId, ReadStatus.READ);
    }

    @Override
    public void onRemoteDeleteEvent(String messageId) {   
        if(sLogger.isActivated()){
            sLogger.debug("onRemoteDeleteEvent"); 
         }
        //Two choices : 
        // mark message as deleted in local storage
        // remove definitively the message from local storage
        //mXmsLog.updateDeleteStatusdWithBaseId(messageId, DeleteStatus.DELETED);
        mXmsLog.deleteMessage(messageId);
    }

    @Override
    public String getMessageId(IImapMessage message) {
        
        // check if an entry already exist in imapData provider
        MessageData messageData = mImapLog.getMessage(message.getFolder(), message.getUid());
        if(messageData!=null){
            sLogger.error(new StringBuilder("This message should not be present in local storage : ").append(message.getFolder()).append(",").append(message.getUid()).toString());
            return messageData.getMessageId();
        }
        
        // get messages from provider with contact, direction and correlator fields
        // messages are sorted by Date DESC (more recent first).
        SmsData smsData = MessageDataConverter.convertIntoSmsData((ImapSmsMessage)message);        
        List<String> ids =  mXmsLog.getBaseIds(smsData.getContact(),smsData.getDirection(), smsData.getMessageCorrelator());
        
        // take the first message which s not synchronized with CMS server (have no uid)
        for(String id : ids){
            Integer uid = mImapLog.getUid(Constants.TEL_PREFIX.concat(smsData.getContact()), id);
            if(uid==null){
                return id;
            }
        }         
        return null;
    }

    @Override
    public String onRemoteNewMessage(IImapMessage message) {
        if(sLogger.isActivated()){
            sLogger.debug("onRemoteNewMessage"); 
         }
            SmsData smsData = MessageDataConverter.convertIntoSmsData((ImapSmsMessage)message);
            String messageId =  mXmsLog.addMessage(smsData);
            // TODO FGI : could be optimized :
            // we have to create the message in the xms content provider to retrieve its id used in imap content provider.
            // and just after this message is deleted 
            if(message.isDeleted()){ 
                mXmsLog.deleteMessage(messageId);
            }            
            return messageId;
     }

    @Override
    public void onIncomingSms(SmsData message) {
        if(sLogger.isActivated()){
            sLogger.debug("onIncomingSms" + message.toString());
        }
        //add message in provider
        String baseId = mXmsLog.addMessage(message);
        message.setBaseId(baseId);        
    }

    @Override
    public void onOutgoingSms(SmsData message) {
        if(sLogger.isActivated()){
            sLogger.debug("onOutgoingSms" + message.toString());
        }        
        //add message in provider
        String baseId = mXmsLog.addMessage(message);
        message.setBaseId(baseId);        
    }

    @Override
    public void onDeliverNativeSms(long nativeProviderId, long sentDate) {
        if(sLogger.isActivated()){
            sLogger.debug("onLocalDeliveredSms" + nativeProviderId + ";" + sentDate);
        }
        sLogger.info("onDeliverNativeSms" + nativeProviderId + ";" + sentDate);
        mXmsLog.setMessageAsDeliveredWithNativeProviderId(String.valueOf(nativeProviderId), sentDate);
    }

    @Override
    public void onReadNativeSms(long nativeProviderId) {
        if(sLogger.isActivated()){
            sLogger.debug("onReadNativeSms" + nativeProviderId );
        }
        mXmsLog.updateReadStatusWithNativeProviderId(nativeProviderId, ReadStatus.READ_REQUESTED);
    }

    @Override
    public void onDeleteNativeSms(long nativeProviderId) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteNativeSms" + nativeProviderId );
        }
        mXmsLog.updateDeleteStatusWithNativeProviderId(nativeProviderId, DeleteStatus.DELETED_REQUESTED);
    }

    @Override
    public void onReadRcsConversation(String contact) {
        if(sLogger.isActivated()){
            sLogger.debug("onReadRcsConversation" + contact );
        }
        mXmsLog.updateReadStatus(contact, ReadStatus.READ_REQUESTED);
    }

    @Override
    public void onDeleteRcsSms(String contact, String id) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteRcsSms " + id );
        }
        mXmsLog.updateDeleteStatusdWithBaseId(id, DeleteStatus.DELETED_REQUESTED);
    }

    @Override
    public void onDeleteRcsConversation(String contact) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteRcsConversation" + contact );
        }
        mXmsLog.updateDeleteStatus(contact, DeleteStatus.DELETED_REQUESTED);
    }

    @Override
    public void onReadRcsMessage(String contact, String baseId) {
        if(sLogger.isActivated()){
            sLogger.debug("onReadRcsMessage" + baseId );
        }
        mXmsLog.updateReadStatusdWithBaseId(baseId, ReadStatus.READ_REQUESTED);        
    }

    @Override
    public void onDeleteNativeConversation(String contact) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteNativeConversation" + contact );
        }
        mXmsLog.updateDeleteStatus(contact, DeleteStatus.DELETED_REQUESTED);
    }
    
    @Override
    public int getPriority() {
        return PRIORITY_HIGH;
    }

    @Override
    public int compareTo(INativeSmsEventListener another) {
        return another.getPriority() - getPriority();
    }

    @Override
    public Set<FlagChange> getLocalEvents(String folder) {        
        List<SmsData> messages;        
        String contact = folder;
        if(folder.startsWith(Constants.TEL_PREFIX)){
            contact = folder.substring(Constants.TEL_PREFIX.length());
        }            
        messages = mXmsLog.getMessages(contact, ReadStatus.READ_REQUESTED, DeleteStatus.DELETED_REQUESTED);
         
        Set<FlagChange> changes = new HashSet<FlagChange>();
        for (SmsData sms :  messages) {
            Integer uid = mImapLog.getUid(folder,sms.getBaseId());
            if(uid!=null){
                Set<Flag> flags = new HashSet<Flag>();
                if(ReadStatus.READ_REQUESTED == sms.getReadStatus()){
                    flags.add(Flag.Seen);
                }
                if(DeleteStatus.DELETED_REQUESTED == sms.getDeleteStatus()){
                    flags.add(Flag.Deleted);
                }
                changes.add(new FlagChange(folder,uid,flags));
            }
        }
        return changes;
    }

    @Override
    public void finalizeLocalEvents(String messageId, boolean seenEvent, boolean deleteEvent) {
        
        if(deleteEvent){
            mXmsLog.deleteMessage(messageId);
        }
        else if(seenEvent){
            mXmsLog.updateReadStatusdWithBaseId(messageId, ReadStatus.READ);
        }            
    }
}
