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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Content provider for XMS message
 * Created by Philippe LEMORDANT on 12/11/2015.
 */
public class XmsMessageLog {

    /**
     * Content provider URI for chat messages
     */
    public static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.services.rcs.provider.xms/message");
    /**
     * History log member id
     */
    public static final int HISTORYLOG_MEMBER_ID = 6;
    /**
     * The name of the column containing the unique id across provider tables.
     * <p/>
     * Type: INTEGER
     * </P>
     */
    public static final String BASECOLUMN_ID = BaseColumns._ID;
    /**
     * The name of the column containing the message ID.
     * <p/>
     * Type: TEXT
     * </P>
     */
    public static final String MESSAGE_ID = "msg_id";
    /**
     * The name of the column containing the unique ID of the chat conversation.
     * Used for the aggregation of messaging providers and always set to the contact
     * in case of XMS messages.
     * <p/>
     * Type: TEXT
     * </P>
     */
    public static final String CHAT_ID = "chat_id";
    /**
     * The name of the column containing the MSISDN of the remote contact.
     * <p/>
     * Type: TEXT
     * </P>
     */
    public static final String CONTACT = "contact";
    /**
     * The name of the column containing the XMS message content.
     * In case of SMS, content is set to the body text message.
     * In case of MMS, content is set to the subject.
     * <p/>
     * Type: TEXT
     * </P>
     */
    public static final String CONTENT = "content";
    /**
     * The name of the column containing the MIME-TYPE of the message.
     * <p/>
     * Type: TEXT
     * </P>
     */
    public static final String MIME_TYPE = "mime_type";
    /**
     * The name of the column containing the message direction.
     * <p/>
     * Type: INTEGER
     * </P>
     *
     * @see com.gsma.services.rcs.RcsService.Direction
     */
    public static final String DIRECTION = "direction";
    /**
     * The name of the column containing the time when message is created.
     * <p/>
     * Type: INTEGER
     * </P>
     */
    public static final String TIMESTAMP = "timestamp";
    /**
     * The name of the column containing the time when message is sent. If 0 means not sent.
     * <p/>
     * Type: INTEGER
     * </P>
     */
    public static final String TIMESTAMP_SENT = "timestamp_sent";
    /**
     * The name of the column containing the time when message is delivered. If 0 means not
     * delivered.
     * <p/>
     * Type: INTEGER
     * </P>
     */
    public static final String TIMESTAMP_DELIVERED = "timestamp_delivered";
    /**
     * The name of the column containing the message state.
     * <p/>
     * Type: INTEGER
     * </P>
     *
     * @see XmsMessage.State
     */
    public static final String STATE = "state";
    /**
     * The name of the column containing the message state reason code.
     * <p/>
     * Type: INTEGER
     * </P>
     *
     * @see XmsMessage.ReasonCode
     */
    public static final String REASON_CODE = "reason_code";
    /**
     * The name of the column containing the message read status.
     * <p/>
     * Type: INTEGER
     * </P>
     */
    public static final String READ_STATUS = "read_status";

    /**
     * Message MIME-types
     */
    public static class MimeType {

        /**
         * MIME-type of SMS messages
         */
        public static final String TEXT_MESSAGE = "text/plain";

        /**
         * MIME-type of MMS messages
         */
        public static final String MULTIMEDIA_MESSAGE = "application/mms";

        /**
         * MIME-type of SMS messages
         */
        public static final String APPLICATION_SMIL = "application/smil";

    }

}
