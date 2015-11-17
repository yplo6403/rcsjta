/*
 * ******************************************************************************
 *  * Software Name : RCS IMS Stack
 *  *
 *  * Copyright (C) 2010 France Telecom S.A.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package com.gsma.services.rcs.cms;

import com.gsma.services.rcs.contact.ContactId;

import java.util.List;

/**
 * XMS message event listener
 *
 * @author Philippe LEMORDANT
 */
public abstract class XmsMessageListener {

    /**
     * Callback called when the XMS message state/reasonCode is changed.
     *
     * @param contact    Contact ID
     * @param mimeType   mime type
     * @param messageId  Id of XMS message
     * @param state      State of the XMS message
     * @param reasonCode Reason code
     */
    public abstract void onStateChanged(ContactId contact, String mimeType, String messageId, XmsMessage.State state,
                                        XmsMessage.ReasonCode reasonCode);

    /**
     * Callback called when a delete operation completed that resulted in that one or several XMS
     * messages were deleted specified by the message ID parameter corresponding to a
     * specific contact.
     *
     * @param contact    Contact ID
     * @param messageIds ids of those deleted XMS messages
     */
    public abstract void onDeleted(ContactId contact, List<String> messageIds);
}
