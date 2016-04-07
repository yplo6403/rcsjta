/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.cms.xms.observer;

import static com.gsma.rcs.provider.CursorUtil.assertCursorIsNotNull;
import static com.gsma.rcs.provider.CursorUtil.close;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.utils.MmsUtils;
import com.gsma.rcs.core.cms.xms.observer.XmsObserverUtils.Conversation;
import com.gsma.rcs.core.cms.xms.observer.XmsObserverUtils.Mms;
import com.gsma.rcs.core.cms.xms.observer.XmsObserverUtils.Mms.Part;
import com.gsma.rcs.core.cms.xms.observer.XmsObserverUtils.Sms;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class in charge of detecting changes on XMS messages in the native content provider.<br>
 * The followings events are detected by this observer:
 * <ul>
 * <li>incoming & outgoing SMS</li>
 * <li>read of a SMS</li>
 * <li>deletion of a SMS</li>
 * <li>incoming & outgoing MMS</li>
 * <li>read of a MMS</li>
 * <li>read of a conversation</li>
 * <li>deletion of a conversation</li>
 * </ul>
 */
public class XmsObserver implements XmsObserverListener {

    private static final Logger sLogger = Logger.getLogger(XmsObserver.class.getSimpleName());
    private static final String sThreadName = "XmsObserver";
    private final List<XmsObserverListener> mXmsObserverListeners = new ArrayList<>();

    private static final Uri MMS_SMS_URI = Uri.parse("content://mms-sms/");

    private final ContentResolver mContentResolver;
    private XmsContentObserver mXmsContentObserver;
    private Handler mXmsObserverHandler;

    /**
     * Content observer in charge of generating events from changes in native XMS content provider
     */
    private class XmsContentObserver extends ContentObserver {

        private Set<String> mMmsIds;
        private Set<Long> mSmsIds;
        // / key : threadId, value:read
        private Map<Long, Boolean> mConversations;

        /**
         * Constructor
         *
         * @param handler background handler
         */
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
                sLogger.info("onChange: " + uri);
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
                if (MMS_SMS_URI.equals(uri)) { // handle read notification and delete
                    try {
                        if (checkMmsEvents()) {
                            return;
                        }
                    } catch (FileAccessException e) {
                        sLogger.error("Check MMS events failure", e);
                    }
                    Map<Long, Boolean> currentConversations = getConversations();
                    if (!checkDeleteConversationEvent(currentConversations.keySet())) {
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
         * Check SMS events from content provider
         *
         * @param uri the URI of the SMS
         */
        private void checkSmsEvents(Uri uri) {
            Set<Long> currentSmsIds = getSmsIds();
            int diff = currentSmsIds.size() - mSmsIds.size();
            mSmsIds = currentSmsIds;
            if (diff == -1) {
                onDeleteSmsFromNativeApp(Long.parseLong(uri.getLastPathSegment()));
            } else if (diff == 1) {
                handleNewSms(uri);
            } else { // sms update status
                handleSmsUpdateStatus(uri);
            }
        }

        /**
         * Notify listeners of a new SMS in the native content provider
         *
         * @param uri the URI of the SMS
         */
        private void handleNewSms(Uri uri) {
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(uri, Sms.PROJECTION, Sms.WHERE_CONTACT_NOT_NULL,
                        null, null);
                assertCursorIsNotNull(cursor, uri);
                if (!cursor.moveToNext()) {
                    return;
                }
                Long _id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
                Long threadId = cursor.getLong(cursor
                        .getColumnIndexOrThrow(TextBasedSmsColumns.THREAD_ID));
                String address = cursor.getString(cursor
                        .getColumnIndexOrThrow(TextBasedSmsColumns.ADDRESS));
                PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromAndroid(address);
                if (phoneNumber == null) {
                    return;
                }
                ContactId contactId = ContactUtil.createContactIdFromValidatedData(phoneNumber);
                String body = cursor.getString(cursor
                        .getColumnIndexOrThrow(TextBasedSmsColumns.BODY));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(TextBasedSmsColumns.DATE));
                String protocol = cursor.getString(cursor
                        .getColumnIndexOrThrow(TextBasedSmsColumns.PROTOCOL));
                Direction direction = Direction.INCOMING;
                if (protocol == null) {
                    direction = Direction.OUTGOING;
                }
                SmsDataObject smsDataObject = new SmsDataObject(IdGenerator.generateMessageID(),
                        contactId, body, direction, NtpTrustedTime.currentTimeMillis(), _id, threadId);
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

        /**
         * Notify listeners that the SMS status has changed
         *
         * @param uri the URI of the SMS
         */
        private void handleSmsUpdateStatus(Uri uri) {
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(uri, Sms.PROJECTION, Sms.WHERE_CONTACT_NOT_NULL,
                        null, null);
                assertCursorIsNotNull(cursor, uri);
                if (!cursor.moveToNext()) {
                    return;
                }
                Long _id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
                int status = cursor
                        .getInt(cursor.getColumnIndexOrThrow(TextBasedSmsColumns.STATUS));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(TextBasedSmsColumns.TYPE));
                onXmsMessageStateChanged(_id, MimeType.TEXT_MESSAGE,
                        XmsObserverUtils.getSmsState(type, status));
            } finally {
                close(cursor);
            }
        }

        /**
         * Check MMS events from content provider
         */
        private boolean checkMmsEvents() throws FileAccessException {
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
                        onDeleteMmsFromNativeApp(id);
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
                id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
                threadId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseMmsColumns.THREAD_ID));
                mmsId = cursor.getString(cursor.getColumnIndexOrThrow(BaseMmsColumns.MESSAGE_ID));
                subject = cursor.getString(cursor.getColumnIndexOrThrow(BaseMmsColumns.SUBJECT));
                transactionId = cursor.getString(cursor
                        .getColumnIndexOrThrow(BaseMmsColumns.TRANSACTION_ID));
                int messageType = cursor.getInt(cursor
                        .getColumnIndexOrThrow(BaseMmsColumns.MESSAGE_TYPE));
                if (128 == messageType) {
                    direction = Direction.OUTGOING;
                }
                date = cursor.getLong(cursor.getColumnIndexOrThrow(BaseMmsColumns.DATE));
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
                        Mms.Addr.PROJECTION, Mms.Addr.WHERE, new String[]{
                                String.valueOf(type)
                        }, null);
                assertCursorIsNotNull(cursor, Mms.Addr.URI);
                int adressIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS);
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
                        Mms.Part.WHERE, new String[]{
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
                        byte[] bytes;
                        try {
                            bytes = MmsUtils.getContent(mContentResolver, file);

                        } catch (FileAccessException e) {
                            if (sLogger.isActivated()) {
                                sLogger.warn("Failed to read MMS part " + file, e);
                            }
                            /* Skip invalid record */
                            continue;
                        }
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
                        NtpTrustedTime.currentTimeMillis(), id, threadId, entry.getValue());
                if (Direction.INCOMING == direction) {
                    mmsDataObject.setState(State.RECEIVED);
                    onIncomingMms(mmsDataObject);
                } else {
                    mmsDataObject.setState(State.SENT);
                    onOutgoingMms(mmsDataObject);
                }
            }
            return true;
        }

        /**
         * Check if a conversation has been read
         *
         * @param currentConversations the current conversations
         * @return ?? //TODO FG what does it return
         */
        private boolean checkReadConversationEvent(Map<Long, Boolean> currentConversations) {
            boolean eventChecked = false;
            Set<Long> unreadConversations = new HashSet<>();
            for (Entry<Long, Boolean> entry : mConversations.entrySet()) {
                if (!entry.getValue()) {
                    unreadConversations.add(entry.getKey());
                }
            }
            Set<Long> currentUnreadConversations = new HashSet<>();
            for (Entry<Long, Boolean> entry : currentConversations.entrySet()) {
                if (!entry.getValue()) {
                    currentUnreadConversations.add(entry.getKey());
                }
            }
            // make delta between unread to get read conversation
            unreadConversations.removeAll(currentUnreadConversations);
            for (Long threadId : unreadConversations) {
                onReadXmsConversationFromNativeApp(threadId);
                eventChecked = true;
            }
            return eventChecked;
        }

        /**
         * Check if a conversation has been deleted
         *
         * @param currentConversations the conversations
         * @return true if some conversations were deleted
         */
        private boolean checkDeleteConversationEvent(Set<Long> currentConversations) {
            boolean eventChecked = false;
            Set<Long> deletedConversations = new HashSet<>(mConversations.keySet());
            deletedConversations.removeAll(currentConversations);
            for (Long conversation : deletedConversations) {
                onDeleteXmsConversationFromNativeApp(conversation);
                eventChecked = true;
            }
            if (eventChecked) {
                mSmsIds = getSmsIds();
                mMmsIds = getMmsIds();
            }
            return eventChecked;
        }

        /**
         * Get conversations
         *
         * @return map of conversation ID with their read status
         */
        private Map<Long, Boolean> getConversations() {
            Map<Long, Boolean> conversations = new HashMap<>();
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(Conversation.URI, Conversation.PROJECTION, null,
                        null, BaseColumns._ID);
                assertCursorIsNotNull(cursor, Conversation.URI);
                int _idIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
                int readIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.READ);
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
                int idx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
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
                int idx = cursor.getColumnIndexOrThrow(BaseMmsColumns.MESSAGE_ID);
                while (cursor.moveToNext()) {
                    ids.add(cursor.getString(idx));
                }
                return ids;
            } finally {
                close(cursor);
            }
        }
    }

    /**
     * Constructor
     *
     * @param context the context
     */
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
     * Start the content observer
     */
    public void start() {
        // register content observer
        mXmsContentObserver = new XmsContentObserver(mXmsObserverHandler);
        mContentResolver.registerContentObserver(Sms.URI, true, mXmsContentObserver);
        mContentResolver.registerContentObserver(Mms.URI, true, mXmsContentObserver);
        mContentResolver.registerContentObserver(MMS_SMS_URI, true, mXmsContentObserver);
    }

    /**
     * Stop the content observer
     */
    public void stop() {
        // unregister content observer
        mContentResolver.unregisterContentObserver(mXmsContentObserver);
        mXmsObserverHandler.getLooper().quit();
        mXmsObserverHandler.getLooper().getThread().interrupt();
        mXmsContentObserver = null;
        mXmsObserverListeners.clear();
    }

    /**
     * Register a listener which want to be notified of XMS events
     *
     * @param listener the listener
     */
    public void registerListener(XmsObserverListener listener) {
        synchronized (mXmsObserverListeners) {
            mXmsObserverListeners.add(listener);
        }
    }

    /**
     * Unregister listener
     *
     * @param listener the listener
     */

    public void unregisterListener(XmsObserverListener listener) {
        synchronized (mXmsObserverListeners) {
            mXmsObserverListeners.remove(listener);
        }
    }

    /**********************
     * XMS Events
     **********************/

    @Override
    public void onDeleteSmsFromNativeApp(long nativeProviderId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteNativeSms : ".concat(String.valueOf(nativeProviderId)));
        }
        synchronized (mXmsObserverListeners) {
            for (XmsObserverListener listener : mXmsObserverListeners) {
                listener.onDeleteSmsFromNativeApp(nativeProviderId);
            }
        }
    }

    @Override
    public void onDeleteMmsFromNativeApp(String mmsId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteNativeMms : ".concat(mmsId));
        }
        synchronized (mXmsObserverListeners) {
            for (XmsObserverListener listener : mXmsObserverListeners) {
                listener.onDeleteMmsFromNativeApp(mmsId);
            }
        }
    }

    @Override
    public void onDeleteXmsConversationFromNativeApp(long nativeThreadId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteNativeConversation : " + nativeThreadId);
        }
        synchronized (mXmsObserverListeners) {
            for (XmsObserverListener listener : mXmsObserverListeners) {
                listener.onDeleteXmsConversationFromNativeApp(nativeThreadId);
            }
        }
    }

    @Override
    public void onReadXmsConversationFromNativeApp(long nativeThreadId) {
        if (sLogger.isActivated()) {
            sLogger.info("onReadNativeConversation : " + nativeThreadId);
        }
        synchronized (mXmsObserverListeners) {
            for (XmsObserverListener listener : mXmsObserverListeners) {
                listener.onReadXmsConversationFromNativeApp(nativeThreadId);
            }
        }
    }

    @Override
    public void onIncomingSms(SmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.info("onIncomingSms : ".concat(String.valueOf(message.getNativeProviderId())));
            sLogger.info("listeners size : ".concat(String.valueOf(mXmsObserverListeners.size())));
        }
        synchronized (mXmsObserverListeners) {
            for (XmsObserverListener listener : mXmsObserverListeners) {
                listener.onIncomingSms(message);
            }
        }
    }

    @Override
    public void onOutgoingSms(SmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.info("onOutgoingSms : ".concat(String.valueOf(message.getNativeProviderId())));
            sLogger.info("listeners size : ".concat(String.valueOf(mXmsObserverListeners.size())));
        }
        synchronized (mXmsObserverListeners) {
            for (XmsObserverListener listener : mXmsObserverListeners) {
                listener.onOutgoingSms(message);
            }
        }
    }

    @Override
    public void onIncomingMms(MmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.info("onIncomingMms : ".concat(String.valueOf(message.getNativeProviderId())));
            sLogger.info("listeners size : ".concat(String.valueOf(mXmsObserverListeners.size())));
        }
        synchronized (mXmsObserverListeners) {
            for (XmsObserverListener listener : mXmsObserverListeners) {
                listener.onIncomingMms(message);
            }
        }
    }

    @Override
    public void onOutgoingMms(MmsDataObject message) throws FileAccessException {
        if (sLogger.isActivated()) {
            sLogger.info("onOutgoingMms : ".concat(String.valueOf(message.getNativeProviderId())));
            sLogger.info("listeners size : ".concat(String.valueOf(mXmsObserverListeners.size())));
        }
        synchronized (mXmsObserverListeners) {
            for (XmsObserverListener listener : mXmsObserverListeners) {
                listener.onOutgoingMms(message);
            }
        }
    }

    @Override
    public void onXmsMessageStateChanged(Long nativeProviderId, String mimeType, State state) {
        if (state == null) {
            return;
        }
        synchronized (mXmsObserverListeners) {
            for (XmsObserverListener listener : mXmsObserverListeners) {
                listener.onXmsMessageStateChanged(nativeProviderId, mimeType, state);
            }
        }
    }

}
