/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.cms.provider.xms;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;

import android.net.Uri;

/**
 * Xms data constants
 * 
 */
public class XmsData {
    /**
     * Database URIs
     */
    public static final Uri CONTENT_URI = Uri.parse("content://com.gsma.rcs.cms.xms/message");

    
    /**
     * Database filename
     */    
    public static final String DATABASE_NAME = "rcs_cms_xms.db";

    /**
     * Database table
     */    

    public static final String TABLE_XMS = "xms";
    
    /**
     * History log member id
     */
    public static final int HISTORYLOG_MEMBER_ID = 6;

    /**
     * Unique history ID
     */
    public static final String KEY_BASECOLUMN_ID = "_id";

    /**
     * Naitve provider id
     */
    public static final String KEY_NATIVE_PROVIDER_ID = "native_provider_id";

    /**
     * ContactId formatted number of remote contact or null if the message is an outgoing group chat
     * message.
     */
    public static final String KEY_CONTACT = "contact";

    /**
     * Subject
     */
    public static final String KEY_SUBJECT = "subject";
    
    /**
     * Content of the message
     */
    public static final String KEY_CONTENT = "content";

    /**
     * Content of the message
     */
    public static final String KEY_ATTACHMENT = "attachment";

    
    /**
     * Multipurpose Internet Mail Extensions (MIME) type of message
     */
    public static final String KEY_MIME_TYPE = "mime_type";

    /**
     * Status direction of message.
     * 
     * @see Direction
     */
    public static final String KEY_DIRECTION = "direction";

    /**
     * @see Read status
     */
    public static final String KEY_READ_STATUS = "read_status";
    
    /**
     * @see Push status
     */
    public static final String KEY_PUSH_STATUS = "push_status";
    
    
    /**
     * @see Delete status
     */
    public static final String KEY_DELETE_STATUS = "delete_status";

    /**
     * This is set on the receiver side when the message has been delivered.
     * 
     * @see com.gsma.services.rcs.RcsCommon.ReadStatus for the list of status.
     */
    public static final String KEY_DELIVERY_DATE = "delivery_date";

    /**
     * Time when message inserted
     */
    public static final String KEY_DATE = "date";

    /**
     * Message correlator
     */
    public static final String KEY_MESSAGE_CORRELATOR = "message_correlator";

}
