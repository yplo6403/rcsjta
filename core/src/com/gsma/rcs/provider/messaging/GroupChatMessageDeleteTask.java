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
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.utils.logger.Logger;

import java.util.Set;

public class GroupChatMessageDeleteTask extends DeleteTask.GroupedByChatId {

    private static final Logger sLogger = Logger.getLogger(GroupChatMessageDeleteTask.class
            .getName());

    private static final String SEL_GROUP_CHATMESSAGES = MessageData.KEY_CHAT_ID + "<>"
            + MessageData.KEY_CONTACT + " OR " + MessageData.KEY_CONTACT + " IS NULL";

    private static final String SEL_CHATMESSAGES_BY_CHATID = MessageData.KEY_CHAT_ID + "=?";

    private final ChatServiceImpl mChatService;

    private final InstantMessagingService mImService;
    private final CmsSessionController mCmsSessionCtrl;
    private final CmsLog mCmsLog;

    /**
     * Deletion of all group chat messages.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param cmsSessionController the CMS session controller
     */
    public GroupChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            CmsSessionController cmsSessionController) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                MessageData.KEY_CHAT_ID, false, SEL_GROUP_CHATMESSAGES);
        mChatService = chatService;
        mImService = imService;
        mCmsSessionCtrl = cmsSessionController;
        mCmsLog = null;
    }

    /**
     * Deletion of all chat messages from the specified group chat id.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param chatId the chat id
     * @param cmsSessionCtrl the CMS session controller
     */
    public GroupChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver, String chatId,
            CmsSessionController cmsSessionCtrl) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                MessageData.KEY_CHAT_ID, false, SEL_CHATMESSAGES_BY_CHATID, chatId);
        mChatService = chatService;
        mImService = imService;
        mCmsSessionCtrl = cmsSessionCtrl;
        mCmsLog = null;
    }

    /**
     * Constructor to process a delete action for a specific message.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param chatId the chat id (optional, can be null)
     * @param messageId the message id
     * @param cmsSessionCtrl the CMS session controller
     */
    public GroupChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver, String chatId,
            String messageId, CmsSessionController cmsSessionCtrl) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                MessageData.KEY_CHAT_ID, true, null, messageId);
        mChatService = chatService;
        mImService = imService;
        mCmsSessionCtrl = cmsSessionCtrl;
        mCmsLog = null;
    }

    /**
     * Constructor to process a delete event for a specific message.
     *
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param messageId the message id
     * @param cmsLog the CMS log accessor
     */
    public GroupChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            String messageId, CmsLog cmsLog) {
        super(contentResolver, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                MessageData.KEY_CHAT_ID, true, null, messageId);
        mChatService = chatService;
        mImService = imService;
        mCmsSessionCtrl = null;
        mCmsLog = cmsLog;
    }

    @Override
    protected void onRowDelete(String chatId, String msgId) throws PayloadException {
        if (isSingleRowDelete()) {
            return;
        }
        ChatSession session = mImService.getGroupChatSession(chatId);
        if (session == null) {
            mChatService.removeGroupChat(chatId);
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
        mChatService.removeGroupChat(chatId);
    }

    @Override
    protected void onCompleted(String chatId, Set<String> msgIds) {
        mChatService.broadcastGroupChatMessagesDeleted(chatId, msgIds);
        if (mCmsSessionCtrl != null) {
            mCmsSessionCtrl.getGroupChatEventHandler().onDeleteGroupChatMessages(chatId, msgIds);
        } else {
            for (String delId : msgIds) {
                mCmsLog.updateRcsDeleteStatus(MessageType.CHAT_MESSAGE, delId,
                        DeleteStatus.DELETED, null);
            }
        }
    }

}
