
package com.gsma.rcs.cms.observer;

import com.gsma.rcs.cms.event.INativeSmsEventListener;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.PushStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.Telephony.TextBasedSmsColumns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmsObserver implements INativeSmsEventListener {

    private static final Logger sLogger = Logger.getLogger(SmsObserver.class.getSimpleName());

    private final List<INativeSmsEventListener> sSmsEventListeners = new ArrayList<INativeSmsEventListener>();
    // Uri used when:
    // - an outgoing sms is sent and delivered
    // - an message or conversation is deleted
    private static String sSmsUri = "content://sms/";
    private static String sSmsRaw = "content://sms/raw";
    private static String sSmsConversations = "content://sms/conversations";
    private static String sSmsInbox = "content://sms/inbox";
    // Uri used when:
    // a message or group of message is marked as read
    private static final String sMmsSmsUri = "content://mms-sms/";

    private Context mContext;
    private ContentResolver mContentResolver;
    private XmsContentObserver mXmsContentObserver;

    private static SmsObserver sInstance;

    private class XmsContentObserver extends ContentObserver {

        private final String[] PROJECTION_SMS = new String[] {
                BaseColumns._ID, TextBasedSmsColumns.ADDRESS, TextBasedSmsColumns.DATE,
                TextBasedSmsColumns.DATE_SENT, TextBasedSmsColumns.PROTOCOL,
                TextBasedSmsColumns.STATUS, TextBasedSmsColumns.TYPE, TextBasedSmsColumns.BODY
        };
        private final String[] PROJECTION_CONTACT = new String[] {"DISTINCT ".concat(TextBasedSmsColumns.ADDRESS)};
        private final String[] PROJECTION_UNREAD_SMS = new String[] {
                BaseColumns._ID, TextBasedSmsColumns.ADDRESS
        };

        private final String WHERE_CONTACT_NOT_NULL = new StringBuilder(TextBasedSmsColumns.ADDRESS).append(" is not null").toString();
        private final String WHERE_UNREAD = new StringBuilder(TextBasedSmsColumns.READ).append("=0").append(" AND ").append(WHERE_CONTACT_NOT_NULL).toString();
        
        private Map<String,Set<Long>> mUnreadMessagesId = new HashMap<String,Set<Long>>();
        private Set<String> mConversations;

        public XmsContentObserver(Handler handler) {
            super(handler);
            mConversations = getConversations();
            mUnreadMessagesId = getUnreadMessages();
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            sLogger.info("onChange method : " + uri);
            synchronized (this) {

                if (uri.toString().equals(sMmsSmsUri)) { // handle read notification and delete
                                                         // conversation
                    checkDeletedConversations();
                    checkReadMessages();                                            
                    return;
                }

                if (!uri.toString().equals(sSmsRaw) &&
                        !uri.toString().equals(sSmsInbox) &&
                        !uri.toString().startsWith((sSmsConversations)) &&
                        uri.toString().startsWith(sSmsUri)) { // handle
                                                                                             // incoming/outgoing
                                                                                             // message
                                                                                             // and
                                                                                             // delete
                                                                                             // event
                    Cursor cursor = null;
                    try {
                        cursor = mContentResolver.query(uri, PROJECTION_SMS, null, null, null);
                        CursorUtil.assertCursorIsNotNull(cursor, uri);
                        if (!cursor.moveToNext()) {
                            // Delete event
                            onDeleteNativeSms(Long.parseLong(uri.getLastPathSegment()));
                        } else {
                            Long _id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                            String address = cursor
                                    .getString(cursor.getColumnIndex(TextBasedSmsColumns.ADDRESS));
                            if(address==null){
                                return;
                            }
                            PhoneNumber phoneNumber = ContactUtil
                                    .getValidPhoneNumberFromAndroid(address);
                            if (phoneNumber != null) {
                                address = ContactUtil.createContactIdFromValidatedData(phoneNumber)
                                        .toString();
                            } else {
                                address = address.replaceAll(" ", "");
                            }
                            long date = cursor
                                    .getLong(cursor.getColumnIndex(TextBasedSmsColumns.DATE));
                            long date_sent = cursor
                                    .getLong(cursor.getColumnIndex(TextBasedSmsColumns.DATE_SENT));
                            String protocol = cursor
                                    .getString(cursor.getColumnIndex(TextBasedSmsColumns.PROTOCOL));
                            int status = cursor
                                    .getInt(cursor.getColumnIndex(TextBasedSmsColumns.STATUS));
                            int type = cursor
                                    .getInt(cursor.getColumnIndex(TextBasedSmsColumns.TYPE));
                            String body = cursor
                                    .getString(cursor.getColumnIndex(TextBasedSmsColumns.BODY));

                            if (protocol != null) { // incoming message
                                onIncomingSms(new SmsData(_id, address, body, date,
                                        Direction.INCOMING, ReadStatus.UNREAD));
                            } else if (protocol == null
                                    && type == TextBasedSmsColumns.MESSAGE_TYPE_SENT) // outgoing
                                                                                      // message
                            {
                                if (status == TextBasedSmsColumns.STATUS_NONE || // No delivery
                                                                                 // report
                                                                                 // asked by user
                                status == TextBasedSmsColumns.STATUS_PENDING) { // delivery
                                                                                // report asked
                                                                                // by user
                                    onOutgoingSms(new SmsData(_id, address, body, date,
                                            Direction.OUTGOING, ReadStatus.READ_REQUESTED));
                                } else if (status == TextBasedSmsColumns.STATUS_COMPLETE) { // delivery
                                                                                            // report
                                                                                            // receipt
                                    onDeliverNativeSms(_id, date_sent);
                                }
                            }

                        }
                        return;
                    } finally {
                        CursorUtil.close(cursor);
                    }
                }

            }
        }

        private void checkReadMessages() {
            Map<String,Set<Long>> unreadMessages = getUnreadMessages();  
            
            Set<Long> currentIds = new HashSet<Long>();
            for (Set<Long> ids : unreadMessages.values()){
                currentIds.addAll(ids);
            }
            
            Set<Long> previousIds = new HashSet<Long>();
            for (Set<Long> ids : mUnreadMessagesId.values()){
                previousIds.addAll(ids);
            }
            
            previousIds.removeAll(currentIds);
            for(Long id : previousIds){
                onReadNativeSms(id);                
            }
            
            mUnreadMessagesId = unreadMessages;            
        }
        
        private void checkDeletedConversations(){
            Set<String> currentConversations = getConversations();                        
            mConversations.removeAll(currentConversations);
            for(String conversation : mConversations){
                onDeleteNativeConversation(conversation);
                mUnreadMessagesId.remove(conversation);
            }                        
            mConversations = currentConversations;            
        }
        
        private Map<String,Set<Long>> getUnreadMessages(){
            Map<String,Set<Long>> unreadMessages = new HashMap<String,Set<Long>>(); 
            Uri uri = Uri.parse(sSmsUri);
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(uri, PROJECTION_UNREAD_SMS, WHERE_UNREAD, null,
                        null);
                CursorUtil.assertCursorIsNotNull(cursor, uri);
                int idIdx = cursor.getColumnIndex(BaseColumns._ID);
                int addressIdx = cursor.getColumnIndex(TextBasedSmsColumns.ADDRESS);
                while (cursor.moveToNext()) {
                    String contact  = cursor.getString(addressIdx);
                    Long id = cursor.getLong(idIdx);
                    Set<Long> ids = unreadMessages.get(contact);
                    if(ids==null){
                        ids = new HashSet<Long>();
                        unreadMessages.put(contact,ids);
                    }
                    ids.add(id);
                }
                return unreadMessages;
            } finally {
                CursorUtil.close(cursor);
            }
        }
        
        private Set<String> getConversations(){
            Set<String> conversations = new HashSet<String>();
            Cursor cursor = null;
            try{
                Uri uri = Uri.parse(sSmsUri);
                cursor= mContentResolver.query(uri, PROJECTION_CONTACT, WHERE_CONTACT_NOT_NULL, null, null);
                CursorUtil.assertCursorIsNotNull(cursor, uri);
                int idx  = cursor.getColumnIndex(TextBasedSmsColumns.ADDRESS);
                while(cursor.moveToNext()) {
                    conversations.add(cursor.getString(idx));
                }
                return conversations;
            }
            finally{
                CursorUtil.close(cursor);
            }
        }

    }

    /**
     * Returns the status report value found in the "pdu" extra of the intent
     * 
     * @param intent to be analyzed
     * @return The value of the status report. 0 if the message has been delivered to the end user,
     *         others values defined TS 23.040 or -1 if no "pdu" extra in the intent or the pdu is
     *         not a status report message.
     */
    private int getStatusReportFromIntent(Intent intent) {
        Object message = intent.getExtras().get("pdu");
        if (message == null) {
            return -1;
        }
        byte[] pdu;
        pdu = (byte[]) message;
        android.telephony.SmsMessage msg;
        msg = android.telephony.SmsMessage.createFromPdu(pdu);
        if (msg.isStatusReportMessage()) {
            return msg.getStatus();
        } else {
            return -1;
        }
    }

    private SmsObserver(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    public static SmsObserver createInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (SmsObserver.class) {
            if (sInstance == null) {
                sInstance = new SmsObserver(context);
            }
        }
        return sInstance;
    }

    /**
     * 
     */
    public void start() {
        // register content observer
        mXmsContentObserver = new XmsContentObserver(new Handler());
        mContentResolver.registerContentObserver(Uri.parse(sSmsUri), true, mXmsContentObserver);
        mContentResolver.registerContentObserver(Uri.parse(sMmsSmsUri), true, mXmsContentObserver);
    }

    /**
     * 
     */
    public void stop() {
        // unregister content observer
        sSmsEventListeners.clear();
        mContentResolver.unregisterContentObserver(mXmsContentObserver);
        mXmsContentObserver = null;
    }

    public void registerListener(INativeSmsEventListener listener) {
        synchronized (sSmsEventListeners) {
            sSmsEventListeners.add(listener);
        }
    }

    public void unregisterListener(INativeSmsEventListener listener) {
        synchronized (sSmsEventListeners) {
            sSmsEventListeners.remove(listener);
        }
    }

    @Override
    public void onIncomingSms(SmsData message) {
        if(sLogger.isActivated()){
            sLogger.debug("onIncomingSms : ".concat(String.valueOf(message.getNativeProviderId())));
            sLogger.debug("listeners size : ".concat(String.valueOf(sSmsEventListeners.size())));
        }
        message.setPushStatus(PushStatus.PUSH_REQUESTED);
        synchronized (sSmsEventListeners) {
            for(INativeSmsEventListener listener : sSmsEventListeners){
                listener.onIncomingSms(message);
            }
        }
    }

    @Override
    public void onOutgoingSms(SmsData message) {
        if(sLogger.isActivated()){
            sLogger.debug("onOutgoingSms : ".concat(String.valueOf(message.getNativeProviderId())));
        }
        message.setPushStatus(PushStatus.PUSH_REQUESTED);
        synchronized (sSmsEventListeners) {
            for(INativeSmsEventListener listener : sSmsEventListeners){
                listener.onOutgoingSms(message);
            }
        }
    }

    @Override
    public void onDeliverNativeSms(long nativeProviderId, long sentDate) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeliverNativeSms : ".concat(String.valueOf(nativeProviderId)));
        }
        synchronized (sSmsEventListeners) {
            for(INativeSmsEventListener listener : sSmsEventListeners){
                listener.onDeliverNativeSms(nativeProviderId, sentDate);
            }
        }
    }

    @Override
    public void onReadNativeSms(long nativeProviderId) {
        if(sLogger.isActivated()){
            sLogger.debug("onReadNativeSms : ".concat(String.valueOf(nativeProviderId)));
        }
        synchronized (sSmsEventListeners) {
            for (INativeSmsEventListener listener : sSmsEventListeners) {
                listener.onReadNativeSms(nativeProviderId);
            }
        }
    }

    @Override
    public void onDeleteNativeSms(long nativeProviderId) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteNativeSms : ".concat(String.valueOf(nativeProviderId)));
        }
        synchronized (sSmsEventListeners) {
            for (INativeSmsEventListener listener : sSmsEventListeners) {
                listener.onDeleteNativeSms(nativeProviderId);
            }
        }
    }

    @Override
    public void onDeleteNativeConversation(String contact) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteNativeConversation : ".concat(contact));
        }
        synchronized (sSmsEventListeners) {
            for (INativeSmsEventListener listener : sSmsEventListeners) {
                listener.onDeleteNativeConversation(contact);
            }
        }
    }
}
