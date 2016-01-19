/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2015 France Telecom S.A.
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

package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.observer.XmsObserverListener;
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
import com.gsma.rcs.service.broadcaster.IXmsMessageEventBroadcaster;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage.ReasonCode;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class XmsEventHandler implements XmsMessageListener, XmsObserverListener {

    private static final Logger sLogger = Logger.getLogger(XmsEventHandler.class.getSimpleName());
    private final XmsLog mXmsLog;
    private final ImapLog mImapLog;
    private final RcsSettings mSettings;
    private final IXmsMessageEventBroadcaster mXmsMessageEventBroadcaster;

    /**
     * Default constructor
     *
     * @param imapLog the IMAP log accessor
     * @param xmsLog the XMS log accessor
     * @param settings the RCS settings accessor
     */
    public XmsEventHandler(ImapLog imapLog, XmsLog xmsLog, RcsSettings settings,
            IXmsMessageEventBroadcaster xmsMessageEventBroadcaster) {
        mXmsLog = xmsLog;
        mImapLog = imapLog;
        mSettings = settings;
        mXmsMessageEventBroadcaster = xmsMessageEventBroadcaster;
    }

    /********************************************************************/
    /****************** Native SMS Events *******************************/
    /********************************************************************/

    @Override
    public void onIncomingSms(SmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.debug("onIncomingSms: ".concat(message.toString()));
        }
        mXmsLog.addSms(message);
        mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings,
                message.getContact()), MessageData.ReadStatus.UNREAD,
                MessageData.DeleteStatus.NOT_DELETED,
                mSettings.getCmsPushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.SMS, message.getMessageId(), message.getNativeProviderId()));
        mXmsMessageEventBroadcaster.broadcastNewMessage(message.getMimeType(),
                message.getMessageId());
    }

    @Override
    public void onOutgoingSms(SmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.debug("onOutgoingSms ".concat(message.toString()));
        }
        mXmsLog.addSms(message);
        mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings,
                message.getContact()), MessageData.ReadStatus.READ,
                MessageData.DeleteStatus.NOT_DELETED,
                mSettings.getCmsPushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.SMS, message.getMessageId(), message.getNativeProviderId()));
    }

    @Override
    public void onDeleteSmsFromNativeApp(long nativeProviderId) {
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getXmsMessage(nativeProviderId, MimeType.TEXT_MESSAGE);
            if (!cursor.moveToNext()) {
                return;
            }
            String contact = cursor.getString(cursor.getColumnIndex(XmsMessageLog.CONTACT));
            String messageId = cursor.getString(cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID));
            mXmsLog.deleteXmsMessage(messageId);
            mImapLog.updateDeleteStatus(MessageType.SMS, messageId,
                    MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);

            Set<String> messageIds = new HashSet<>(Collections.singletonList(messageId));
            mXmsMessageEventBroadcaster.broadcastMessageDeleted(
                    ContactUtil.createContactIdFromTrustedData(contact), messageIds);
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /********************************************************************/
    /****************** Native MMS Events *******************************/
    /********************************************************************/
    @Override
    public void onIncomingMms(MmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.debug("onIncomingMms ".concat(message.toString()));
        }
        mXmsLog.addMms(message);
        mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings,
                message.getContact()), ReadStatus.UNREAD, MessageData.DeleteStatus.NOT_DELETED,
                mSettings.getCmsPushMms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.MMS, message.getMessageId(), message.getNativeProviderId()));
        mXmsMessageEventBroadcaster.broadcastNewMessage(message.getMimeType(),
                message.getMessageId());
    }

    @Override
    public void onOutgoingMms(MmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.debug("onOutgoingMms ".concat(message.toString()));
        }
        /*
         * Checks if an outgoing MMS already exists in local provider having a messageId equals to
         * the transactionId.
         */
        if (!mXmsLog.isMessagePersisted(message.getTransId())) {
            mXmsLog.addMms(message);
            mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings,
                    message.getContact()), ReadStatus.READ, MessageData.DeleteStatus.NOT_DELETED,
                    mSettings.getCmsPushMms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                    MessageType.MMS, message.getMessageId(), message.getNativeProviderId()));
        }
    }

    @Override
    public void onDeleteMmsFromNativeApp(String mmsId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteNativeMms ".concat(mmsId));
        }
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getMmsMessage(mmsId);
            if (!cursor.moveToNext()) {
                return;
            }
            String contact = cursor.getString(cursor.getColumnIndex(XmsMessageLog.CONTACT));
            String messageId = cursor.getString(cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID));
            mXmsLog.deleteXmsMessage(messageId);
            mImapLog.updateDeleteStatus(MessageType.MMS, messageId,
                    MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);

            Set<String> messageIds = new HashSet<>(Collections.singletonList(messageId));
            mXmsMessageEventBroadcaster.broadcastMessageDeleted(
                    ContactUtil.createContactIdFromTrustedData(contact), messageIds);
        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public void onXmsMessageStateChanged(Long nativeProviderId, String mimeType, State state) {
        if (sLogger.isActivated()) {
            sLogger.debug("onXmsMessageStateChanged:" + nativeProviderId + "," + mimeType + ","
                    + state);
        }
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getXmsMessage(nativeProviderId, mimeType);
            if (!cursor.moveToNext()) {
                return;
            }
            String messageId = cursor.getString(cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID));
            String number = cursor.getString(cursor.getColumnIndex(XmsMessageLog.CONTACT));
            mXmsLog.updateState(messageId, state);
            mXmsMessageEventBroadcaster.broadcastMessageStateChanged(
                    ContactUtil.createContactIdFromTrustedData(number), mimeType, messageId, state,
                    ReasonCode.UNSPECIFIED);
        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public void onReadXmsConversationFromNativeApp(long nativeThreadId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onReadNativeConversation " + nativeThreadId);
        }
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getUnreadXmsMessages(nativeThreadId);
            int messageIdIdx = cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID);
            int mimeTypeIdx = cursor.getColumnIndex(XmsMessageLog.MIME_TYPE);
            int contactIdIx = cursor.getColumnIndex(XmsMessageLog.CONTACT);
            while (cursor.moveToNext()) {
                String contact = cursor.getString(contactIdIx);
                String messageId = cursor.getString(messageIdIdx);
                String mimeType = cursor.getString(mimeTypeIdx);
                MessageType messageType = MessageType.SMS;
                if (MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)) {
                    messageType = MessageType.MMS;
                }
                mImapLog.updateReadStatus(messageType, messageId,
                        MessageData.ReadStatus.READ_REPORT_REQUESTED);
                mXmsLog.markMessageAsRead(messageId);
                mXmsMessageEventBroadcaster.broadcastMessageStateChanged(
                        ContactUtil.createContactIdFromTrustedData(contact), mimeType, messageId,
                        State.DISPLAYED, ReasonCode.UNSPECIFIED);
            }
        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public void onDeleteXmsConversationFromNativeApp(long nativeThreadId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteNativeConversation " + nativeThreadId);
        }
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getXmsMessages(nativeThreadId);
            int messageIdIdx = cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID);
            int mimeTypeIdx = cursor.getColumnIndex(XmsMessageLog.MIME_TYPE);
            int contactIdIx = cursor.getColumnIndex(XmsMessageLog.CONTACT);
            String contact = null;
            Set<String> messageIds = new HashSet<>();
            while (cursor.moveToNext()) {
                contact = cursor.getString(contactIdIx);
                String messageId = cursor.getString(messageIdIdx);
                String mimeType = cursor.getString(mimeTypeIdx);
                MessageType messageType = MessageType.SMS;
                if (MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)) {
                    messageType = MessageType.MMS;
                }
                mImapLog.updateDeleteStatus(messageType, messageId,
                        MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);
                mXmsLog.deleteXmsMessage(messageId);
                messageIds.add(messageId);
            }
            if (!messageIds.isEmpty()) {
                mXmsMessageEventBroadcaster.broadcastMessageDeleted(
                        ContactUtil.createContactIdFromTrustedData(contact), messageIds);
            }
        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public void onReadXmsMessage(String messageId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onReadXmsMessage ".concat(messageId));
        }
        MessageType messageType = MessageType.SMS;
        String mimeType = mXmsLog.getMimeType(messageId);
        if (MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)) {
            messageType = MessageType.MMS;
        }
        mImapLog.updateReadStatus(messageType, messageId,
                MessageData.ReadStatus.READ_REPORT_REQUESTED);
        mXmsLog.markMessageAsRead(messageId);
    }

    @Override
    public void onReadXmsConversation(ContactId contactId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onReadXmsConversation ".concat(contactId.toString()));
        }
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getXmsMessages(contactId);
            int messageIdIdx = cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID);
            int mimeTypeIdx = cursor.getColumnIndex(XmsMessageLog.MIME_TYPE);
            while (cursor.moveToNext()) {
                String messageId = cursor.getString(messageIdIdx);
                String mimeType = cursor.getString(mimeTypeIdx);
                MessageType messageType = MessageType.SMS;
                if (MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)) {
                    messageType = MessageType.MMS;
                }
                mImapLog.updateReadStatus(messageType, messageId, ReadStatus.READ_REPORT_REQUESTED);
            }
        } finally {
            CursorUtil.close(cursor);
        }
        mXmsLog.markConversationAsRead(contactId);
    }

    @Override
    public void onDeleteXmsMessage(String messageId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteXmsMessage ".concat(messageId));
        }
        String mimeType = mXmsLog.getMimeType(messageId);
        MessageType messageType = MessageType.SMS;
        if (MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)) {
            messageType = MessageType.MMS;
        }
        mImapLog.updateDeleteStatus(messageType, messageId,
                MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);
        mXmsLog.deleteXmsMessage(messageId);
    }

    @Override
    public void onDeleteXmsConversation(ContactId contactId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteXmsConversation ".concat(contactId.toString()));
        }
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getXmsMessages(contactId);
            int messageIdIdx = cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID);
            int mimeTypeIdx = cursor.getColumnIndex(XmsMessageLog.MIME_TYPE);
            while (cursor.moveToNext()) {
                String messageId = cursor.getString(messageIdIdx);
                String mimeType = cursor.getString(mimeTypeIdx);
                MessageType messageType = MessageType.SMS;
                if (MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)) {
                    messageType = MessageType.MMS;
                }
                mImapLog.updateDeleteStatus(messageType, messageId,
                        MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);
            }
        } finally {
            CursorUtil.close(cursor);
        }
        mXmsLog.deleteXmsMessages(contactId);
    }

    @Override
    public void onDeleteAllXmsMessage() {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteAllXmsMessage");
        }
        Map<ContactId, Set<String>> mapContactIdMsgIds = mXmsLog.getMessagesIdsPerContact();
        if (!mapContactIdMsgIds.isEmpty()) {
            mXmsLog.deleteAllEntries();
            mImapLog.updateDeleteStatus(MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);
            for (Map.Entry<ContactId, Set<String>> entry : mapContactIdMsgIds.entrySet()) {
                mXmsMessageEventBroadcaster.broadcastMessageDeleted(entry.getKey(),
                        entry.getValue());
            }
        }
    }

}
