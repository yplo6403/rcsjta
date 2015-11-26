/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.gsma.rcs.provider.xms;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.cms.XmsMessageLog;

import android.net.Uri;

/**
 * Xms data constants
 */
public class XmsData {
    /**
     * Database URIs
     */
    public static final Uri CONTENT_URI = Uri.parse("content://com.gsma.rcs.xms/message");

    /**
     * History log member id
     */
    public static final int HISTORYLOG_MEMBER_ID = XmsMessageLog.HISTORYLOG_MEMBER_ID;

    /**
     * Unique history ID (even across several history log members)
     */
    public static final String KEY_BASECOLUMN_ID = XmsMessageLog.BASECOLUMN_ID;

    /**
     * Unique XMS identifier.
     */
    public static final String KEY_MESSAGE_ID = XmsMessageLog.MESSAGE_ID;

    /**
     * ContactId formatted number of remote contact. message.
     */
    public static final String KEY_CONTACT = XmsMessageLog.CONTACT;

    /**
     * Body text of the message (SMS or MMS)
     */
    public static final String KEY_BODY = XmsMessageLog.BODY;

    /**
     * Multipurpose Internet Mail Extensions (MIME) type of message
     */
    public static final String KEY_MIME_TYPE = XmsMessageLog.MIME_TYPE;

    /**
     * Status direction of message.
     * 
     * @see Direction
     */
    public static final String KEY_DIRECTION = XmsMessageLog.DIRECTION;

    /**
     * Time when message inserted.
     */
    public static final String KEY_TIMESTAMP = XmsMessageLog.TIMESTAMP;

    /**
     * Time when message is sent. If 0 means not sent.
     */
    public static final String KEY_TIMESTAMP_SENT = XmsMessageLog.TIMESTAMP_SENT;

    /**
     * Time when message is delivered. If 0 means not delivered.
     */
    public static final String KEY_TIMESTAMP_DELIVERED = XmsMessageLog.TIMESTAMP_DELIVERED;

    /**
     * See enum XmsMessage.State for possible state codes.
     */
    public static final String KEY_STATE = XmsMessageLog.STATE;
    /**
     * See enum XmsMessage.ReasonCode for possible reason codes.
     */
    public static final String KEY_REASON_CODE = XmsMessageLog.REASON_CODE;
    /**
     * This is set on the receiver side when the message has been marked as read. See enum
     * ReadStatus for the list of statuses.
     */
    public static final String KEY_READ_STATUS = XmsMessageLog.READ_STATUS;

    /**
     * Native provider id (MMS or SMS)
     */
    public static final String KEY_NATIVE_ID = "native_id";

    /**
     * Message correlator
     */
    public static final String KEY_MESSAGE_CORRELATOR = "correlator";

    /**
     * Multimedia Message ID.
     */
    public static final String KEY_MMS_ID = "mms_id";

    /**
     * Chat Id (column contains the contact Id).
     */
    public static final String KEY_CHAT_ID = XmsMessageLog.CHAT_ID;

}
