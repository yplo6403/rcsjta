package com.gsma.rcs.cms.provider.xms;

import android.content.Context;
import android.database.Cursor;

import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.provider.xms.model.XmsData;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.services.rcs.RcsService;

import java.util.ArrayList;
import java.util.List;

public class XmsLogEnvIntegration {

    private static final String SELECTION_READSTATUS_OR_DELETESTATUS = new StringBuilder(XmsLog.SELECTION_READ_STATUS).append(" OR ").append(XmsLog.SELECTION_DELETE_STATUS).toString();

    static final String[] PROJECTION_COUNT = new String[] {"count(*)"};

    protected final LocalContentResolver mLocalContentResolver;

    /**
     * Current instance
     */
    private static volatile XmsLogEnvIntegration sInstance;

    private XmsLogEnvIntegration(Context context) {
        mLocalContentResolver = new LocalContentResolver(context.getContentResolver());
    }

    public static XmsLogEnvIntegration getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (XmsLogEnvIntegration.class) {
            if (sInstance == null) {
                sInstance = new XmsLogEnvIntegration(context);
            }
        }
        return sInstance;
    }

    public List<SmsData> getMessages(XmsData.ReadStatus readStatus, XmsData.DeleteStatus deleteStatus) {

        List<SmsData> messages = new ArrayList<SmsData>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, XmsLog.PROJECTION_ID_CONTACT_READ_DELETE,
                    SELECTION_READSTATUS_OR_DELETESTATUS, new String[] {
                            String.valueOf(readStatus.toInt()),String.valueOf(deleteStatus.toInt())
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_BASECOLUMN_ID);
            int contactIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_CONTACT);
            int readIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_READ_STATUS);
            int deleteIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_DELETE_STATUS);
            while (cursor.moveToNext()) {
                SmsData smsData = new SmsData();
                smsData.setBaseId(cursor.getString(baseIdIdx));
                smsData.setContact(cursor.getString(contactIdIdx));
                smsData.setReadStatus(XmsData.ReadStatus.valueOf(cursor.getInt(readIdIdx)));
                smsData.setDeleteStatus(XmsData.DeleteStatus.valueOf(cursor.getInt(deleteIdIdx)));
                messages.add(smsData);
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }

    }

    /**
     * @param contact
     * @return SmsData
     */
    public List<SmsData> getMessages(String contact) {

        List<SmsData> messages = new ArrayList<SmsData>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, null,XmsLog.SELECTION_CONTACT,new String[]{contact},XmsLog.SORT_BY_DATE_ASC);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);

            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_BASECOLUMN_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_NATIVE_PROVIDER_ID);
            int nativeThreadIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_NATIVE_THREAD_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_CONTACT);
            int contentIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_CONTENT);
            int dateIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_DATE);
            int directionIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_DIRECTION);
            int readStatusIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_READ_STATUS);
            int deleteStatusIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_DELETE_STATUS);
            int deliveryDateIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_DELIVERY_DATE);
            int messageCorrelatorIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_MESSAGE_CORRELATOR);

            while (cursor.moveToNext()) {
                messages.add(new SmsData(
                        cursor.getString(baseIdIdx),
                        cursor.getLong(nativeProviderIdIdx),
                        cursor.getLong(nativeThreadIdIdx),
                        cursor.getString(contactIdx),
                        cursor.getString(contentIdx),
                        cursor.getLong(dateIdx),
                        cursor.getLong(deliveryDateIdx),
                        RcsService.Direction.valueOf(cursor.getInt(directionIdx)),
                        XmsData.ReadStatus.valueOf(cursor.getInt(readStatusIdx)),
                        XmsData.DeleteStatus.valueOf(cursor.getInt(deleteStatusIdx)),
                        cursor.getString(messageCorrelatorIdx)
                ));
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * @param contact
     * @return id
     */
    public int deleteConversationForContact(String contact) {
        return mLocalContentResolver.delete(XmsLogData.CONTENT_URI, XmsLog.SELECTION_CONTACT,
                new String[] {
                        contact
                });
    }

    /**
     * @return if the content provider is empty
     */
    public Boolean isEmpty(){

        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_COUNT, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0)==0;
            }
            return null;
        } finally {
            CursorUtil.close(cursor);
        }


    }
}
