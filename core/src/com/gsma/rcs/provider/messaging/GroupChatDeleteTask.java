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

import com.gsma.rcs.core.cms.service.CmsManager;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.utils.logger.Logger;

import java.util.Set;

/**
 * Deletion task for group chats.
 */
public class GroupChatDeleteTask extends DeleteTask.NotGrouped {

    private static final Logger sLogger = Logger.getLogger(GroupChatDeleteTask.class.getName());

    private static final String SELECTION_GROUPDELIVERY_BY_CHATID = GroupDeliveryInfoData.KEY_CHAT_ID + "=?";

    private final ChatServiceImpl mChatService;

    private final InstantMessagingService mImService;
    private final CmsManager mCmsManager;

    /**
     * Deletion of all group chats.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param cmsManager the CMS manager
     */
    public GroupChatDeleteTask(ChatServiceImpl chatService, InstantMessagingService imService,
            LocalContentResolver contentResolver, CmsManager cmsManager) {
        super(contentResolver, GroupChatData.CONTENT_URI, GroupChatData.KEY_CHAT_ID, null);
        mChatService = chatService;
        mImService = imService;
        mCmsManager = cmsManager;
    }

    /**
     * Deletion of a specific group chat.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param chatId the group chat id
     * @param cmsManager the CMS manager
     */
    public GroupChatDeleteTask(ChatServiceImpl chatService, InstantMessagingService imService,
            LocalContentResolver contentResolver, String chatId, CmsManager cmsManager) {
        super(contentResolver, GroupChatData.CONTENT_URI, GroupChatData.KEY_CHAT_ID, null,
                chatId);
        mChatService = chatService;
        mImService = imService;
        mCmsManager = cmsManager;
    }

    @Override
    protected void onRowDelete(String chatId) throws PayloadException {
        GroupChatSession session = mImService.getGroupChatSession(chatId);
        if (session == null) {
            mLocalContentResolver.delete(GroupDeliveryInfoData.CONTENT_URI,
                    SELECTION_GROUPDELIVERY_BY_CHATID, new String[] {
                        chatId
                    });
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
        mLocalContentResolver.delete(GroupDeliveryInfoData.CONTENT_URI,
                SELECTION_GROUPDELIVERY_BY_CHATID, new String[] {
                    chatId
                });
    }

    @Override
    protected void onCompleted(Set<String> deletedIds) {
        mChatService.broadcastGroupChatsDeleted(deletedIds);
        for(String deletedId : deletedIds){
            mCmsManager.getGroupChatEventHandler().onDeleteGroupChat(deletedId);
        }
    }

}
