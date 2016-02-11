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

package com.gsma.rcs.core.cms.event;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.event.framework.EventFramework;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.cms.xms.observer.XmsObserverListener;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage.ReasonCode;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;

public class XmsEventHandler implements XmsMessageListener, XmsObserverListener {

    private static final Logger sLogger = Logger.getLogger(XmsEventHandler.class.getSimpleName());
    private final XmsLog mXmsLog;
    private final CmsLog mCmsLog;
    private final RcsSettings mSettings;
    private final CmsServiceImpl mCmsService;
    private EventFramework mEventFramework;

    /**
     * Default constructor
     *
     * @param cmsLog the IMAP log accessor
     * @param xmsLog the XMS log accessor
     * @param settings the RCS settings accessor
     * @param cmsService the CMS service impl
     */
    public XmsEventHandler(CmsLog cmsLog, XmsLog xmsLog, RcsSettings settings,
            CmsServiceImpl cmsService) {
        mXmsLog = xmsLog;
        mCmsLog = cmsLog;
        mSettings = settings;
        mCmsService = cmsService;
    }

    public void setEventFramework(EventFramework eventFramework) {
        mEventFramework = eventFramework;
    }

    @Override
    public void onIncomingSms(SmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.debug("onIncomingSms: ".concat(message.toString()));
        }
        mXmsLog.addSms(message);
        String messageId = message.getMessageId();
        mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mSettings,
                message.getContact()), CmsObject.ReadStatus.UNREAD,
                CmsObject.DeleteStatus.NOT_DELETED,
                mSettings.getMessageStorePushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.SMS, messageId, message.getNativeProviderId()));
        mCmsService.broadcastNewMessage(message.getMimeType(), messageId);
        if (mEventFramework != null) {
            mEventFramework.pushSmsMessage(message.getContact());
        }
    }

    @Override
    public void onOutgoingSms(SmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.debug("onOutgoingSms ".concat(message.toString()));
        }
        mXmsLog.addSms(message);
        mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mSettings,
                message.getContact()), CmsObject.ReadStatus.READ,
                CmsObject.DeleteStatus.NOT_DELETED,
                mSettings.getMessageStorePushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.SMS, message.getMessageId(), message.getNativeProviderId()));
        if (mEventFramework != null) {
            mEventFramework.pushSmsMessage(message.getContact());
        }

    }

    @Override
    public void onDeleteSmsFromNativeApp(long nativeProviderId) {
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getXmsMessage(nativeProviderId, MimeType.TEXT_MESSAGE);
            if (!cursor.moveToNext()) {
                return;
            }
            String messageId = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID));
            mCmsService.deleteXmsMessageById(messageId);
        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public void onIncomingMms(MmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.debug("onIncomingMms ".concat(message.toString()));
        }
        mXmsLog.addIncomingMms(message);
        String msgId = message.getMessageId();
        mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mSettings,
                message.getContact()), ReadStatus.UNREAD, CmsObject.DeleteStatus.NOT_DELETED,
                mSettings.getMessageStorePushMms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.MMS, msgId, message.getNativeProviderId()));
        mCmsService.broadcastNewMessage(message.getMimeType(), msgId);
        if (mEventFramework != null) {
            mEventFramework.pushMmsMessage(message.getContact());
        }
    }

    @Override
    public void onOutgoingMms(MmsDataObject message) throws FileAccessException {
        if (sLogger.isActivated()) {
            sLogger.debug("onOutgoingMms ".concat(message.toString()));
        }
        /*
         * Checks if an outgoing MMS already exists in local provider having a messageId equals to
         * the transactionId.
         */
        if (!mXmsLog.isMessagePersisted(message.getTransId())) {
            mXmsLog.addOutgoingMms(message);
            mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mSettings,
                    message.getContact()), ReadStatus.READ, CmsObject.DeleteStatus.NOT_DELETED,
                    mSettings.getMessageStorePushMms() ? PushStatus.PUSH_REQUESTED
                            : PushStatus.PUSHED, MessageType.MMS, message.getMessageId(), message
                            .getNativeProviderId()));
            if (mEventFramework != null) {
                mEventFramework.pushMmsMessage(message.getContact());
            }
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
            String messageId = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID));
            mCmsService.deleteXmsMessageById(messageId);

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
            String messageId = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID));
            String number = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.CONTACT));
            mXmsLog.updateState(messageId, state);
            ContactId contact = ContactUtil.createContactIdFromTrustedData(number);
            mCmsService.broadcastMessageStateChanged(contact, mimeType, messageId, state,
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
            int messageIdIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.MIME_TYPE);
            int contactIdIx = cursor.getColumnIndexOrThrow(XmsMessageLog.CONTACT);
            while (cursor.moveToNext()) {
                String number = cursor.getString(contactIdIx);
                String messageId = cursor.getString(messageIdIdx);
                String mimeType = cursor.getString(mimeTypeIdx);
                MessageType messageType = MessageType.SMS;
                if (MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)) {
                    messageType = MessageType.MMS;
                }
                mCmsLog.updateReadStatus(messageType, messageId,
                        CmsObject.ReadStatus.READ_REPORT_REQUESTED);
                mXmsLog.markMessageAsRead(messageId);
                ContactId contact = ContactUtil.createContactIdFromTrustedData(number);
                mCmsService.broadcastMessageStateChanged(contact, mimeType, messageId,
                        State.DISPLAYED, ReasonCode.UNSPECIFIED);
            }
        } finally {
            CursorUtil.close(cursor);
        }
        if (mEventFramework != null) {
            mEventFramework.updateFlagsForXms();
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
            int messageIdIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID);
            while (cursor.moveToNext()) {
                String messageId = cursor.getString(messageIdIdx);
                mCmsService.deleteXmsMessageById(messageId);
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
        mCmsLog.updateReadStatus(messageType, messageId, CmsObject.ReadStatus.READ_REPORT_REQUESTED);
        mXmsLog.markMessageAsRead(messageId);
        if (mEventFramework != null) {
            mEventFramework.updateFlagsForXms();
        }
    }

    @Override
    public void onReadXmsConversation(ContactId contactId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onReadXmsConversation ".concat(contactId.toString()));
        }
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getXmsMessages(contactId);
            int messageIdIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(XmsMessageLog.MIME_TYPE);
            while (cursor.moveToNext()) {
                String messageId = cursor.getString(messageIdIdx);
                String mimeType = cursor.getString(mimeTypeIdx);
                MessageType messageType = MessageType.SMS;
                if (MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)) {
                    messageType = MessageType.MMS;
                }
                mCmsLog.updateReadStatus(messageType, messageId, ReadStatus.READ_REPORT_REQUESTED);
            }
        } finally {
            CursorUtil.close(cursor);
        }
        mXmsLog.markConversationAsRead(contactId);
        if (mEventFramework != null) {
            mEventFramework.updateFlagsForXms();
        }
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
        mCmsLog.updateDeleteStatus(messageType, messageId,
                CmsObject.DeleteStatus.DELETED_REPORT_REQUESTED);
        if (mEventFramework != null) {
            mEventFramework.updateFlagsForXms();
        }
    }

}
