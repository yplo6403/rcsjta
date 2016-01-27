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

package com.gsma.rcs.cms.observer;

import com.gsma.services.rcs.cms.XmsMessage.State;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.TextBasedSmsColumns;

import java.util.Arrays;
import java.util.List;

public class XmsObserverUtils {

    public static class Sms {

        public static final Uri URI = Uri.parse("content://sms/");
        static List<Uri> FILTERED_URIS = Arrays.asList(Uri.parse("content://sms/raw"),
                Uri.parse("content://sms/inbox"), Uri.parse("content://sms/conversations"));

        static final String[] PROJECTION = new String[] {
                BaseColumns._ID, Telephony.TextBasedSmsColumns.THREAD_ID,
                Telephony.TextBasedSmsColumns.ADDRESS, Telephony.TextBasedSmsColumns.DATE,
                Telephony.TextBasedSmsColumns.DATE_SENT, Telephony.TextBasedSmsColumns.PROTOCOL,
                Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.TYPE,
                Telephony.TextBasedSmsColumns.BODY
        };

        static final String[] PROJECTION_ID = new String[] {
            BaseColumns._ID
        };

        static final String WHERE_CONTACT_NOT_NULL = TextBasedSmsColumns.ADDRESS + " is not null";
    }

    public static class Mms {

        public static final Uri URI = Uri.parse("content://mms/");

        static final String[] PROJECTION_MMS_ID = new String[] {
            Telephony.BaseMmsColumns.MESSAGE_ID
        };
        public static final String[] PROJECTION = new String[] {
                Telephony.BaseMmsColumns._ID, Telephony.BaseMmsColumns.SUBJECT,
                Telephony.BaseMmsColumns.MESSAGE_ID, Telephony.BaseMmsColumns.MESSAGE_TYPE,
                Telephony.BaseMmsColumns.THREAD_ID, Telephony.BaseMmsColumns.DATE,
                Telephony.BaseMmsColumns.TRANSACTION_ID
        };

        private static final String WHERE_INBOX = Telephony.BaseMmsColumns.MESSAGE_BOX + "="
                + Telephony.BaseMmsColumns.MESSAGE_BOX_INBOX;
        private static final String WHERE_SENT = Telephony.BaseMmsColumns.MESSAGE_BOX + "="
                + Telephony.BaseMmsColumns.MESSAGE_BOX_SENT;
        private static final String WHERE_MSG_ID_NOT_NULL = Telephony.BaseMmsColumns.MESSAGE_ID
                + " is not null";
        public static final String WHERE_INBOX_OR_SENT = WHERE_MSG_ID_NOT_NULL + " AND ("
                + WHERE_INBOX + " OR " + WHERE_SENT + " )";

        public static class Addr {
            public static final String URI = "content://mms/%1$s/addr/";
            public static final String[] PROJECTION = new String[] {
                Telephony.Mms.Addr.ADDRESS,
            };
            public static final String WHERE = Telephony.Mms.Addr.TYPE + "=?";
            public static final int FROM = 137;
            public static final int TO = 151;
        }

        public static class Part {
            public static final String URI = "content://mms/part/";
            public static final String[] PROJECTION = new String[] {
                    Telephony.BaseMmsColumns._ID, Telephony.Mms.Part.CONTENT_TYPE,
                    Telephony.Mms.Part.TEXT, Telephony.Mms.Part.CONTENT_LOCATION,
                    Telephony.Mms.Part._DATA,
            };
            public static final String WHERE = Telephony.Mms.Part.MSG_ID + "=?";
        }
    }

    public static class Conversation {

        static final Uri URI = Uri.parse("content://mms-sms/conversations?simple=true");

        static final String[] PROJECTION = new String[] {
                BaseColumns._ID, Telephony.BaseMmsColumns.READ
        };
    }

    public static State getSmsState(int type, int status) {
        if (type == TextBasedSmsColumns.MESSAGE_TYPE_FAILED) {
            return State.FAILED;
        }

        if (type == TextBasedSmsColumns.MESSAGE_TYPE_SENT) {
            if (status == TextBasedSmsColumns.STATUS_COMPLETE) {
                return State.DELIVERED;
            } else {
                return State.SENT;
            }
        }
        return null;
    }
}
