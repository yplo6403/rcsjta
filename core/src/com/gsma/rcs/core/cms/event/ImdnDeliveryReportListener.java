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

import com.gsma.services.rcs.contact.ContactId;

/**
 * Interface used to take into account when an imdn delivery message is sent from ImdnManager
 */
public interface ImdnDeliveryReportListener {

    /**
     * A delivery report message was received or sent
     *
     * @param contact
     * @param messageId the chat message ID
     * @param imdnMessageId the IMDN message ID
     */
    void onDeliveryReport(ContactId contact, String messageId, String imdnMessageId);

    /**
     * A delivery report message was received or sent
     *
     * @param chatId
     * @param messageId the chat message ID
     * @param imdnMessageId the message ID
     */
    void onDeliveryReport(String chatId, String messageId, String imdnMessageId);

    /**
     * A delivery report message was received or sent
     * 
     * @param contact
     * @param chatId
     * @param messageId the chat message ID
     * @param imdnMessageId the message ID
     */
    void onDeliveryReport(String chatId, ContactId contact, String messageId, String imdnMessageId);

}
