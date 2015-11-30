
package com.gsma.rcs.cms.event;

import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.Telephony.TextBasedSmsColumns;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.imap.message.ImapMmsMessage;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObjectFactory;
import com.gsma.rcs.service.broadcaster.IXmsMessageEventBroadcaster;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage.ReasonCode;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class XmsEventListener implements INativeXmsEventListener, IRcsXmsEventListener, IRemoteEventHandler {

    private static final Logger sLogger = Logger.getLogger(XmsEventListener.class.getSimpleName());
    private final Context mContext;
    private final XmsLog mXmsLog;
    private final ImapLog mImapLog;
    private final RcsSettings mSettings;

    private final List<IXmsMessageEventBroadcaster> mXmsMessageEventBroadcaster = new ArrayList<>();

    /**
     * Default constructor
     * @param imapLog
     * @param xmsLog
     */
    public XmsEventListener(Context context, ImapLog imapLog, XmsLog xmsLog, RcsSettings settings) {
        mContext = context;
        mXmsLog = xmsLog;
        mImapLog = imapLog;
        mSettings = settings;
    }

    public void registerBroadcaster(IXmsMessageEventBroadcaster broadcaster) {
        synchronized (mXmsMessageEventBroadcaster) {
            mXmsMessageEventBroadcaster.add(broadcaster);
        }
    }

    public void unregisterBroadcaster(IXmsMessageEventBroadcaster broadcaster) {
        synchronized (mXmsMessageEventBroadcaster) {
            mXmsMessageEventBroadcaster.remove(broadcaster);
        }
    }

    /***********************************************************************/
    /******************         Native SMS Events         ******************/
    /***********************************************************************/

    @Override
    public void onIncomingSms(SmsDataObject message) {
        if(sLogger.isActivated()){
            sLogger.debug("onIncomingSms " + message.toString());
        }

        mXmsLog.addSms(message);
        mImapLog.addMessage(new MessageData(
                CmsUtils.contactToCmsFolder(mSettings, message.getContact()),
                MessageData.ReadStatus.UNREAD,
                MessageData.DeleteStatus.NOT_DELETED,
                mSettings.getCmsPushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.SMS,
                message.getMessageId()
        ));

        synchronized (mXmsMessageEventBroadcaster){
            for (IXmsMessageEventBroadcaster listener : mXmsMessageEventBroadcaster) {
                listener.broadcastNewMessage(message.getMimeType(), message.getMessageId());
            }
        }
    }

    @Override
    public void onOutgoingSms(SmsDataObject message) {
        if(sLogger.isActivated()){
            sLogger.debug("onOutgoingSms " + message.toString());
        }
        mXmsLog.addSms(message);
        mImapLog.addMessage(new MessageData(
                CmsUtils.contactToCmsFolder(mSettings, message.getContact()),
                MessageData.ReadStatus.READ,
                MessageData.DeleteStatus.NOT_DELETED,
                mSettings.getCmsPushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.SMS,
                message.getMessageId()
        ));

        synchronized (mXmsMessageEventBroadcaster){
            for (IXmsMessageEventBroadcaster listener : mXmsMessageEventBroadcaster) {
                listener.broadcastNewMessage(message.getMimeType(), message.getMessageId());
            }
        }
    }

    @Override
    public void onDeleteNativeSms(long nativeProviderId) {

        Cursor cursor = null;
        try{
            cursor = mXmsLog.getXmsMessage(nativeProviderId, MimeType.TEXT_MESSAGE);
            if(!cursor.moveToNext()){
                return;
            }
            String contact = cursor.getString(cursor.getColumnIndex(XmsMessageLog.CONTACT));
            String messageId = cursor.getString(cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID));

            mXmsLog.deleteXmsMessage(messageId);
            mImapLog.updateDeleteStatus(MessageType.SMS, messageId, MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);

            synchronized (mXmsMessageEventBroadcaster){
                for (IXmsMessageEventBroadcaster listener : mXmsMessageEventBroadcaster) {
                    listener.broadcastMessageStateChanged(ContactUtil.createContactIdFromTrustedData(contact), MimeType.TEXT_MESSAGE, messageId, State.DELETING, ReasonCode.UNSPECIFIED);
                }
            }
        }
        finally{
            CursorUtil.close(cursor);
        }
    }

    /***********************************************************************/
    /******************         Native MMS Events         ******************/
    /***********************************************************************/
    @Override
    public void onIncomingMms(MmsDataObject message) {
        if(sLogger.isActivated()){
            sLogger.debug("onIncomingMms " + message.toString());
        }
        try { //TODO FGI : handle exceptions
            mXmsLog.addMms(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
        mImapLog.addMessage(new MessageData(
                CmsUtils.contactToCmsFolder(mSettings, message.getContact()),
                ReadStatus.UNREAD,
                MessageData.DeleteStatus.NOT_DELETED,
                mSettings.getCmsPushMms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.MMS,
                message.getMessageId()
        ));

        synchronized (mXmsMessageEventBroadcaster){
            for (IXmsMessageEventBroadcaster listener : mXmsMessageEventBroadcaster) {
                listener.broadcastNewMessage(message.getMimeType(), message.getMessageId());
            }
        }
    }

    @Override
    public void onOutgoingMms(MmsDataObject message) {
        if(sLogger.isActivated()){
            sLogger.debug("onOutgoingMms " + message.toString());
        }
        try { //TODO FGI : handle exceptions
            mXmsLog.addMms(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
        mImapLog.addMessage(new MessageData(
                CmsUtils.contactToCmsFolder(mSettings, message.getContact()),
                ReadStatus.READ,
                MessageData.DeleteStatus.NOT_DELETED,
                mSettings.getCmsPushMms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.MMS,
                message.getMessageId()
        ));

        synchronized (mXmsMessageEventBroadcaster){
            for (IXmsMessageEventBroadcaster listener : mXmsMessageEventBroadcaster) {
                listener.broadcastNewMessage(message.getMimeType(), message.getMessageId());
            }
        }
    }

    @Override
    public void onDeleteNativeMms(String mmsId) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteNativeMms " + mmsId);
        }
        Cursor cursor = null;
        try{
            cursor = mXmsLog.getMmsMessage(mmsId);
            if(!cursor.moveToNext()){
                return;
            }
            String contact = cursor.getString(cursor.getColumnIndex(XmsMessageLog.CONTACT));
            String messageId = cursor.getString(cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID));
            mXmsLog.deleteXmsMessage(messageId);
            mImapLog.updateDeleteStatus(MessageType.MMS, messageId, MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);
            synchronized (mXmsMessageEventBroadcaster){
                for (IXmsMessageEventBroadcaster listener : mXmsMessageEventBroadcaster) {
                    listener.broadcastMessageStateChanged(ContactUtil.createContactIdFromTrustedData(contact), MimeType.MULTIMEDIA_MESSAGE, messageId, State.DELETING, ReasonCode.UNSPECIFIED);
                }
            }
        }
        finally{
            CursorUtil.close(cursor);
        }
    }

    @Override
    public void onMessageStateChanged(Long nativeProviderId, String mimeType, int type, int status) {
        if(sLogger.isActivated()){
            sLogger.debug(new StringBuilder("onMessageStateChanged:")
                    .append(nativeProviderId).append(",")
                    .append(mimeType).append(",")
                    .append(type).append(",")
                    .append(status).toString());
        }

        State state = getState(type, status);
        if(state == null){
            return;
        }
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getXmsMessage(nativeProviderId, mimeType);
            if(!cursor.moveToNext()){
                return;
            }
            String messageId = cursor.getString(cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID));
            String contact =  cursor.getString(cursor.getColumnIndex(XmsMessageLog.CONTACT));
            synchronized (mXmsMessageEventBroadcaster){
                for (IXmsMessageEventBroadcaster listener : mXmsMessageEventBroadcaster) {
                    Set<String> messageIds = new HashSet<>();
                    messageIds.add(messageId);
                    listener.broadcastMessageStateChanged(ContactUtil.createContactIdFromTrustedData(contact), mimeType, messageId, state, ReasonCode.UNSPECIFIED);
                }
            }
        }
        finally{
            CursorUtil.close(cursor);
        }
    }

    private State getState(int type, int status){
        if(type == TextBasedSmsColumns.MESSAGE_TYPE_FAILED){
            return State.FAILED;
        }

        if(type == TextBasedSmsColumns.MESSAGE_TYPE_SENT){
            if(status == TextBasedSmsColumns.STATUS_NONE ||
                    status == TextBasedSmsColumns.STATUS_PENDING){
                return State.SENT;
            }
            else if (status == TextBasedSmsColumns.STATUS_COMPLETE){
                return State.DELIVERED;
            }
        }
        return null;
    }

    @Override
    public void onReadNativeConversation(long nativeThreadId) {
        if(sLogger.isActivated()){
            sLogger.debug("onReadNativeConversation " + nativeThreadId );
        }
        Cursor cursor = null;
        try{
            cursor = mXmsLog.getUnreadXmsMessages(nativeThreadId);
            int messageIdIdx = cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID);
            int mimeTypeIdx = cursor.getColumnIndex(XmsMessageLog.MIME_TYPE);
            int contactIdIx = cursor.getColumnIndex(XmsMessageLog.CONTACT);
            while(cursor.moveToNext()){
                String contact = cursor.getString(contactIdIx);
                String messageId = cursor.getString(messageIdIdx);
                String mimeType = cursor.getString(mimeTypeIdx);
                MessageType messageType = MessageType.SMS;
                if(MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)){
                    messageType = MessageType.MMS;
                }
                mImapLog.updateReadStatus(messageType, messageId, MessageData.ReadStatus.READ_REPORT_REQUESTED);
                mXmsLog.markMessageAsRead(messageId);
                synchronized (mXmsMessageEventBroadcaster){
                    for (IXmsMessageEventBroadcaster listener : mXmsMessageEventBroadcaster) {
                        Set<String> messageIds = new HashSet<>();
                        messageIds.add(messageId);
                        listener.broadcastMessageStateChanged(ContactUtil.createContactIdFromTrustedData(contact), mimeType, messageId, State.DISPLAYED, ReasonCode.UNSPECIFIED);
                    }
                }
            }
        }
        finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public void onDeleteNativeConversation(long nativeThreadId) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteNativeConversation " + nativeThreadId);
        }
        Cursor cursor = null;
        try{
            cursor = mXmsLog.getXmsMessages(nativeThreadId);
            int messageIdIdx = cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID);
            int mimeTypeIdx = cursor.getColumnIndex(XmsMessageLog.MIME_TYPE);
            int contactIdIx = cursor.getColumnIndex(XmsMessageLog.CONTACT);
            while(cursor.moveToNext()){
                String contact = cursor.getString(contactIdIx);
                String messageId = cursor.getString(messageIdIdx);
                String mimeType = cursor.getString(mimeTypeIdx);
                MessageType messageType = MessageType.SMS;
                if(MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)){
                    messageType = MessageType.MMS;
                }
                mImapLog.updateDeleteStatus(messageType, messageId, MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);
                mXmsLog.deleteXmsMessage(messageId);
                synchronized (mXmsMessageEventBroadcaster){
                    for (IXmsMessageEventBroadcaster listener : mXmsMessageEventBroadcaster) {
                        Set<String> messageIds = new HashSet<>();
                        messageIds.add(messageId);
                        listener.broadcastMessageStateChanged(ContactUtil.createContactIdFromTrustedData(contact), mimeType, messageId, State.DELETING, ReasonCode.UNSPECIFIED);
                    }
                }
            }
        }
        finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public void onReadRcsMessage(String messageId) {
        if(sLogger.isActivated()){
            sLogger.debug("onReadRcsMessage " + messageId );
        }
        MessageType messageType = MessageType.SMS;
        String mimeType = mXmsLog.getMimeType(messageId);
        if(MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)){
            messageType = MessageType.MMS;
        }
        mImapLog.updateReadStatus(messageType, messageId, MessageData.ReadStatus.READ_REPORT_REQUESTED);
        mXmsLog.markMessageAsRead(messageId);
    }

    @Override
    public void onReadRcsConversation(ContactId contactId) {
        if(sLogger.isActivated()){
            sLogger.debug("onReadRcsConversation " + contactId.toString() );
        }
        Cursor cursor = null;
        try{
            cursor = mXmsLog.getXmsMessages(contactId);
            int messageIdIdx = cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID);
            int mimeTypeIdx = cursor.getColumnIndex(XmsMessageLog.MIME_TYPE);
            while(cursor.moveToNext()){
                String messageId = cursor.getString(messageIdIdx);
                String mimeType = cursor.getString(mimeTypeIdx);
                MessageType messageType = MessageType.SMS;
                if(MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)){
                    messageType = MessageType.MMS;
                }
                mImapLog.updateReadStatus(messageType, messageId, ReadStatus.READ_REPORT_REQUESTED);
            }
        }
        finally {
            CursorUtil.close(cursor);
        }
        mXmsLog.markConversationAsRead(contactId);
    }

    @Override
    public void onDeleteRcsMessage(String messageId) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteRcsMessage " + messageId );
        }
        String mimeType = mXmsLog.getMimeType(messageId);
        MessageType messageType = MessageType.SMS;
        if(MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)){
            messageType = MessageType.MMS;
        }
        mImapLog.updateDeleteStatus(messageType, messageId, MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);
        mXmsLog.deleteXmsMessage(messageId);
    }

    @Override
    public void onDeleteRcsConversation(ContactId contactId) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteRcsConversation " + contactId.toString() );
        }
        Cursor cursor = null;
        try{
            cursor = mXmsLog.getXmsMessages(contactId);
            int messageIdIdx = cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID);
            int mimeTypeIdx = cursor.getColumnIndex(XmsMessageLog.MIME_TYPE);
            while(cursor.moveToNext()){
                String messageId = cursor.getString(messageIdIdx);
                String mimeType = cursor.getString(mimeTypeIdx);
                MessageType messageType = MessageType.SMS;
                if(MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)){
                    messageType = MessageType.MMS;
                }
                mImapLog.updateDeleteStatus(messageType, messageId, MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);
            }
        }
        finally {
            CursorUtil.close(cursor);
        }
        mXmsLog.deleteXmsMessages(contactId);
    }

    @Override
    public void onDeleteAll() {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteAll");
        }
        mXmsLog.deleteAllEntries();
        mImapLog.updateDeleteStatus(MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);
    }

    @Override
    public void onRemoteReadEvent(MessageType messageType, String messageId) {
        if(sLogger.isActivated()){
            sLogger.debug("onRemoteReadEvent");
        }
        mXmsLog.markMessageAsRead(messageId);
        //TODO update message status for API

    }

    @Override
    public void onRemoteDeleteEvent(MessageType messageType, String messageId) {
        if(sLogger.isActivated()){
            sLogger.debug("onRemoteDeleteEvent");
        }
        mXmsLog.deleteXmsMessage(messageId);
    }

    @Override
    public String onRemoteNewMessage(MessageType messageType, IImapMessage message) {
        if(sLogger.isActivated()){
            sLogger.debug("onRemoteNewMessage");
        }

        if(message.isDeleted()){ // no need to add a deleted message in xms content provider
            return IdGenerator.generateMessageID();
        }

        if(MessageType.SMS == messageType){
            SmsDataObject smsDataObject = XmsDataObjectFactory.createSmsDataObject((ImapSmsMessage) message);
            mXmsLog.addSms(smsDataObject);
            return smsDataObject.getMessageId();
        }
        else if(MessageType.MMS == messageType){
            MmsDataObject mmsDataObject = XmsDataObjectFactory.createMmsDataObject(mContext, (ImapMmsMessage) message);
            try { //TODO FGI handle exceptions
                mXmsLog.addMms(mmsDataObject);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
            return mmsDataObject.getMessageId();
        }
        return null;
    }

    @Override
    public String getMessageId(MessageType messageType, IImapMessage message) {

        // check if an entry already exist in imapData provider
        MessageData messageData = mImapLog.getMessage(message.getFolder(), message.getUid());
        if(messageData!=null){
            return messageData.getMessageId();
        }

        if(MessageType.SMS == messageType){
            // get messages from provider with contact, direction and correlator fields
            // messages are sorted by Date DESC (more recent first).
            SmsDataObject smsData = XmsDataObjectFactory.createSmsDataObject((ImapSmsMessage)message);
            List<String> ids =  mXmsLog.getMessageIds(smsData.getContact().toString(), smsData.getDirection(), smsData.getCorrelator());

            // take the first message which s not synchronized with CMS server (have no uid)
            for(String id : ids){
                Integer uid = mImapLog.getUidForXmsMessage(id);
                if(uid==null || uid==0){
                    return id;
                }
            }
            return null;
        }
        else if(MessageType.MMS == messageType){
            String mmsId = ((ImapMmsMessage)message).getRawMessage().getBody().getHeader(Constants.HEADER_MESSAGE_ID);
            return getMessageId(mmsId);
        }
        return null;
    }

    private String getMessageId(String mmsId){
        Cursor cursor = null;
        try{
            cursor = mXmsLog.getMmsMessage(mmsId);
            if(!cursor.moveToNext()){
                return null;
            }
            return cursor.getString(cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID));
        }
        finally{
            CursorUtil.close(cursor);
        }
    }
}
