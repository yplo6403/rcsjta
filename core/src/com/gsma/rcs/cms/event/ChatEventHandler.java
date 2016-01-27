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

package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.DeleteStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.im.chat.ChatError;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSessionListener;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

public class ChatEventHandler implements OneToOneChatSessionListener, ChatMessageListener {

    private static final Logger sLogger = Logger.getLogger(ChatEventHandler.class.getSimpleName());
    protected final MessagingLog mMessagingLog;
    protected final ImapLog mImapLog;
    protected final RcsSettings mSettings;

    /**
     * Default constructor
     *
     * @param imapLog the IMAP log accessor
     * @param messagingLog the messaging log accessor
     * @param settings the RCS settings accessor
     */
    public ChatEventHandler(ImapLog imapLog, MessagingLog messagingLog, RcsSettings settings) {
        mMessagingLog = messagingLog;
        mImapLog = imapLog;
        mSettings = settings;
    }

    @Override
    public void onMessageReceived(ChatMessage msg, boolean imdnDisplayedRequested,
            boolean deliverySuccess) {
        if (sLogger.isActivated()) {
            sLogger.debug("onMessageReceived: ".concat(msg.toString()));
        }
        mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings,
                msg.getRemoteContact()), ReadStatus.UNREAD, MessageData.DeleteStatus.NOT_DELETED,
                PushStatus.PUSHED, MessageType.CHAT_MESSAGE, msg.getMessageId(), null));
    }

    @Override
    public void onIsComposingEventReceived(ContactId contact, boolean status) {

    }

    @Override
    public void onMessageSent(String msgId, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.debug("onMessageSent: ".concat(msgId));
        }
        ContactId contact = mMessagingLog.getMessageContact(msgId);
        mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings, contact),
                ReadStatus.READ, MessageData.DeleteStatus.NOT_DELETED, PushStatus.PUSHED,
                MessageType.CHAT_MESSAGE, msgId, null));
    }

    @Override
    public void onMessageFailedSend(String msgId, String mimeType) {

    }

    @Override
    public void onMessageDeliveryStatusReceived(ContactId contact, ImdnDocument imdn, String imdnId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onMessageDeliveryStatusReceived: ".concat(imdnId));
        }
        mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings, contact),
                ReadStatus.READ, MessageData.DeleteStatus.NOT_DELETED, PushStatus.PUSHED,
                MessageType.IMDN, imdnId, null));
    }

    @Override
    public void onDeliveryStatusReceived(String contributionId, ContactId contact,
            ImdnDocument imdn, String imdnId) {
        onMessageDeliveryStatusReceived(contact, imdn, imdnId);
    }

    @Override
    public void onChatMessageDisplayReportSent(String msgId) {

    }

    @Override
    public void onSessionInvited(ContactId contact) {

    }

    @Override
    public void onSessionAutoAccepted(ContactId contact) {

    }

    @Override
    public void onDeliveryReportSendViaMsrpFailure(String msgId, ContactId contact,
            TypeMsrpChunk chunktype) {

    }

    @Override
    public void onImError(ChatError error, String msgId, String mimeType) {

    }

    @Override
    public void onSessionStarted(ContactId contact) {

    }

    @Override
    public void onSessionAborted(ContactId contact, TerminationReason reason) {

    }

    @Override
    public void onSessionRejected(ContactId contact, TerminationReason reason) {

    }

    @Override
    public void onSessionAccepting(ContactId contact) {

    }

    @Override
    public void onReadChatMessage(String messageId) {
        mImapLog.updateReadStatus(MessageType.CHAT_MESSAGE, messageId,
                ReadStatus.READ_REPORT_REQUESTED);
    }

    @Override
    public void onDeleteChatMessage(String messageId) {
        mImapLog.updateDeleteStatus(MessageType.CHAT_MESSAGE, messageId,
                DeleteStatus.DELETED_REPORT_REQUESTED);
    }
}
