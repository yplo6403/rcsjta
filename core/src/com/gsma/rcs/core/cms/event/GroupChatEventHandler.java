/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.gsma.rcs.core.cms.event;

import com.gsma.rcs.core.cms.event.framework.EventReportingFrameworkManager;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.service.im.chat.ChatError;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSessionListener;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Map;
import java.util.Set;

public class GroupChatEventHandler extends ChatEventHandler implements GroupChatSessionListener,
        GroupChatListener {

    private static final Logger sLogger = Logger.getLogger(GroupChatEventHandler.class
            .getSimpleName());

    /**
     * Default constructor
     *
     * @param eventFrameworkManager the event framework handler
     * @param cmsLog the IMAP log accessor
     * @param messagingLog the messaging accessor
     * @param settings the RCS settings accessor
     */
    public GroupChatEventHandler(EventReportingFrameworkManager eventFrameworkManager,
            CmsLog cmsLog, MessagingLog messagingLog, RcsSettings settings,
            ImdnDeliveryReportListener imdnDeliveryReportListener) {
        super(eventFrameworkManager, cmsLog, messagingLog, settings, imdnDeliveryReportListener);
    }

    @Override
    public void onCreateGroupChat(String conversationId, String contributionId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onCreateGroupChat: " + conversationId + "/" + contributionId);
        }
        mCmsLog.addMessage(new CmsObject(CmsUtils.groupChatToCmsFolder(mSettings, conversationId,
                contributionId), ReadStatus.UNREAD, CmsObject.DeleteStatus.NOT_DELETED,
                PushStatus.PUSHED, MessageType.CPM_SESSION, contributionId, null));
    }

    @Override
    public void onDeleteGroupChatMessages(String chatId, Set<String> msgIds) {
        for (String msgId : msgIds) {
            mCmsLog.updateDeleteStatus(MessageType.CHAT_MESSAGE, msgId,
                    DeleteStatus.DELETED_REPORT_REQUESTED);
        }
        if (mEventFrameworkManager != null) {
            mEventFrameworkManager.updateFlagsForGroupChat(chatId);
        }
    }

    @Override
    public void onDeleteGroupChat(String chatId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteGroupChat: " + chatId);
        }
        mCmsLog.updateDeleteStatus(CmsUtils.groupChatToCmsFolder(mSettings, chatId, chatId),
                DeleteStatus.DELETED_REPORT_REQUESTED);
    }

    @Override
    public void onMessageReceived(ChatMessage msg, boolean imdnDisplayedRequested,
            boolean deliverySuccess) {
        if (sLogger.isActivated()) {
            sLogger.debug("onMessageReceived: ".concat(msg.toString()));
        }
        String chatId = mMessagingLog.getMessageChatId(msg.getMessageId());
        mCmsLog.addMessage(new CmsObject(CmsUtils.groupChatToCmsFolder(mSettings, chatId, chatId),
                ReadStatus.UNREAD, CmsObject.DeleteStatus.NOT_DELETED, PushStatus.PUSHED,
                MessageType.CHAT_MESSAGE, msg.getMessageId(), null));
    }

    @Override
    public void onMessageSent(String msgId, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.debug("onMessageSent: ".concat(msgId));
        }
        String chatId = mMessagingLog.getMessageChatId(msgId);
        mCmsLog.addMessage(new CmsObject(CmsUtils.groupChatToCmsFolder(mSettings, chatId, chatId),
                ReadStatus.READ, CmsObject.DeleteStatus.NOT_DELETED, PushStatus.PUSHED,
                MessageType.CHAT_MESSAGE, msgId, null));
    }

    @Override
    public void onMessageDeliveryStatusReceived(ContactId contact, ImdnDocument imdn, String imdnId) {
    }

    @Override
    public void onDeliveryStatusReceived(String contributionId, ContactId contact,
            ImdnDocument imdn, String imdnMessageId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onMessageDeliveryStatusReceived: ".concat(imdnMessageId));
        }
        mImdnDeliveryReportListener.onDeliveryReport(contributionId, imdnMessageId);
    }

    @Override
    public void onConferenceEventReceived(ContactId contact, ParticipantStatus status,
            long timestamp) {

    }

    @Override
    public void onSessionInvited(ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, long timestamp) {

    }

    @Override
    public void onSessionAutoAccepted(ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, long timestamp) {

    }

    @Override
    public void onParticipantsUpdated(Map<ContactId, ParticipantStatus> updatedParticipants,
            Map<ContactId, ParticipantStatus> allParticipants) {

    }

    @Override
    public void onDeliveryReportSendViaMsrpFailure(String msgId, String chatId,
            TypeMsrpChunk chunktype) {
    }

    @Override
    public void onImError(ChatError error) {

    }
}
