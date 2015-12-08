package com.gsma.rcs.cms.provider.xms;

import android.content.Context;
import android.database.Cursor;

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.contact.ContactId;

import java.util.ArrayList;
import java.util.List;

public class XmsLogEnvIntegration {

    static final String[] PROJECTION_COUNT = new String[] {"count(*)"};

    private static final String SORT_BY_DATE_DESC = new StringBuilder(XmsData.KEY_TIMESTAMP).append(" DESC").toString();

    private static final String SELECTION_XMS_CONTACT = XmsData.KEY_CONTACT + "=?";

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

    /**
     * @param contact
     * @return SmsData
     */
    public List<SmsDataObject> getMessages(ContactId contact) {

        List<SmsDataObject> messages = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null, SELECTION_XMS_CONTACT,new String[]{contact.toString()},SORT_BY_DATE_DESC);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);

            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_ID);
            int nativeThreadIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_THREAD_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
            int contentIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTENT);
            int dateIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_TIMESTAMP);
            int directionIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DIRECTION);
            int readStatusIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_READ_STATUS);

            while (cursor.moveToNext()) {
                messages.add(new SmsDataObject(
                        IdGenerator.generateMessageID(),
                        ContactUtil.createContactIdFromTrustedData(cursor.getString(contactIdx)),
                        cursor.getString(contentIdx),
                        RcsService.Direction.valueOf(cursor.getInt(directionIdx)),
                        ReadStatus.valueOf(cursor.getInt(readStatusIdx)),
                        cursor.getLong(dateIdx),
                        cursor.isNull(nativeProviderIdIdx) ? null : cursor.getLong(nativeProviderIdIdx),
                        cursor.isNull(nativeThreadIdIdx) ? null :cursor.getLong(nativeThreadIdIdx)
                ));
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * @return if the content provider is empty
     */
    public Boolean isEmpty(){

        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJECTION_COUNT, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0)==0;
            }
            return null;
        } finally {
            CursorUtil.close(cursor);
        }


    }
}
