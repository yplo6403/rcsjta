
package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.imap.message.ImapMmsMessage;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.imap.message.mime.MmsMimeMessage;
import com.gsma.rcs.cms.imap.message.mime.MultiPart;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.xms.PartLog;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.MmsPart;
import com.gsma.rcs.cms.provider.xms.model.XmsData;
import com.gsma.rcs.cms.provider.xms.model.XmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.XmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.XmsData.MimeType;
import com.gsma.rcs.cms.provider.xms.model.MmsData;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.storage.MessageDataConverter;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.cms.utils.MmsUtils;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.logger.Logger;

import com.gsma.services.rcs.RcsService;
import com.sonymobile.rcs.imap.Flag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class XmsEventHandler implements IRemoteEventHandler, ILocalEventHandler, INativeXmsEventListener, RcsXmsEventListener {

    private static final Logger sLogger = Logger.getLogger(XmsEventHandler.class.getSimpleName());
    
    private XmsLog mXmsLog;
    private PartLog mPartLog;
    private ImapLog mImapLog;
    
    /**
     * Default constructor
     * @param imapLog 
     * @param xmsLog 
     */
    public XmsEventHandler(ImapLog imapLog, XmsLog xmsLog, PartLog partLog) {
        mXmsLog = xmsLog;
        mPartLog = partLog;
        mImapLog = imapLog;
    }

    @Override
    public void onRemoteReadEvent(MessageType messageType, String messageId) {
        if(sLogger.isActivated()){
            sLogger.debug("onRemoteReadEvent"); 
         }
        mXmsLog.updateReadStatusdWithBaseId(messageId, ReadStatus.READ);
    }

    @Override
    public void onRemoteDeleteEvent(MessageType messageType, String messageId) {
        if(sLogger.isActivated()){
            sLogger.debug("onRemoteDeleteEvent"); 
         }
        //Two choices : 
        // mark message as deleted in local storage
        // remove definitively the message from local storage
        //mXmsLog.updateDeleteStatusdWithBaseId(messageId, DeleteStatus.DELETED);
        mXmsLog.deleteMessage(messageId);
        if(MessageType.MMS == messageType){
            mPartLog.deleteParts(messageId);
        }
    }

    @Override
    public String getMessageId(MessageType messageType, IImapMessage message) {
        
        // check if an entry already exist in imapData provider
        MessageData messageData = mImapLog.getMessage(message.getFolder(), message.getUid());
        if(messageData!=null){
            sLogger.error(new StringBuilder("This message should not be present in local storage : ").append(message.getFolder()).append(",").append(message.getUid()).toString());
            return messageData.getMessageId();
        }

        if(MessageType.SMS == messageType){
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
        else if(MessageType.MMS == messageType){
            String messageId = ((ImapMmsMessage)message).getRawMessage().getBody().getHeader(Constants.HEADER_MESSAGE_ID);
            MmsData mmsData =  mXmsLog.getMessage(messageId);
            return (mmsData==null ? null : mmsData.getBaseId());
        }
        return null;
    }

    @Override
    public String onRemoteNewMessage(MessageType messageType, IImapMessage message) {
        if(sLogger.isActivated()){
            sLogger.debug("onRemoteNewMessage");
         }

        if(message.isDeleted()){
            SmsData dummySms = new SmsData(null,null,"dummyContact", null, 0, RcsService.Direction.INCOMING, ReadStatus.READ);
            String messageId =  mXmsLog.addSms(dummySms);
            mXmsLog.deleteMessage(messageId);
            return messageId;
        }

        if(MessageType.SMS == messageType){
            SmsData smsData = MessageDataConverter.convertIntoSmsData((ImapSmsMessage)message);
            return mXmsLog.addSms(smsData);
        }
        else if(MessageType.MMS == messageType){
            ImapMmsMessage imapMmsMessage = (ImapMmsMessage)message;
            MmsData mmsData = MessageDataConverter.convertIntoMmsData(imapMmsMessage);
            List<MmsPart> parts = new ArrayList<>();
            MmsMimeMessage mmsMimeMessage = (MmsMimeMessage)imapMmsMessage.getPart();
            String textContent=null;
            for(MultiPart multipart : mmsMimeMessage.getMimebody().getMultiParts()){
                String contentType = multipart.getContentType();
                String path = null;
                String text = null;
                if(MmsUtils.CONTENT_TYPE_IMAGE.contains(contentType)){
                    byte[] data;
                    if(Constants.HEADER_BASE64.equals(multipart.getContentTransferEncoding())){
                        data = Base64.decodeBase64(multipart.getContent().getBytes());
                    }
                    else{
                        data = multipart.getContent().getBytes();
                    }
                    path = MmsUtils.saveContent(contentType, multipart.getContentId(), data);
                }
                else if(Constants.CONTENT_TYPE_TEXT.equals(contentType)){
                    text = multipart.getContent();
                    textContent = text;
                }
                parts.add(new MmsPart(
                        null,
                        null,
                        contentType,
                        multipart.getContentId(),
                        path,
                        text));
            }
            mmsData.setContent(textContent);
            mPartLog.addParts(mmsData.getMmsId(), parts);
            return mXmsLog.addMms(mmsData);
        }
        return null;
     }

    /***********************************************************************/
    /******************         Native SMS Events         ******************/
    /***********************************************************************/
    @Override
    public void onIncomingSms(SmsData message) {
        if(sLogger.isActivated()){
            sLogger.debug("onIncomingSms " + message.toString());
        }
        String baseId = mXmsLog.addSms(message);
        message.setBaseId(baseId);        
    }

    @Override
    public void onOutgoingSms(SmsData message) {
        if(sLogger.isActivated()){
            sLogger.debug("onOutgoingSms " + message.toString());
        }
        String baseId = mXmsLog.addSms(message);
        message.setBaseId(baseId);        
    }

    @Override
    public void onDeliverNativeSms(long nativeProviderId, long sentDate) {
        if(sLogger.isActivated()){
            sLogger.debug("onLocalDeliveredSms " + nativeProviderId + ";" + sentDate);
        }
        sLogger.info("onDeliverNativeSms" + nativeProviderId + ";" + sentDate);
        mXmsLog.setMessageAsDeliveredWithNativeProviderId(MimeType.SMS, String.valueOf(nativeProviderId), sentDate);
    }

    @Override
    public void onDeleteNativeSms(long nativeProviderId) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteNativeSms " + nativeProviderId );
        }
        mXmsLog.updateSmsDeleteStatus(nativeProviderId, DeleteStatus.DELETED_REQUESTED);
    }

    /***********************************************************************/
    /******************         Native MMS Events         ******************/
    /***********************************************************************/

    @Override
    public void onIncomingMms(MmsData message) {
        if(sLogger.isActivated()){
            sLogger.debug("onIncomingMms " + message.toString());
        }
        String baseId = mXmsLog.addMms(message);
        message.setBaseId(baseId);
        mPartLog.addParts(message.getMmsId(), message.getParts());
    }

    @Override
    public void onOutgoingMms(MmsData message) {
        if(sLogger.isActivated()){
            sLogger.debug("onOutgoingMms " + message.toString());
        }
        String baseId = mXmsLog.addMms(message);
        message.setBaseId(baseId);
        mPartLog.addParts(message.getMmsId(), message.getParts());
    }

    @Override
    public void onDeleteNativeMms(String mmsId) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteNativeMms " + mmsId );
        }
        mXmsLog.updateMmsDeleteStatus( mmsId, DeleteStatus.DELETED_REQUESTED);
        mPartLog.deleteParts(mmsId);
    }

    /***********************************************************************/
    /******************       Native conversation events  ******************/
    /***********************************************************************/
    @Override
    public void onDeleteNativeConversation(long threadId) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteNativeConversation " + threadId );
        }
        mXmsLog.updateDeleteStatus(threadId, DeleteStatus.DELETED_REQUESTED);
        for(String mmsId : mXmsLog.getMmsIds(threadId)){
            mPartLog.deleteParts(mmsId);
        }
    }

    @Override
    public void onReadNativeConversation(long nativeThreadId) {
        if(sLogger.isActivated()){
            sLogger.debug("onReadNativeConversation " + nativeThreadId );
        }
        mXmsLog.updateReadStatusWithNativeThreadId(nativeThreadId, ReadStatus.READ_REQUESTED);
    }

    /***********************************************************************/
    /******************       RCS messages events         ******************/
    /***********************************************************************/

    @Override
    public void onReadRcsConversation(String contact) {
        if(sLogger.isActivated()){
            sLogger.debug("onReadRcsConversation " + contact );
        }
        mXmsLog.updateReadStatus(contact, ReadStatus.READ_REQUESTED);
    }

    @Override
    public void onDeleteRcsSms(String contact, String id) {
        if(sLogger.isActivated()){
            sLogger.debug(new StringBuilder("onDeleteRcsSms :").append(id).toString());
        }
        mXmsLog.updateDeleteStatusdWithBaseId(id, DeleteStatus.DELETED_REQUESTED);
    }

    @Override
    public void onDeleteRcsMms(String contact, String baseId, String mms_id) {
        if(sLogger.isActivated()){
            sLogger.debug(new StringBuilder("onDeleteRcsSms :").append(contact).append(",").append(mms_id).toString());
        }
        mXmsLog.updateMmsDeleteStatus(mms_id, DeleteStatus.DELETED_REQUESTED);
        mPartLog.deleteParts(mms_id);
    }

    @Override
    public void onDeleteRcsConversation(String contact) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteRcsConversation " + contact );
        }
        mXmsLog.updateDeleteStatus(contact, DeleteStatus.DELETED_REQUESTED);
        for(String mmsId : mXmsLog.getMmsIds(contact)){
            mPartLog.deleteParts(mmsId);
        }
    }

    @Override
    public void onReadRcsMessage(String contact, String baseId) {
        if(sLogger.isActivated()){
            sLogger.debug("onReadRcsMessage " + baseId );
        }
        mXmsLog.updateReadStatusdWithBaseId(baseId, ReadStatus.READ_REQUESTED);        
    }

    @Override
    public Set<FlagChange> getLocalEvents(String folder) {        
        List<XmsData> messages;
        String contact = folder;
        if(folder.startsWith(Constants.TEL_PREFIX)){
            contact = folder.substring(Constants.TEL_PREFIX.length());
        }
        Set<FlagChange> changes = new HashSet<FlagChange>();
        List<Integer> readUids = new ArrayList<>();
        List<Integer> deletedUids = new ArrayList<>();
        messages = mXmsLog.getMessages(contact, ReadStatus.READ_REQUESTED, DeleteStatus.DELETED_REQUESTED);
        for (XmsData xms :  messages) {
            Integer uid = mImapLog.getUid(folder,xms.getBaseId());
            if(uid!=null){
                if(ReadStatus.READ_REQUESTED == xms.getReadStatus()){
                    readUids.add(uid);
                }
                if(DeleteStatus.DELETED_REQUESTED == xms.getDeleteStatus()){
                    deletedUids.add(uid);
                }
            }
        }
        if(!readUids.isEmpty()){
            changes.add(new FlagChange(folder,readUids,Flag.Seen));
        }
        if(!deletedUids.isEmpty()){
            changes.add(new FlagChange(folder,deletedUids,Flag.Deleted));
        }
        return changes;
    }

    @Override
    public void finalizeLocalEvents(MessageType messageType, String messageId, boolean seenEvent, boolean deleteEvent) {
        
        if(deleteEvent){
            mXmsLog.deleteMessage(messageId);
        }
        else if(seenEvent){
            mXmsLog.updateReadStatusdWithBaseId(messageId, ReadStatus.READ);
        }            
    }
}
