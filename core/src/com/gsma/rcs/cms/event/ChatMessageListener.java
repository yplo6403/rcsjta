/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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

/**
 * Interface used to take into account changes for chat messages.
 */
public interface ChatMessageListener {

    /**
     *  Take into account that a chat message is read
     *
     * @param messageId
     */
    void onReadChatMessage(String messageId);

    /**
     *  Take into account that a chat message is deleted
     *
     * @param messageId
     */
    void onDeleteChatMessage(String messageId);
}
