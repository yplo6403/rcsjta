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
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncScheduler;
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

import java.util.Map;
import java.util.Set;

public class XmsEventHandler implements XmsObserverListener {

    private static final Logger sLogger = Logger.getLogger(XmsEventHandler.class.getSimpleName());
    private final XmsLog mXmsLog;
    private final CmsLog mCmsLog;
    private final RcsSettings mSettings;
    private final CmsServiceImpl mCmsService;
    private CmsSyncScheduler mCmsSyncScheduler;

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

    public void setCmsSyncScheduler(CmsSyncScheduler cmsSyncScheduler) {
        mCmsSyncScheduler = cmsSyncScheduler;
    }

    @Override
    public void onIncomingSms(SmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.debug("onIncomingSms: ".concat(message.toString()));
        }
        mXmsLog.addSms(message);
        String folder = CmsUtils.contactToCmsFolder(message.getContact());
        String messageId = message.getMessageId();
        PushStatus pushStatus = mSettings.shouldPushSms() ? PushStatus.PUSH_REQUESTED
                : PushStatus.PUSHED;
        mCmsLog.addMessage(new CmsObject(folder, CmsObject.ReadStatus.UNREAD,
                CmsObject.DeleteStatus.NOT_DELETED, pushStatus, MessageType.SMS, messageId, message
                        .getNativeProviderId()));
        mCmsService.broadcastNewMessage(message.getMimeType(), messageId);
        if (mCmsSyncScheduler != null) {
            mCmsSyncScheduler.schedulePushMessages(message.getContact());
        }
    }

    @Override
    public void onOutgoingSms(SmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.debug("onOutgoingSms ".concat(message.toString()));
        }
        mXmsLog.addSms(message);
        String folder = CmsUtils.contactToCmsFolder(message.getContact());
        PushStatus pushStatus = mSettings.shouldPushSms() ? PushStatus.PUSH_REQUESTED
                : PushStatus.PUSHED;
        mCmsLog.addMessage(new CmsObject(folder, CmsObject.ReadStatus.READ,
                CmsObject.DeleteStatus.NOT_DELETED, pushStatus, MessageType.SMS, message
                        .getMessageId(), message.getNativeProviderId()));
        if (mCmsSyncScheduler != null) {
            mCmsSyncScheduler.schedulePushMessages(message.getContact());
        }
    }

    @Override
    public void onDeleteSmsFromNativeApp(long nativeProviderId) {
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getSmsMessage(nativeProviderId);
            if (!cursor.moveToNext()) {
                return;
            }
            String msgId = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID));
            String number = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.CONTACT));
            ContactId contact = ContactUtil.createContactIdFromTrustedData(number);
            mCmsService.deleteXmsMessageByIdAndContact(contact, msgId);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public void onIncomingMms(MmsDataObject message) {
        String msgId = message.getMessageId();
        ContactId remote = message.getContact();
        if (!mXmsLog.isMessagePersisted(remote, msgId)) {
            if (sLogger.isActivated()) {
                sLogger.debug("onIncomingMms ID=" + msgId + " contact=" + remote);
            }
            mXmsLog.addIncomingMms(message);
            PushStatus pushStatus = mSettings.shouldPushMms() ? PushStatus.PUSH_REQUESTED
                    : PushStatus.PUSHED;
            String folder = CmsUtils.contactToCmsFolder(remote);
            mCmsLog.addMessage(new CmsObject(folder, ReadStatus.UNREAD,
                    CmsObject.DeleteStatus.NOT_DELETED, pushStatus, MessageType.MMS, msgId, message
                            .getNativeProviderId()));
            mCmsService.broadcastNewMessage(message.getMimeType(), msgId);
            if (mCmsSyncScheduler != null) {
                mCmsSyncScheduler.schedulePushMessages(message.getContact());
            }
        } else {
            if (sLogger.isActivated()) {
                sLogger.warn("onIncomingMms is already persisted ID=" + msgId + " contact="
                        + remote);
            }
        }
    }

    @Override
    public void onOutgoingMms(MmsDataObject message) throws FileAccessException {
        /*
         * Checks if an outgoing MMS already exists in local provider
         */
        ContactId contact = message.getContact();
        String msgId = message.getMessageId();
        if (!mXmsLog.isMessagePersisted(contact, msgId)) {
            if (sLogger.isActivated()) {
                sLogger.debug("onOutgoingMms ID=" + msgId + " contact=" + contact);
            }
            mXmsLog.addOutgoingMms(message);
            String folder = CmsUtils.contactToCmsFolder(contact);
            PushStatus pushStatus = mSettings.shouldPushMms() ? PushStatus.PUSH_REQUESTED
                    : PushStatus.PUSHED;
            mCmsLog.addMessage(new CmsObject(folder, ReadStatus.READ,
                    CmsObject.DeleteStatus.NOT_DELETED, pushStatus, MessageType.MMS, msgId, message
                            .getNativeProviderId()));
            if (mCmsSyncScheduler != null) {
                mCmsSyncScheduler.schedulePushMessages(contact);
            }
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("onOutgoingMms is already persisted ID=" + msgId + " contact="
                        + contact);
            }
        }
    }

    @Override
    public void onDeleteMmsFromNativeApp(String mmsId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteNativeMms ".concat(mmsId));
        }
        Set<ContactId> contacts = mXmsLog.getContactsForXmsId(mmsId);
        for (ContactId contact : contacts) {
            mCmsService.deleteXmsMessageByIdAndContact(contact, mmsId);
        }
    }

    @Override
    public void onSmsMessageStateChanged(Long nativeProviderId, State state) {
        if (sLogger.isActivated()) {
            sLogger.debug("onSmsMessageStateChanged ID=" + nativeProviderId + ", state=" + state);
        }
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getSmsMessage(nativeProviderId);
            if (!cursor.moveToNext()) {
                return;
            }
            String messageId = cursor.getString(cursor
                    .getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID));
            String number = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.CONTACT));
            ContactId contact = ContactUtil.createContactIdFromTrustedData(number);
            mXmsLog.updateState(contact, messageId, state);
            mCmsService.broadcastMessageStateChanged(contact, MimeType.TEXT_MESSAGE, messageId,
                    state, ReasonCode.UNSPECIFIED);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public void onReadMmsConversationFromNativeApp(long nativeThreadId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onReadNativeConversation " + nativeThreadId);
        }
        Map<ContactId, Set<String>> unreads = mXmsLog.getUnreadMms(nativeThreadId);
        for (Map.Entry<ContactId, Set<String>> entry : unreads.entrySet()) {
            ContactId contact = entry.getKey();
            Set<String> mmsIds = entry.getValue();
            for (String mmsId : mmsIds) {
                mCmsService.markXmsMessageAsRead_(contact, mmsId);
            }
        }
    }

    @Override
    public void onDeleteMmsConversationFromNativeApp(long nativeThreadId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteNativeConversation " + nativeThreadId);
        }
        Map<ContactId, Set<String>> mmss = mXmsLog.getMmsMessages(nativeThreadId);
        for (Map.Entry<ContactId, Set<String>> entry : mmss.entrySet()) {
            ContactId contact = entry.getKey();
            Set<String> mmsIds = entry.getValue();
            for (String mmsId : mmsIds) {
                mCmsService.deleteXmsMessageByIdAndContact(contact, mmsId);
            }
        }
    }

}
