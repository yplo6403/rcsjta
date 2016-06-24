/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.DeliveryExpirationManager;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

public class OneToOneChatMessageDeleteTask extends DeleteTask.GroupedByContactId {

    private static final Logger sLogger = Logger.getLogger(OneToOneChatMessageDeleteTask.class
            .getName());

    private static final String SEL_ONETOONE_CHATMESSAGES = MessageData.KEY_CHAT_ID + "="
            + MessageData.KEY_CONTACT;

    private static final String SEL_ONETOONE_CHATMESSAGES_BY_CHATID = MessageData.KEY_CHAT_ID
            + "=?";

    private final ChatServiceImpl mChatService;

    private final InstantMessagingService mImService;
    private final CmsSessionController mCmsSessionCtrl;
    private final CmsLog mCmsLog;

    /**
     * Constructor to delete all one to one chat messages.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param cmsSessionCtrl the CMS session controller
     */
    public OneToOneChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            CmsSessionController cmsSessionCtrl) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID, true,
                MessageData.KEY_CONTACT, false, SEL_ONETOONE_CHATMESSAGES);
        mChatService = chatService;
        mImService = imService;
        setAllAtOnce(true);
        mCmsSessionCtrl = cmsSessionCtrl;
        mCmsLog = null;
        if (sLogger.isActivated()) {
            sLogger.debug("OneToOneChatMessageDeleteTask delete all messages");
        }
    }

    /**
     * Constructor to delete a specific chat message.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param messageId the message id
     * @param cmsSessionController the CMS session controller
     */
    public OneToOneChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            String messageId, CmsSessionController cmsSessionController) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID, true,
                MessageData.KEY_CONTACT, true, null, messageId);
        mChatService = chatService;
        mImService = imService;
        mCmsSessionCtrl = cmsSessionController;
        mCmsLog = null;
        if (sLogger.isActivated()) {
            sLogger.debug("OneToOneChatMessageDeleteTask delete ID=" + messageId);
        }
    }

    /**
     * Constructor to process event of deletion for a specific message ID
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param messageId the message id
     * @param cmsLog the CMS log accessor
     */
    public OneToOneChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            String messageId, CmsLog cmsLog) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID, true,
                MessageData.KEY_CONTACT, true, null, messageId);
        mChatService = chatService;
        mImService = imService;
        mCmsSessionCtrl = null;
        mCmsLog = cmsLog;
        if (sLogger.isActivated()) {
            sLogger.debug("OneToOneChatMessageDeleteTask delete ID=" + messageId);
        }
    }

    /**
     * Constructor to delete a specific one to one conversation.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param cmsSessionController the CMS session controller
     */
    public OneToOneChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            ContactId contact, CmsSessionController cmsSessionController) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID, true,
                MessageData.KEY_CONTACT, false, SEL_ONETOONE_CHATMESSAGES_BY_CHATID, contact
                        .toString());
        mChatService = chatService;
        mImService = imService;
        setAllAtOnce(true);
        mCmsSessionCtrl = cmsSessionController;
        mCmsLog = null;
        if (sLogger.isActivated()) {
            sLogger.debug("OneToOneChatMessageDeleteTask delete messages with contact " + contact);
        }
    }

    @Override
    protected void onRowDelete(ContactId contact, String msgId) throws PayloadException {
        if (isSingleRowDelete()) {
            return;
        }
        ChatSession session = mImService.getOneToOneChatSession(contact);
        if (session == null) {
            mChatService.removeOneToOneChat(contact);
            return;
        }
        try {
            session.deleteSession();

        } catch (NetworkException e) {
            /*
             * If network is lost during a delete operation the remaining part of the delete
             * operation (delete from persistent storage) can succeed to 100% anyway since delete
             * can be executed anyway while no network connectivity is present and still succeed.
             */
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        }
        mChatService.removeOneToOneChat(contact);
    }

    @Override
    protected void onCompleted(ContactId contact, Set<String> msgIds) {
        DeliveryExpirationManager expirationManager = mImService.getDeliveryExpirationManager();
        for (String messageId : msgIds) {
            expirationManager.cancelDeliveryTimeoutAlarm(messageId);
        }
        if (mCmsSessionCtrl != null) {
            mCmsSessionCtrl.getChatEventHandler().onDeleteChatMessages(contact, msgIds);
        } else {
            for (String delId : msgIds) {
                mCmsLog.updateRcsDeleteStatus(MessageType.CHAT_MESSAGE, delId,
                        DeleteStatus.DELETED, null);
            }
        }
        mChatService.broadcastOneToOneMessagesDeleted(contact, msgIds);
    }
}
