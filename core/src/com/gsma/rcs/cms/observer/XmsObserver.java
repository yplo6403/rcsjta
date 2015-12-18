// TODO  FG add copyrights

package com.gsma.rcs.cms.observer;

import static com.gsma.rcs.provider.CursorUtil.assertCursorIsNotNull;
import static com.gsma.rcs.provider.CursorUtil.close;

import com.gsma.rcs.cms.event.INativeXmsEventListener;
import com.gsma.rcs.cms.observer.XmsObserverUtils.Conversation;
import com.gsma.rcs.cms.observer.XmsObserverUtils.Mms;
import com.gsma.rcs.cms.observer.XmsObserverUtils.Mms.Part;
import com.gsma.rcs.cms.observer.XmsObserverUtils.Sms;
import com.gsma.rcs.cms.utils.MmsUtils;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.ImageUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
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

public class XmsObserver implements INativeXmsEventListener {

    private static final Logger sLogger = Logger.getLogger(XmsObserver.class.getSimpleName());
    private static final String sThreadName = "XmsObserver";
    private final List<INativeXmsEventListener> mXmsEventListeners = new ArrayList<>();

    private static final Uri MMS_SMS_URI = Uri.parse("content://mms-sms/");

    private final ContentResolver mContentResolver;
    private XmsContentObserver mXmsContentObserver;
    private Handler mXmsObserverHandler;

    private class XmsContentObserver extends ContentObserver {

        private Set<String> mMmsIds;
        private Set<Long> mSmsIds;
        // / key : threadId, value:read
        private Map<Long, Boolean> mConversations;

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
            if (sLogger.isActivated()) {
                sLogger.info("onChange method : " + uri);
            }
            if (uri == null) {
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
                if (uri.toString().equals(MMS_SMS_URI.toString())) { // handle read notification and
                                                                     // delete
                    if (checkMmsEvents()) {
                        return;
                    }
                    Map<Long, Boolean> currentConversations = getConversations();
                    if (!checkDeleteConversationEvent(currentConversations)) {
                        checkReadConversationEvent(currentConversations);
                    }
                    mConversations = currentConversations;
                }
            }
        }

        private boolean ignoreUri(Uri uri) {
            String uriStr = uri.toString();
            for (Uri smsUri : Sms.FILTERED_URIS) {
                if (uriStr.startsWith(smsUri.toString())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * This method checks SMS events : - incoming SMS - outgoing SMS - delete SMS
         */
        private void checkSmsEvents(Uri uri) {
            Set<Long> currentSmsIds = getSmsIds();
            int diff = currentSmsIds.size() - mSmsIds.size();
            mSmsIds = currentSmsIds;
            if (diff == -1) {
                onDeleteNativeSms(Long.parseLong(uri.getLastPathSegment()));
            } else if (diff == 1) {
                handleNewSms(uri);
            } else { // sms update status
                handleSmsUpdateStatus(uri);
            }
        }

        private void handleNewSms(Uri uri) {
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(uri, Sms.PROJECTION, Sms.WHERE_CONTACT_NOT_NULL,
                        null, null);
                assertCursorIsNotNull(cursor, uri);
                if (!cursor.moveToNext()) {
                    return;
                }
                Long _id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                Long threadId = cursor
                        .getLong(cursor.getColumnIndex(TextBasedSmsColumns.THREAD_ID));
                String address = cursor.getString(cursor
                        .getColumnIndex(TextBasedSmsColumns.ADDRESS));
                PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromAndroid(address);
                if (phoneNumber == null) {
                    return;
                }
                ContactId contactId = ContactUtil.createContactIdFromValidatedData(phoneNumber);
                String body = cursor.getString(cursor.getColumnIndex(TextBasedSmsColumns.BODY));
                long date = cursor.getLong(cursor.getColumnIndex(TextBasedSmsColumns.DATE));
                String protocol = cursor.getString(cursor
                        .getColumnIndex(TextBasedSmsColumns.PROTOCOL));
                Direction direction = Direction.INCOMING;
                if (protocol == null) {
                    direction = Direction.OUTGOING;
                }
                SmsDataObject smsDataObject = new SmsDataObject(IdGenerator.generateMessageID(),
                        contactId, body, direction, date, _id, threadId);
                if (Direction.INCOMING == direction) {
                    smsDataObject.setState(State.RECEIVED);
                    onIncomingSms(smsDataObject);
                } else {
                    smsDataObject.setReadStatus(ReadStatus.READ);
                    onOutgoingSms(smsDataObject);
                }
            } finally {
                close(cursor);
            }
        }

        private void handleSmsUpdateStatus(Uri uri) {
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(uri, Sms.PROJECTION, Sms.WHERE_CONTACT_NOT_NULL,
                        null, null);
                assertCursorIsNotNull(cursor, uri);
                if (!cursor.moveToNext()) {
                    return;
                }
                Long _id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                int status = cursor.getInt(cursor.getColumnIndex(TextBasedSmsColumns.STATUS));
                int type = cursor.getInt(cursor.getColumnIndex(TextBasedSmsColumns.TYPE));
                onMessageStateChanged(_id, MimeType.TEXT_MESSAGE,
                        XmsObserverUtils.getSmsState(type, status));
            } finally {
                close(cursor);
            }
        }

        /**
         * This method checks MMS events: - incoming MMS - outgoing MMS - delete MMS
         */
        private boolean checkMmsEvents() {
            Set<String> mmsIds = getMmsIds();
            int diff = mmsIds.size() - mMmsIds.size();
            if (diff == 0) {
                return false;
            }

            Cursor cursor = null;
            if (diff < 0) { // Delete MMS event
                boolean eventChecked = false;
                if (diff == -1) { // one MMS deleted
                    mMmsIds.removeAll(mmsIds);
                    for (String id : mMmsIds) {
                        onDeleteNativeMms(id);
                        eventChecked = true;
                    }
                }
                mMmsIds = mmsIds;
                return eventChecked;
            }

            // check if we have all infos in database about mms message
            Long id, threadId, date;
            id = date = -1L;
            String mmsId, subject, transactionId;
            Direction direction = Direction.INCOMING;
            Set<ContactId> contacts = new HashSet<>();
            try {
                cursor = mContentResolver.query(Mms.URI, Mms.PROJECTION, Mms.WHERE_INBOX_OR_SENT,
                        null, BaseMmsColumns._ID);
                assertCursorIsNotNull(cursor, Mms.URI);
                if (!cursor.moveToLast()) {
                    return false;
                }
                id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                threadId = cursor.getLong(cursor.getColumnIndex(BaseMmsColumns.THREAD_ID));
                mmsId = cursor.getString(cursor.getColumnIndex(BaseMmsColumns.MESSAGE_ID));
                subject = cursor.getString(cursor.getColumnIndex(BaseMmsColumns.SUBJECT));
                transactionId = cursor.getString(cursor
                        .getColumnIndex(BaseMmsColumns.TRANSACTION_ID));
                int messageType = cursor.getInt(cursor.getColumnIndex(BaseMmsColumns.MESSAGE_TYPE));
                if (128 == messageType) {
                    direction = Direction.OUTGOING;
                }
                date = cursor.getLong(cursor.getColumnIndex(BaseMmsColumns.DATE));
            } finally {
                close(cursor);
            }
            // Get recipients
            Map<ContactId, String> messageIds = new HashMap<>();
            try {
                int type = Mms.Addr.FROM;
                if (Direction.OUTGOING == direction) {
                    type = Mms.Addr.TO;
                }
                cursor = mContentResolver.query(Uri.parse(String.format(Mms.Addr.URI, id)),
                        Mms.Addr.PROJECTION, Mms.Addr.WHERE, new String[] {
                            String.valueOf(type)
                        }, null);
                assertCursorIsNotNull(cursor, Mms.Addr.URI);
                int adressIdx = cursor.getColumnIndex(Telephony.Mms.Addr.ADDRESS);
                while (cursor.moveToNext()) {
                    String address = cursor.getString(adressIdx);
                    PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromAndroid(address);
                    if (phoneNumber == null) {
                        if (sLogger.isActivated()) {
                            sLogger.info("Bad format for contact : " + address);
                        }
                        continue;
                    }
                    ContactId contact = ContactUtil.createContactIdFromValidatedData(phoneNumber);
                    messageIds.put(contact, IdGenerator.generateMessageID());
                    contacts.add(contact);
                }
            } finally {
                close(cursor);
            }

            if (contacts.isEmpty()) {
                return true;
            }

            mMmsIds = mmsIds;
            // Get part
            Map<ContactId, List<MmsPart>> mmsParts = new HashMap<>();
            try {
                cursor = mContentResolver.query(Uri.parse(Mms.Part.URI), Mms.Part.PROJECTION,
                        Mms.Part.WHERE, new String[] {
                            String.valueOf(id)
                        }, null);
                assertCursorIsNotNull(cursor, Mms.Part.URI);
                int _idIdx = cursor.getColumnIndexOrThrow(BaseMmsColumns._ID);
                int filenameIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_LOCATION);
                int contentTypeIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_TYPE);
                int textIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.TEXT);
                int dataIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part._DATA);

                while (cursor.moveToNext()) {
                    String contentType = cursor.getString(contentTypeIdx);
                    String text = cursor.getString(textIdx);
                    String filename = cursor.getString(filenameIdx);
                    String data = cursor.getString(dataIdx);
                    if (contentType == null) { // skip MMS message with null content type
                        return true;
                    }
                    if (data != null) {
                        Uri file = Uri.parse(Part.URI.concat(cursor.getString(_idIdx)));
                        byte[] bytes = MmsUtils.getContent(mContentResolver, file);
                        Long fileSize = (long) bytes.length;
                        byte[] fileIcon = null;
                        if (MimeManager.isImageType(contentType)) {
                            fileIcon = ImageUtils.tryGetThumbnail(mContentResolver, file);
                        }
                        for (ContactId contact : contacts) {
                            List<MmsPart> mmsPart = mmsParts.get(contact);
                            if (mmsPart == null) {
                                mmsPart = new ArrayList<>();
                                mmsParts.put(contact, mmsPart);
                            }
                            mmsPart.add(new MmsPart(messageIds.get(contact), contact, filename,
                                    fileSize, contentType, file, fileIcon));
                        }
                    } else {
                        for (ContactId contact : contacts) {
                            List<MmsPart> mmsPart = mmsParts.get(contact);
                            if (mmsPart == null) {
                                mmsPart = new ArrayList<>();
                                mmsParts.put(contact, mmsPart);
                            }
                            mmsPart.add(new MmsPart(messageIds.get(contact), contact, contentType,
                                    text));
                        }
                    }
                }
            } finally {
                close(cursor);
            }

            ReadStatus readStatus = (Direction.INCOMING == direction ? ReadStatus.UNREAD
                    : ReadStatus.READ);
            for (Entry<ContactId, List<MmsPart>> entry : mmsParts.entrySet()) {
                ContactId contact = entry.getKey();
                MmsDataObject mmsDataObject = new MmsDataObject(mmsId, transactionId,
                        messageIds.get(contact), contact, subject, direction, readStatus,
                        date * 1000, id, threadId, entry.getValue());
                if (Direction.INCOMING == direction) {
                    mmsDataObject.setState(State.RECEIVED);
                    onIncomingMms(mmsDataObject);
                } else {
                    mmsDataObject.setState(State.DISPLAYED);
                    onOutgoingMms(mmsDataObject);
                }
            }
            return true;
        }

        private boolean checkReadConversationEvent(Map<Long, Boolean> currentConversations) {

            boolean eventChecked = false;
            Set<Long> unreadConversations = new HashSet<>();
            Iterator<Entry<Long, Boolean>> iter = mConversations.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<Long, Boolean> entry = iter.next();
                if (!entry.getValue()) {
                    unreadConversations.add(entry.getKey());
                }
            }

            Set<Long> currentUnreadConversations = new HashSet<>();
            iter = currentConversations.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<Long, Boolean> entry = iter.next();
                if (!entry.getValue()) {
                    currentUnreadConversations.add(entry.getKey());
                }
            }

            // make delta between unread to get read conversation
            unreadConversations.removeAll(currentUnreadConversations);
            for (Long threadId : unreadConversations) {
                onReadNativeConversation(threadId);
                eventChecked = true;
            }
            return eventChecked;
        }

        private boolean checkDeleteConversationEvent(Map<Long, Boolean> currentConversations) {

            boolean eventChecked = false;
            Set<Long> deletedConversations = new HashSet<>(mConversations.keySet());
            deletedConversations.removeAll(currentConversations.keySet());
            for (Long conversation : deletedConversations) {
                onDeleteNativeConversation(conversation);
                eventChecked = true;
            }
            if (eventChecked) {
                mSmsIds = getSmsIds();
                mMmsIds = getMmsIds();
            }
            return eventChecked;
        }

        private Map<Long, Boolean> getConversations() {
            Map<Long, Boolean> conversations = new HashMap<>();
            Cursor cursor = null;

            try {
                cursor = mContentResolver.query(Conversation.URI, Conversation.PROJECTION, null,
                        null, BaseColumns._ID);
                assertCursorIsNotNull(cursor, Conversation.URI);
                int _idIdx = cursor.getColumnIndex(BaseColumns._ID);
                int readIdx = cursor.getColumnIndex(Telephony.Mms.READ);
                while (cursor.moveToNext()) {
                    conversations.put(cursor.getLong(_idIdx), cursor.getInt(readIdx) == 1);
                }
                return conversations;
            } finally {
                close(cursor);
            }
        }

        private Set<Long> getSmsIds() {
            Cursor cursor = null;
            Set<Long> ids = new HashSet<>();
            try {
                cursor = mContentResolver.query(Sms.URI, Sms.PROJECTION_ID,
                        Sms.WHERE_CONTACT_NOT_NULL, null, BaseMmsColumns._ID);
                assertCursorIsNotNull(cursor, Sms.URI);
                int idx = cursor.getColumnIndex(BaseColumns._ID);
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(idx));
                }
                return ids;
            } finally {
                close(cursor);
            }
        }

        private Set<String> getMmsIds() {
            Cursor cursor = null;
            Set<String> ids = new HashSet<>();
            try {
                cursor = mContentResolver.query(Mms.URI, Mms.PROJECTION_MMS_ID,
                        Mms.WHERE_INBOX_OR_SENT, null, BaseMmsColumns._ID);
                assertCursorIsNotNull(cursor, Mms.URI);
                int idx = cursor.getColumnIndex(BaseMmsColumns.MESSAGE_ID);
                while (cursor.moveToNext()) {
                    ids.add(cursor.getString(idx));
                }
                return ids;
            } finally {
                close(cursor);
            }
        }
    }

    public XmsObserver(Context context) {
        mContentResolver = context.getContentResolver();
        mXmsObserverHandler = allocateBgHandler(sThreadName);
    }

    private Handler allocateBgHandler(String threadName) {
        HandlerThread thread = new HandlerThread(threadName);
        thread.start();
        return new Handler(thread.getLooper());
    }

    /**
     *
     */
    public void start() {
        // register content observer
        mXmsContentObserver = new XmsContentObserver(mXmsObserverHandler);
        mContentResolver.registerContentObserver(Sms.URI, true, mXmsContentObserver);
        mContentResolver.registerContentObserver(Mms.URI, true, mXmsContentObserver);
        mContentResolver.registerContentObserver(MMS_SMS_URI, true, mXmsContentObserver);
    }

    /**
     *
     */
    public void stop() {
        // unregister content observer
        mContentResolver.unregisterContentObserver(mXmsContentObserver);
        mXmsObserverHandler.getLooper().quit();
        mXmsObserverHandler.getLooper().getThread().interrupt();
        mXmsContentObserver = null;
        mXmsEventListeners.clear();
    }

    public void registerListener(INativeXmsEventListener listener) {
        synchronized (mXmsEventListeners) {
            mXmsEventListeners.add(listener);
        }
    }

    public void unregisterListener(INativeXmsEventListener listener) {
        synchronized (mXmsEventListeners) {
            mXmsEventListeners.remove(listener);
        }
    }

    /********************** XMS Events **********************/

    @Override
    public void onDeleteNativeSms(long nativeProviderId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteNativeSms : ".concat(String.valueOf(nativeProviderId)));
        }
        synchronized (mXmsEventListeners) {
            for (INativeXmsEventListener listener : mXmsEventListeners) {
                listener.onDeleteNativeSms(nativeProviderId);
            }
        }
    }

    @Override
    public void onDeleteNativeMms(String mmsId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteNativeMms : ".concat(mmsId));
        }
        synchronized (mXmsEventListeners) {
            for (INativeXmsEventListener listener : mXmsEventListeners) {
                listener.onDeleteNativeMms(mmsId);
            }
        }
    }

    @Override
    public void onDeleteNativeConversation(long nativeThreadId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteNativeConversation : " + nativeThreadId);
        }
        synchronized (mXmsEventListeners) {
            for (INativeXmsEventListener listener : mXmsEventListeners) {
                listener.onDeleteNativeConversation(nativeThreadId);
            }
        }
    }

    @Override
    public void onReadNativeConversation(long nativeThreadId) {
        if (sLogger.isActivated()) {
            sLogger.info("onReadNativeConversation : " + nativeThreadId);
        }
        synchronized (mXmsEventListeners) {
            for (INativeXmsEventListener listener : mXmsEventListeners) {
                listener.onReadNativeConversation(nativeThreadId);
            }
        }
    }

    @Override
    public void onIncomingSms(SmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.info("onIncomingSms : ".concat(String.valueOf(message.getNativeProviderId())));
            sLogger.info("listeners size : ".concat(String.valueOf(mXmsEventListeners.size())));
        }
        synchronized (mXmsEventListeners) {
            for (INativeXmsEventListener listener : mXmsEventListeners) {
                listener.onIncomingSms(message);
            }
        }
    }

    @Override
    public void onOutgoingSms(SmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.info("onOutgoingSms : ".concat(String.valueOf(message.getNativeProviderId())));
            sLogger.info("listeners size : ".concat(String.valueOf(mXmsEventListeners.size())));
        }
        synchronized (mXmsEventListeners) {
            for (INativeXmsEventListener listener : mXmsEventListeners) {
                listener.onOutgoingSms(message);
            }
        }
    }

    @Override
    public void onIncomingMms(MmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.info("onIncomingMms : ".concat(String.valueOf(message.getNativeProviderId())));
            sLogger.info("listeners size : ".concat(String.valueOf(mXmsEventListeners.size())));
        }
        synchronized (mXmsEventListeners) {
            for (INativeXmsEventListener listener : mXmsEventListeners) {
                listener.onIncomingMms(message);
            }
        }
    }

    @Override
    public void onOutgoingMms(MmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.info("onOutgoingMms : ".concat(String.valueOf(message.getNativeProviderId())));
            sLogger.info("listeners size : ".concat(String.valueOf(mXmsEventListeners.size())));
        }
        synchronized (mXmsEventListeners) {
            for (INativeXmsEventListener listener : mXmsEventListeners) {
                listener.onOutgoingMms(message);
            }
        }
    }

    @Override
    public void onMessageStateChanged(Long nativeProviderId, String mimeType, State state) {
        if (state == null) {
            return;
        }
        synchronized (mXmsEventListeners) {
            for (INativeXmsEventListener listener : mXmsEventListeners) {
                listener.onMessageStateChanged(nativeProviderId, mimeType, state);
            }
        }
    }

}
