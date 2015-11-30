package com.gsma.rcs.cms.observer;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Part;

import java.util.Arrays;
import java.util.List;

public class XmsObserverUtils{

    public static class Sms {

        public static final Uri URI = Uri.parse("content://sms/");
        static List<Uri> FILTERED_URIS = Arrays.asList(new Uri[]{
                Uri.parse("content://sms/raw"),
                Uri.parse("content://sms/inbox"),
                Uri.parse("content://sms/conversations")
        });

        static final String[] PROJECTION = new String[]{
                BaseColumns._ID, Telephony.TextBasedSmsColumns.THREAD_ID, Telephony.TextBasedSmsColumns.ADDRESS, Telephony.TextBasedSmsColumns.DATE,
                Telephony.TextBasedSmsColumns.DATE_SENT, Telephony.TextBasedSmsColumns.PROTOCOL,
                Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.BODY
        };

        static final String[] PROJECTION_ID = new String[]{
                BaseColumns._ID
        };

        static final String WHERE_CONTACT_NOT_NULL = new StringBuilder(Telephony.TextBasedSmsColumns.ADDRESS).append(" is not null").toString();
    }

    public static class Mms {

        public static final Uri URI = Uri.parse("content://mms/");

        static final String[] PROJECTION_MMS_ID = new String[]{Telephony.BaseMmsColumns.MESSAGE_ID};
        public static final String[] PROJECTION = new String[]{
                Telephony.BaseMmsColumns._ID,
                Telephony.BaseMmsColumns.SUBJECT,
                Telephony.BaseMmsColumns.MESSAGE_ID,
                Telephony.BaseMmsColumns.MESSAGE_TYPE,
                Telephony.BaseMmsColumns.THREAD_ID,
                Telephony.BaseMmsColumns.DATE
        };

        private static final String WHERE_INBOX = new StringBuilder(Telephony.BaseMmsColumns.MESSAGE_BOX).append("=").append(Telephony.BaseMmsColumns.MESSAGE_BOX_INBOX).toString();
        private static final String WHERE_SENT = new StringBuilder(Telephony.BaseMmsColumns.MESSAGE_BOX).append("=").append(Telephony.BaseMmsColumns.MESSAGE_BOX_SENT).toString();
        private static final String WHERE_MSG_ID_NOT_NULL = new StringBuilder(Telephony.BaseMmsColumns.MESSAGE_ID).append(" is not null").toString();
        public static final String WHERE = new StringBuilder(WHERE_MSG_ID_NOT_NULL).append(" AND (").append(WHERE_INBOX).append(" OR ").append(WHERE_SENT).append(" )").toString();

        public static class Addr{
            public static final String URI = "content://mms/%1$s/addr/";
            public static final String[] PROJECTION = new String[]{
                    Telephony.Mms.Addr.ADDRESS,
            };
            public static final String WHERE = new StringBuilder(Telephony.Mms.Addr.TYPE).append("=?").toString();
            public static final int FROM = 137;
            public static final int TO = 151;
        }

        public static class Part{
            public static final String URI = "content://mms/part/";
            public static final String[] PROJECTION = new String[]{
                    Telephony.BaseMmsColumns._ID,
                    Telephony.Mms.Part.CONTENT_TYPE,
                    Telephony.Mms.Part.TEXT,
                    Telephony.Mms.Part.NAME,
                    Telephony.Mms.Part._DATA,
            };
            public static final String WHERE = new StringBuilder(Telephony.Mms.Part.MSG_ID).append("=?").toString();
        }
    }

    public static class Conversation {

        static final Uri URI = Uri.parse("content://mms-sms/conversations?simple=true");

        static final String[] PROJECTION = new String[]{
                BaseColumns._ID,
                Telephony.BaseMmsColumns.READ
        };
    }
}
