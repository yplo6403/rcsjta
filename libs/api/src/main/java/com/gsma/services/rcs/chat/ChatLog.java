/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Content provider for chat history
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class ChatLog {
    /**
     * Group chat
     */
    public static class GroupChat {
        /**
         * Content provider URI for chat conversations
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://com.gsma.services.rcs.provider.chat/groupchat");

        /**
         * History log member id
         */
        public static final int HISTORYLOG_MEMBER_ID = 0;

        /**
         * The name of the column containing the unique id across provider tables.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String BASECOLUMN_ID = BaseColumns._ID;

        /**
         * The name of the column containing the unique ID of the group chat or the remote contact
         * in case of one to one chat.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CHAT_ID = "chat_id";

        /**
         * The name of the column containing the state of the group chat.
         * <P>
         * Type: INTEGER
         * </P>
         * 
         * @see State
         */
        public static final String STATE = "state";

        /**
         * The name of the column containing the reason code of the state of the group chat.
         * <P>
         * Type: INTEGER
         * </P>
         * 
         * @see ReasonCode
         */
        public static final String REASON_CODE = "reason_code";

        /**
         * The name of the column containing the subject of the group chat.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String SUBJECT = "subject";

        /**
         * The name of the column containing the direction of the group chat.
         * <P>
         * Type: INTEGER
         * </P>
         * 
         * @see Direction
         */
        public static final String DIRECTION = "direction";

        /**
         * The name of the column containing the time when group chat is created.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String TIMESTAMP = "timestamp";

        /**
         * The name of the column containing the list of participants and associated status.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String PARTICIPANTS = "participants";

        /**
         * ContactId formatted number of the inviter of the group chat or null if this is a group
         * chat initiated by the local user (ie outgoing group chat).
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONTACT = "contact";

        /**
         * Utility method to get participants from its string representation in the ChatLog
         * provider.
         * 
         * @param ctx the context
         * @param participants Participants in string representation
         * @return Participants
         * @throws RcsPermissionDeniedException
         */
        public static Map<ContactId, ParticipantStatus> getParticipants(Context ctx,
                String participants) throws RcsPermissionDeniedException {
            ContactUtil contactUtils = ContactUtil.getInstance(ctx);
            String[] tokens = participants.split(",");
            Map<ContactId, ParticipantStatus> participantResult = new HashMap<>();
            for (String participant : tokens) {
                String[] keyValue = participant.split("=");
                if (keyValue.length == 2) {
                    String contact = keyValue[0];
                    ParticipantStatus status = ParticipantStatus.valueOf(Integer
                            .parseInt(keyValue[1]));
                    participantResult.put(contactUtils.formatContact(contact), status);
                }
            }
            return participantResult;
        }

        private GroupChat() {
        }
    }

    /**
     * Chat message from a single chat or group chat
     */
    public static class Message {
        /**
         * Content provider URI for chat messages
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://com.gsma.services.rcs.provider.chat/chatmessage");

        /**
         * History log member id
         */
        public static final int HISTORYLOG_MEMBER_ID = 1;

        /**
         * The name of the column containing the unique id across provider tables.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String BASECOLUMN_ID = BaseColumns._ID;

        /**
         * The name of the column containing the chat ID.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CHAT_ID = "chat_id";

        /**
         * The name of the column containing the message ID.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String MESSAGE_ID = "msg_id";

        /**
         * The name of the column containing the message status.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String STATUS = "status";

        /**
         * The name of the column containing the message status reason code.
         * <P>
         * Type: INTEGER
         * </P>
         * 
         * @see ReasonCode
         */
        public static final String REASON_CODE = "reason_code";

        /**
         * The name of the column containing the message read status.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String READ_STATUS = "read_status";

        /**
         * The name of the column containing the message direction.
         * <P>
         * Type: INTEGER
         * </P>
         * 
         * @see Direction
         */
        public static final String DIRECTION = "direction";

        /**
         * The name of the column containing the MSISDN of the remote contact.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONTACT = "contact";

        /**
         * The name of the column containing the message content.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONTENT = "content";

        /**
         * The name of the column containing the time when message is created.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String TIMESTAMP = "timestamp";

        /**
         * The name of the column containing the time when message is sent. If 0 means not sent.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String TIMESTAMP_SENT = "timestamp_sent";

        /**
         * The name of the column containing the time when message is delivered. If 0 means not
         * delivered.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String TIMESTAMP_DELIVERED = "timestamp_delivered";

        /**
         * The name of the column containing the time when message is displayed. If 0 means not
         * displayed.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String TIMESTAMP_DISPLAYED = "timestamp_displayed";

        /**
         * If delivery has expired for this message. Values: 1 (true), 0 (false)
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String EXPIRED_DELIVERY = "expired_delivery";

        /**
         * The name of the column containing the MIME-TYPE of the message body.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String MIME_TYPE = "mime_type";

        /**
         * Message MIME-types
         */
        public static class MimeType {

            /**
             * MIME-type of text messages
             */
            public static final String TEXT_MESSAGE = "text/plain";

            /**
             * MIME-type of geoloc messages
             */
            public static final String GEOLOC_MESSAGE = "application/geoloc";

            /**
             * MIME-type of group chat events
             */
            public static final String GROUPCHAT_EVENT = "rcs/groupchat-event";

            private MimeType() {
            }
        }

        public static class Content {
            /**
             * Status of the message
             */
            public enum Status {

                /**
                 * The message has been rejected
                 */
                REJECTED(0),

                /**
                 * The message is queued to be sent by rcs service when possible
                 */
                QUEUED(1),

                /**
                 * The message is in progress of sending
                 */
                SENDING(2),

                /**
                 * The message has been sent
                 */
                SENT(3),

                /**
                 * The message sending has been failed
                 */
                FAILED(4),

                /**
                 * The message has been delivered to the remote.
                 */
                DELIVERED(5),

                /**
                 * The message has been received and a displayed delivery report is requested
                 */
                DISPLAY_REPORT_REQUESTED(6),

                /**
                 * The message is delivered and no display delivery report is requested.
                 */
                RECEIVED(7),

                /**
                 * The message has been displayed
                 */
                DISPLAYED(8);

                private final int mValue;

                private static SparseArray<Status> mValueToEnum = new SparseArray<>();
                static {
                    for (Status entry : Status.values()) {
                        mValueToEnum.put(entry.toInt(), entry);
                    }
                }

                Status(int value) {
                    mValue = value;
                }

                public final int toInt() {
                    return mValue;
                }

                public static Status valueOf(int value) {
                    Status entry = mValueToEnum.get(value);
                    if (entry != null) {
                        return entry;
                    }
                    throw new IllegalArgumentException("No enum const class "
                            + Status.class.getName() + "" + value + "!");
                }
            }

            /**
             * Reason code of the message status
             */
            public enum ReasonCode {

                /**
                 * No specific reason code specified.
                 */
                UNSPECIFIED(0),

                /**
                 * Sending of the message failed.
                 */
                FAILED_SEND(1),

                /**
                 * Delivering of the message failed.
                 */
                FAILED_DELIVERY(2),

                /**
                 * Displaying of the message failed.
                 */
                FAILED_DISPLAY(3),

                /**
                 * Incoming one-to-one message was detected as spam.
                 */
                REJECTED_SPAM(4);

                private final int mValue;

                private static SparseArray<ReasonCode> mValueToEnum = new SparseArray<>();
                static {
                    for (ReasonCode entry : ReasonCode.values()) {
                        mValueToEnum.put(entry.toInt(), entry);
                    }
                }

                ReasonCode(int value) {
                    mValue = value;
                }

                public final int toInt() {
                    return mValue;
                }

                public static ReasonCode valueOf(int value) {
                    ReasonCode entry = mValueToEnum.get(value);
                    if (entry != null) {
                        return entry;
                    }
                    throw new IllegalArgumentException("No enum const class "
                            + ReasonCode.class.getName() + "" + value + "!");
                }
            }

            private Content() {
            }
        }

        public static class GroupChatEvent {
            /**
             * Status of group chat event message
             */
            public enum Status {

                /**
                 * JOINED.
                 */
                JOINED(0),

                /**
                 * DEPARTED.
                 */
                DEPARTED(1);

                private final int mValue;

                private static SparseArray<Status> mValueToEnum = new SparseArray<>();
                static {
                    for (Status entry : Status.values()) {
                        mValueToEnum.put(entry.toInt(), entry);
                    }
                }

                Status(int value) {
                    mValue = value;
                }

                public final int toInt() {
                    return mValue;
                }

                public static Status valueOf(int value) {
                    Status entry = mValueToEnum.get(value);
                    if (entry != null) {
                        return entry;
                    }
                    throw new IllegalArgumentException("No enum const class "
                            + Status.class.getName() + "" + value + "!");
                }
            }

            private GroupChatEvent() {
            }
        }

        private Message() {
        }
    }

    private ChatLog() {
    }
}
