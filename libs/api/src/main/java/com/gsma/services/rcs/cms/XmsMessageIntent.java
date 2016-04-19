/*
 * ******************************************************************************
 *  * Software Name : RCS IMS Stack
 *  *
 *  * Copyright (C) 2010-2016 Orange.
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

/**
 * Intent for XMS message
 * 
 * @author Philippe LEMORDANT
 */
public class XmsMessageIntent {
    /**
     * Broadcast action: a new XMS message has been received.
     * <p>
     * Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_MESSAGE_ID} containing the message ID of XMS message.
     * <li> {@link #EXTRA_MIME_TYPE} containing the MIME-type of XMS message.
     * </ul>
     */
    public final static String ACTION_NEW_XMS_MESSAGE = "com.gsma.services.rcs.cms.action.NEW_XMS_MESSAGE";

    /**
     * MIME-type of received message
     */
    public final static String EXTRA_MIME_TYPE = "mimeType";

    /**
     * Message ID of received message
     */
    public final static String EXTRA_MESSAGE_ID = "messageId";

    private XmsMessageIntent() {
    }
}
