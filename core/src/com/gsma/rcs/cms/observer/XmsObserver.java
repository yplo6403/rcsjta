
package com.gsma.rcs.cms.observer;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.event.INativeXmsEventListener;
import com.gsma.rcs.cms.observer.XmsObserverUtils.Sms;
import com.gsma.rcs.cms.observer.XmsObserverUtils.Mms;
import com.gsma.rcs.cms.observer.XmsObserverUtils.Conversation;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.model.MmsPart;
import com.gsma.rcs.cms.provider.xms.model.XmsData.PushStatus;
import com.gsma.rcs.cms.provider.xms.model.XmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.MmsData;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.BaseMmsColumns;
import android.provider.Telephony.TextBasedSmsColumns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class XmsObserver implements INativeXmsEventListener {

    private static final Logger sLogger = Logger.getLogger(XmsObserver.class.getSimpleName());

    private final List<INativeXmsEventListener> sXmsEventListeners = new ArrayList<INativeXmsEventListener>();

    private static final Uri MMS_SMS_URI = Uri.parse("content://mms-sms/");

    private Context mContext;
    private ContentResolver mContentResolver;
    private XmsContentObserver mXmsContentObserver;
    private CmsSettings mCmsSettings;

    private static XmsObserver sInstance;

    private class XmsContentObserver extends ContentObserver {

        private Set<String> mMmsIds;
        private Set<Long> mSmsIds;
        /// key : threadId, value:read
        private Map<Long,Boolean> mConversations;

        public XmsContentObserver(Handler handler) {
            super(handler);
            mConversations = getConversations();
            mMmsIds = getMmsIds();
            mSmsIds = getSmsIds();
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if(sLogger.isActivated()){
                sLogger.info("onChange method : " + uri);
            }

            if(uri==null){
                return;
            }

            synchronized (this) {

                if (ignoreUri(uri)) { // not interested by this uri
                    return;
                }

                if (uri.toString().startsWith(Sms.URI.toString())) {
                    checkSmsEvents(uri);
                    return;
                }

                if (uri.toString().equals(MMS_SMS_URI.toString())) { // handle read notification and delete
                    if (checkMmsEvents()) {
                        return;
                    }
                    Map<Long,Boolean> currentConversations = getConversations();
                    if(!checkDeleteConversationEvent(currentConversations)){
                        checkReadConversationEvent(currentConversations);
                    }
                    mConversations = currentConversations;
                }
            }
        }


        private boolean ignoreUri(Uri uri){

            String uriStr = uri.toString();
            for(Uri smsUri : Sms.FILTERED_URIS){
                if(uriStr.startsWith(smsUri.toString())){
                    return true;
                }
            }
            return false;
        }

        /**
         * This method checks SMS events :
         * - incoming SMS
         * - outgoing SMS
         * - delete SMS
         */
        private void checkSmsEvents(Uri uri) {
            Cursor cursor = null;
            try {
                Set<Long> currentSmsIds = getSmsIds();
                int diff  = currentSmsIds.size() - mSmsIds.size();
                mSmsIds = currentSmsIds;
                if(diff==0){
                    return;
                }
                if(diff==-1){
                    onDeleteNativeSms(Long.parseLong(uri.getLastPathSegment()));
                    return;
                }

                cursor = mContentResolver.query(uri, Sms.PROJECTION, Sms.WHERE_INBOX_OR_SENT, null, null);
                if(!cursor.moveToNext()){
                    return;
                }
                CursorUtil.assertCursorIsNotNull(cursor, uri);
                Long _id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                Long threadId = cursor.getLong(cursor.getColumnIndex(TextBasedSmsColumns.THREAD_ID));
                String address = cursor
                        .getString(cursor.getColumnIndex(TextBasedSmsColumns.ADDRESS));
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
                if ("0".equals(protocol)) { // incoming message
                    onIncomingSms(new SmsData(_id, threadId, address, body, date,
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
                        onOutgoingSms(new SmsData(_id, threadId, address, body, date,
                                Direction.OUTGOING, ReadStatus.READ_REQUESTED));
                    } else if (status == TextBasedSmsColumns.STATUS_COMPLETE) { // delivery
                        // report
                        // receipt
                        onDeliverNativeSms(_id, date_sent);
                    }
                }

                return;
            } finally {
                CursorUtil.close(cursor);
            }
        }

    /**
     * This method checks MMS events:
     * - incoming MMS
     *
     * - outgoing MMS
     * - delete MMS
     */
    private boolean checkMmsEvents() {

        Set<String> mmsIds = getMmsIds();
        int diff = mmsIds.size() - mMmsIds.size();
        if (diff==0) {
            return false;
        }

        Cursor cursor = null;
        if(diff<0) { // Delete MMS event
            boolean eventChecked = false;
            if(diff==-1){ // one MMS deleted
                mMmsIds.removeAll(mmsIds);
                for(String id : mMmsIds){
                    onDeleteNativeMms(id);
                    eventChecked = true;
                }
            }
            mMmsIds = mmsIds;
            return eventChecked;
        }

        //check if we have all infos in database about mms message
        Long id,threadId, date;
        id = date = -1l;
        String subject, messageId, from;
        subject = messageId =from = null;
        Direction direction = Direction.INCOMING;
        TreeSet<String> contacts = new TreeSet<>();
        try{
            cursor = mContentResolver.query(Mms.URI, Mms.PROJECTION, Mms.WHERE, null, BaseMmsColumns._ID);
            CursorUtil.assertCursorIsNotNull(cursor, Mms.URI);
            if (!cursor.moveToLast()) {
                return false;
            }
            id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
            threadId = cursor.getLong(cursor.getColumnIndex(BaseMmsColumns.THREAD_ID));
            subject =  cursor.getString(cursor.getColumnIndex(BaseMmsColumns.SUBJECT));
            messageId = cursor.getString(cursor.getColumnIndex(BaseMmsColumns.MESSAGE_ID));
            int messageType = cursor.getInt(cursor.getColumnIndex(BaseMmsColumns.MESSAGE_TYPE));
            if(128 == messageType){
                direction = Direction.OUTGOING;
            }
            date = cursor.getLong(cursor.getColumnIndex(BaseMmsColumns.DATE));
        }
        finally{
            CursorUtil.close(cursor);
        }

        // Get recipients
        try {
            int type = Mms.Addr.FROM;
            if(direction == Direction.OUTGOING){
                type = Mms.Addr.TO;
            }
            cursor = mContentResolver.query(Uri.parse(String.format(Mms.Addr.URI,id)), Mms.Addr.PROJECTION, Mms.Addr.WHERE, new String[]{String.valueOf(type)}, null);
            CursorUtil.assertCursorIsNotNull(cursor, Mms.Addr.URI);
            int adressIdx = cursor.getColumnIndex(Telephony.Mms.Addr.ADDRESS);
            while(cursor.moveToNext()){
                contacts.add(cursor.getString(adressIdx));
            }
        } finally {
            CursorUtil.close(cursor);
        }

        if(contacts.isEmpty()){
            return false;
        }

        mMmsIds = mmsIds;
        // Get part
        List<MmsPart> parts = new ArrayList<>();
        String textContent = null;
        try {
            cursor = mContentResolver.query(Uri.parse(Mms.Part.URI), Mms.Part.PROJECTION, Mms.Part.WHERE, new String[]{String.valueOf(id)}, null);
            CursorUtil.assertCursorIsNotNull(cursor, Mms.Part.URI);
            int _idIdx = cursor.getColumnIndexOrThrow(BaseMmsColumns._ID);
            int contentTypeIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_TYPE);
            int contentIdIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_ID);
            int textIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.TEXT);
            int dataIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part._DATA);
            while(cursor.moveToNext()){
                String contentType = cursor.getString(contentTypeIdx);
                String text = cursor.getString(textIdx);
                if(Constants.CONTENT_TYPE_TEXT.equals(contentType)){
                    textContent = text;
                }
                parts.add(new MmsPart(
                        null,
                        cursor.getString(_idIdx),
                        contentType,
                        cursor.getString(contentIdIdx),
                        cursor.getString(dataIdx),
                        text));

            }
        }
        finally {
            CursorUtil.close(cursor);
        }

        MmsData mmsData = new MmsData(id, threadId, messageId, contacts, subject, textContent, date*1000, direction);
        mmsData.setParts(parts);
        if(Direction.INCOMING == direction){
            mmsData.setReadStatus(ReadStatus.UNREAD);
            onIncomingMms(mmsData);
        }
        else{
            mmsData.setReadStatus(ReadStatus.READ_REQUESTED);
            onOutgoingMms(mmsData);
        }
        return true;
    }

        private boolean checkReadConversationEvent(Map<Long,Boolean> currentConversations) {

            boolean eventChecked = false;
            Set<Long> unreadConversations = new HashSet<>();
            Iterator<Entry<Long, Boolean>> iter = mConversations.entrySet().iterator();
            while(iter.hasNext()){
                Entry<Long,Boolean> entry = iter.next();
                if(!entry.getValue()){
                    unreadConversations.add(entry.getKey());
                }
            }

            Set<Long> currentUnreadConversations = new HashSet<>();
            iter = currentConversations.entrySet().iterator();
            while(iter.hasNext()){
                Entry<Long,Boolean> entry = iter.next();
                if(!entry.getValue()){
                    currentUnreadConversations.add(entry.getKey());
                }
            }

            // make delta between unread to get read conversation
            unreadConversations.removeAll(currentUnreadConversations);
            for(Long threadId : unreadConversations){
                onReadNativeConversation(threadId);
                eventChecked = true;
            }
            return eventChecked;
        }

        private boolean checkDeleteConversationEvent(Map<Long,Boolean> currentConversations) {

            boolean eventChecked = false;
            Set<Long> deletedConversations = new HashSet(mConversations.keySet());
            deletedConversations.removeAll(currentConversations.keySet());
            for (Long conversation : deletedConversations) {
                onDeleteNativeConversation(conversation);
                eventChecked = true;
            }
            return eventChecked;
        }

    private Map<Long,Boolean> getConversations() {
        Map<Long,Boolean> conversations = new HashMap<>();
        Cursor cursor = null;

        try {
            cursor = mContentResolver.query(Conversation.URI, Conversation.PROJECTION,null, null, BaseColumns._ID);
            CursorUtil.assertCursorIsNotNull(cursor, Conversation.URI);
            int _idIdx = cursor.getColumnIndex(BaseColumns._ID);
            int readIdx = cursor.getColumnIndex(Telephony.Mms.READ);
            while (cursor.moveToNext()) {
                conversations.put(cursor.getLong(_idIdx), cursor.getInt(readIdx)==1);
            }
            return conversations;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Set<Long> getSmsIds() {
        Cursor cursor = null;
        Set<Long> ids = new HashSet();
        try {
            cursor = mContentResolver.query(Sms.URI, Sms.PROJECTION_ID, Sms.WHERE_INBOX_OR_SENT, null, BaseMmsColumns._ID);
            CursorUtil.assertCursorIsNotNull(cursor, Sms.URI);
            int idx= cursor.getColumnIndex(BaseColumns._ID);
            while(cursor.moveToNext()){
                ids.add(cursor.getLong(idx));
            }
            return ids;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Set<String> getMmsIds() {
            Cursor cursor = null;
            Set<String> ids = new HashSet();
            try {
                cursor = mContentResolver.query(Mms.URI, Mms.PROJECTION_MMS_ID, Mms.WHERE, null, BaseMmsColumns._ID);
                CursorUtil.assertCursorIsNotNull(cursor, Mms.URI);
                int idx= cursor.getColumnIndex(BaseMmsColumns.MESSAGE_ID);
                while(cursor.moveToNext()){
                    ids.add(cursor.getString(idx));
                }
                return ids;
            } finally {
                CursorUtil.close(cursor);
            }
        }
}
//    /**
//     * Returns the status report value found in the "pdu" extra of the intent
//     *
//     * @param intent to be analyzed
//     * @return The value of the status report. 0 if the message has been delivered to the end user,
//     * others values defined TS 23.040 or -1 if no "pdu" extra in the intent or the pdu is
//     * not a status report message.
//     */
//    private int getStatusReportFromIntent(Intent intent) {
//        Object message = intent.getExtras().get("pdu");
//        if (message == null) {
//            return -1;
//        }
//        byte[] pdu;
//        pdu = (byte[]) message;
//        android.telephony.SmsMessage msg;
//        msg = android.telephony.SmsMessage.createFromPdu(pdu);
//        if (msg.isStatusReportMessage()) {
//            return msg.getStatus();
//        } else {
//            return -1;
//        }
//    }

    private XmsObserver(Context context, CmsSettings cmsSettings) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mCmsSettings = cmsSettings;
    }

    public static XmsObserver createInstance(Context context, CmsSettings cmsSettings) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (XmsObserver.class) {
            if (sInstance == null) {
                sInstance = new XmsObserver(context, cmsSettings);
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
        mContentResolver.registerContentObserver(Sms.URI, true, mXmsContentObserver);
        mContentResolver.registerContentObserver(Mms.URI, true, mXmsContentObserver);
        mContentResolver.registerContentObserver(MMS_SMS_URI, true, mXmsContentObserver);
    }

    /**
     *
     */
    public void stop() {
        // unregister content observer
        sXmsEventListeners.clear();
        mContentResolver.unregisterContentObserver(mXmsContentObserver);
        mXmsContentObserver = null;
    }

    public void registerListener(INativeXmsEventListener listener) {
        synchronized (sXmsEventListeners) {
            sXmsEventListeners.add(listener);
        }
    }

    public void unregisterListener(INativeXmsEventListener listener) {
        synchronized (sXmsEventListeners) {
            sXmsEventListeners.remove(listener);
        }
    }

    @Override
    public void onIncomingSms(SmsData message) {
        if (sLogger.isActivated()) {
            sLogger.info("onIncomingSms : ".concat(String.valueOf(message.getNativeProviderId())));
            sLogger.info("listeners size : ".concat(String.valueOf(sXmsEventListeners.size())));
        }
        PushStatus pushStatus = mCmsSettings.getPushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED;
        message.setPushStatus(pushStatus);
        synchronized (sXmsEventListeners) {
            for (INativeXmsEventListener listener : sXmsEventListeners) {
                listener.onIncomingSms(message);
            }
        }
    }

    @Override
    public void onOutgoingSms(SmsData message) {
        if (sLogger.isActivated()) {
            sLogger.info("onOutgoingSms : ".concat(String.valueOf(message.getNativeProviderId())));
        }
        PushStatus pushStatus = mCmsSettings.getPushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED;
        message.setPushStatus(pushStatus);
        synchronized (sXmsEventListeners) {
            for (INativeXmsEventListener listener : sXmsEventListeners) {
                listener.onOutgoingSms(message);
            }
        }
    }

    @Override
    public void onDeliverNativeSms(long nativeProviderId, long sentDate) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeliverNativeSms : ".concat(String.valueOf(nativeProviderId)));
        }
        synchronized (sXmsEventListeners) {
            for (INativeXmsEventListener listener : sXmsEventListeners) {
                listener.onDeliverNativeSms(nativeProviderId, sentDate);
            }
        }
    }

    @Override
    public void onDeleteNativeSms(long nativeProviderId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteNativeSms : ".concat(String.valueOf(nativeProviderId)));
        }
        synchronized (sXmsEventListeners) {
            for (INativeXmsEventListener listener : sXmsEventListeners) {
                listener.onDeleteNativeSms(nativeProviderId);
            }
        }
    }

    @Override
    public void onIncomingMms(MmsData message) {
        if (sLogger.isActivated()) {
            sLogger.info("onIncomingMms : ".concat(message.toString()));
        }
        PushStatus pushStatus = mCmsSettings.getPushMms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED;
        message.setPushStatus(pushStatus);
        synchronized (sXmsEventListeners) {
            for (INativeXmsEventListener listener : sXmsEventListeners) {
                listener.onIncomingMms(message);
            }
        }
    }

    @Override
    public void onOutgoingMms(MmsData message) {
        if (sLogger.isActivated()) {
            sLogger.info("onOutgoingMms : ".concat(message.toString()));
        }
        PushStatus pushStatus = mCmsSettings.getPushMms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED;
        message.setPushStatus(pushStatus);
        synchronized (sXmsEventListeners) {
            for (INativeXmsEventListener listener : sXmsEventListeners) {
                listener.onOutgoingMms(message);
            }
        }
    }

    @Override
    public void onDeleteNativeMms(String mmsId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteNativeMms : ".concat(mmsId));
        }
        synchronized (sXmsEventListeners) {
            for (INativeXmsEventListener listener : sXmsEventListeners) {
                listener.onDeleteNativeMms(mmsId);
            }
        }
    }

    @Override
    public void onDeleteNativeConversation(long nativeThreadId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteNativeConversation : " + nativeThreadId);
        }
        synchronized (sXmsEventListeners) {
            for (INativeXmsEventListener listener : sXmsEventListeners) {
                listener.onDeleteNativeConversation(nativeThreadId);
            }
        }
    }

    @Override
    public void onReadNativeConversation(long nativeThreadId) {
        if (sLogger.isActivated()) {
            sLogger.info("onReadNativeConversation : " + nativeThreadId);
        }
        synchronized (sXmsEventListeners) {
            for (INativeXmsEventListener listener : sXmsEventListeners) {
                listener.onReadNativeConversation(nativeThreadId);
            }
        }
    }
}
