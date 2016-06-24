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

import java.util.Set;

public interface GroupChatListener {

    /**
     * Called when a new group chat conversation is created
     *
     * @param conversationId conversation ID
     * @param contributionId contribution ID
     */
    void onCreateGroupChat(String conversationId, String contributionId);

    /**
     * Take into account deletion of group chat messages
     *
     * @param chatId the chat ID
     * @param msgIds the messages ID
     */
    void onDeleteGroupChatMessages(String chatId, Set<String> msgIds);

    /**
     * Called when group chat conversation is deleted
     *
     * @param chatId the chat ID
     */
    void onDeleteGroupChat(String chatId);
}
