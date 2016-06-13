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

import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Interface used to take into account when an imdn delivery message is sent from ImdnManager
 */
public interface ImdnDeliveryReportListener {

    /**
     * A delivery report message was received or sent
     *
     * @param contact the remote contact
     * @param msId the chat message ID
     * @param imdnMsgId the IMDN message ID
     */
    void onDeliveryReport(ContactId contact, String msId, String imdnMsgId,
            ImdnDocument.DeliveryStatus status);

    /**
     * A delivery report message was received or sent
     *
     * @param chatId the chat ID
     * @param mssId the chat message ID
     * @param imdnMsgId the message ID
     */
    void onDeliveryReport(String chatId, String mssId, String imdnMsgId,
            ImdnDocument.DeliveryStatus status);

    /**
     * A delivery report message was received or sent
     * 
     * @param contact the remote contact
     * @param chatId the chat ID
     * @param msgId the chat message ID
     * @param imdnMsgId the message ID
     */
    void onDeliveryReport(String chatId, ContactId contact, String msgId, String imdnMsgId,
            ImdnDocument.DeliveryStatus status);

}
