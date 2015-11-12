
package com.gsma.rcs.cms.provider.xms;

import com.gsma.rcs.cms.provider.xms.model.XmsData;
import com.gsma.rcs.cms.provider.xms.model.XmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.XmsData.MimeType;
import com.gsma.rcs.cms.provider.xms.model.XmsData.PushStatus;
import com.gsma.rcs.cms.provider.xms.model.XmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.MmsData;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.history.HistoryLogData;
import com.gsma.rcs.provider.history.HistoryProvider;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.history.HistoryLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class XmsLog {

    /**
     * Current instance
     */
    private static volatile XmsLog sInstance;

    protected final LocalContentResolver mLocalContentResolver;

    private static final String[] PROJECTION_BASE_ID = new String[] {
            XmsLogData.KEY_BASECOLUMN_ID
    };

    private static final String[] PROJECTION_MMS_ID = new String[] {
            XmsLogData.KEY_MMS_ID
    };

    private static final String[] PROJECTION_NATIVE_PROVIDER_ID = new String[] {
            XmsLogData.KEY_NATIVE_PROVIDER_ID
    };
    
    private static final String[] PROJECTION_BASE_ID_NATIVE_PROVIDER_ID = new String[] {
            XmsLogData.KEY_BASECOLUMN_ID,
            XmsLogData.KEY_NATIVE_PROVIDER_ID
    };

    private static final String[] PROJECTION_BASE_ID_NATIVE_PROVIDER_ID_CONTACT = new String[] {
            XmsLogData.KEY_BASECOLUMN_ID,
            XmsLogData.KEY_NATIVE_PROVIDER_ID,
            XmsLogData.KEY_CONTACT
    };


    private static final String[] PROJECTION_BASE_ID_CONTACT = new String[] {
            XmsLogData.KEY_BASECOLUMN_ID,
            XmsLogData.KEY_CONTACT
    };
    
    static final String[] PROJECTION_ID_CONTACT_READ_DELETE = new String[] {
            XmsLogData.KEY_BASECOLUMN_ID,
            XmsLogData.KEY_CONTACT,
            XmsLogData.KEY_READ_STATUS,
            XmsLogData.KEY_DELETE_STATUS
    };

    private static final String SELECTION_SMS = new StringBuilder(XmsLogData.KEY_MIME_TYPE).append("=").append(MimeType.SMS.toInt()).toString();
    private static final String SELECTION_MMS = new StringBuilder(XmsLogData.KEY_MIME_TYPE).append("=").append(String.valueOf(MimeType.MMS.toInt())).toString();

    static final String SELECTION_CONTACT = new StringBuilder(XmsLogData.KEY_CONTACT).append("=?").toString();
    static final String SELECTION_READ_STATUS = new StringBuilder(XmsLogData.KEY_READ_STATUS).append("=?").toString();
    static final String SELECTION_DELETE_STATUS = new StringBuilder(XmsLogData.KEY_DELETE_STATUS).append("=?").toString();
    private static final String SELECTION_PUSH_STATUS = new StringBuilder(XmsLogData.KEY_PUSH_STATUS).append("=?").toString();
    private static final String SELECTION_UNREAD = new StringBuilder(XmsLogData.KEY_READ_STATUS).append("=").append(ReadStatus.UNREAD.toInt()).toString();
    private static final String SELECTION_MIME_TYPE = new StringBuilder(XmsLogData.KEY_MIME_TYPE).append("=?").toString();
    private static final String SELECTION_NATIVE_PROVIDER_ID = new StringBuilder(XmsLogData.KEY_NATIVE_PROVIDER_ID).append("=?").toString();
    private static final String SELECTION_BASE_ID = new StringBuilder(XmsLogData.KEY_BASECOLUMN_ID).append("=?").toString();
    private static final String SELECTION_DIRECTION = new StringBuilder(XmsLogData.KEY_DIRECTION).append("=?").toString();
    private static final String SELECTION_CORRELATOR = new StringBuilder(XmsLogData.KEY_MESSAGE_CORRELATOR).append("=?").toString();
    private static final String SELECTION_NATIVE_THREAD_ID = new StringBuilder(XmsLogData.KEY_NATIVE_THREAD_ID).append("=?").toString();
    private static final String SELECTION_MMS_ID = new StringBuilder(XmsLogData.KEY_MMS_ID).append("=?").toString();

    private static final String SELECTION_NATIVE_THREAD_ID_UNREAD = new StringBuilder(SELECTION_NATIVE_THREAD_ID).append(" AND ").append(SELECTION_UNREAD).toString();
    private static final String SELECTION_NATIVE_THREAD_ID_READSTATUS = new StringBuilder(SELECTION_NATIVE_THREAD_ID).append(" AND ").append(SELECTION_READ_STATUS).toString();
    private static final String SELECTION_NATIVE_THREAD_ID_DELETESTATUS = new StringBuilder(SELECTION_NATIVE_THREAD_ID).append(" AND ").append(SELECTION_DELETE_STATUS).toString();
    private static final String SELECTION_NATIVE_THREAD_ID_MMS = new StringBuilder(SELECTION_NATIVE_THREAD_ID).append(" AND ").append(SELECTION_MMS).toString();

    private static final String SELECTION_CONTACT_UNREAD = new StringBuilder(SELECTION_CONTACT).append(" AND ").append(SELECTION_UNREAD).toString();
    private static final String SELECTION_CONTACT_READSTATUS = new StringBuilder(SELECTION_CONTACT).append(" AND ").append(SELECTION_READ_STATUS).toString();
    private static final String SELECTION_CONTACT_DELETESTATUS = new StringBuilder(SELECTION_CONTACT).append(" AND ").append(SELECTION_DELETE_STATUS).toString();
    private static final String SELECTION_CONTACT_READSTATUS_OR_DELETESTATUS = new StringBuilder(SELECTION_CONTACT).append(" AND (").append(SELECTION_READ_STATUS).append(" OR ").append(SELECTION_DELETE_STATUS).append(")").toString();
    private static final String SELECTION_CONTACT_DIRECTION_CORRELATOR = new StringBuilder(SELECTION_CONTACT).append(" AND ").append(SELECTION_DIRECTION).append(" AND ").append(SELECTION_CORRELATOR).append(" AND ").append(SELECTION_SMS).toString();
    private static final String SELECTION_CONTACT_MMS = new StringBuilder(SELECTION_CONTACT).append(" AND ").append(SELECTION_MMS).toString();

    private static final String SELECTION_NATIVE_PROVIDER_ID_MIMETYPE = new StringBuilder(SELECTION_NATIVE_PROVIDER_ID).append(" AND ").append(SELECTION_MIME_TYPE).toString();
    private static final String SELECTION_MMS_ID_MIMETYPE = new StringBuilder(SELECTION_MMS_ID).append(" AND ").append(SELECTION_MIME_TYPE).toString();

    private static final String SORT_BY_DATE_DESC = new StringBuilder(
            XmsLogData.KEY_DATE).append(" DESC").toString();
    static final String SORT_BY_DATE_ASC = new StringBuilder(
            XmsLogData.KEY_DATE).append(" ASC").toString();
    
    
    private static final Logger sLogger = Logger.getLogger(XmsLog.class.getSimpleName());

    /**
     * Gets the instance of XmsLog singleton
     * @param context
     * @return the instance of XmsLog singleton
     */
    public static XmsLog getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (XmsLog.class) {
            if (sInstance == null) {
                sInstance = new XmsLog(context);
            }
        }
        return sInstance;
    }

    /**
     * Constructor
     * 
     * @param context
     */
    private XmsLog(Context context) {
        mLocalContentResolver = new LocalContentResolver(context.getContentResolver());

        HistoryProvider historyProvider = (HistoryProvider) context.getContentResolver()
                .acquireContentProviderClient(HistoryLogData.CONTENT_URI.getAuthority()).getLocalContentProvider();
        Map<String,String> columnMapping = new HashMap<String,String>(); 
        columnMapping.put(HistoryLog.PROVIDER_ID,
                String.valueOf(XmsLogData.HISTORYLOG_MEMBER_ID));
        columnMapping.put(HistoryLog.BASECOLUMN_ID, XmsLogData.KEY_BASECOLUMN_ID);
        columnMapping.put(HistoryLog.MIME_TYPE, XmsLogData.KEY_MIME_TYPE);
        columnMapping.put(HistoryLog.DIRECTION, XmsLogData.KEY_DIRECTION);
        columnMapping.put(HistoryLog.CONTACT, XmsLogData.KEY_CONTACT);
        columnMapping.put(HistoryLog.TIMESTAMP, XmsLogData.KEY_DATE);
        columnMapping.put(HistoryLog.TIMESTAMP_SENT, XmsLogData.KEY_DATE);
        columnMapping.put(HistoryLog.TIMESTAMP_DELIVERED, XmsLogData.KEY_DELIVERY_DATE);
        columnMapping.put(HistoryLog.CHAT_ID, XmsLogData.KEY_CONTACT);
        columnMapping.put(HistoryLog.CONTENT, XmsLogData.KEY_CONTENT);

        //TODO FGI
        // traiter l'exception
        try {
            historyProvider.registerDatabase(XmsLogData.HISTORYLOG_MEMBER_ID, XmsLogData.CONTENT_URI, Uri.fromFile(context.getDatabasePath(XmsLogData.DATABASE_NAME)), XmsLogData.TABLE_XMS, columnMapping);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add Sms
     * @param msg 
     * @return baseColumnId
     */
    public String addSms(SmsData msg) {

        ContentValues values = new ContentValues();
        values.put(XmsLogData.KEY_NATIVE_THREAD_ID, msg.getNativeThreadId());
        values.put(XmsLogData.KEY_CONTACT, msg.getContact());
        values.put(XmsLogData.KEY_CONTENT, msg.getContent());
        values.put(XmsLogData.KEY_DATE, msg.getDate());
        values.put(XmsLogData.KEY_DIRECTION, msg.getDirection().toInt());
        values.put(XmsLogData.KEY_MIME_TYPE, msg.getMimeType().toInt());
        values.put(XmsLogData.KEY_READ_STATUS, msg.getReadStatus().toInt());
        values.put(XmsLogData.KEY_DELETE_STATUS, msg.getDeleteStatus().toInt());
        values.put(XmsLogData.KEY_DELIVERY_DATE, msg.getDeliveryDate());
        values.put(XmsLogData.KEY_NATIVE_PROVIDER_ID, msg.getNativeProviderId());
        values.put(XmsLogData.KEY_PUSH_STATUS, msg.getPushStatus().toInt());
        values.put(XmsLogData.KEY_MESSAGE_CORRELATOR, msg.getMessageCorrelator());

        Uri uri = mLocalContentResolver.insert(XmsLogData.CONTENT_URI, values);
        return uri.getLastPathSegment();
    }

    /**
     * Add Mms
     * @param msg
     * @return baseColumnId
     */
    public String addMms(MmsData msg) {

        ContentValues values = new ContentValues();

        String mmsId = msg.getMmsId();
        values.put(XmsLogData.KEY_NATIVE_THREAD_ID, msg.getNativeThreadId());
        values.put(XmsLogData.KEY_CONTACT, msg.getContact());
        values.put(XmsLogData.KEY_CONTENT, msg.getContent());
        values.put(XmsLogData.KEY_DATE, msg.getDate());
        values.put(XmsLogData.KEY_DIRECTION, msg.getDirection().toInt());
        values.put(XmsLogData.KEY_MIME_TYPE, msg.getMimeType().toInt());
        values.put(XmsLogData.KEY_READ_STATUS, msg.getReadStatus().toInt());
        values.put(XmsLogData.KEY_DELETE_STATUS, msg.getDeleteStatus().toInt());
        values.put(XmsLogData.KEY_DELIVERY_DATE, msg.getDeliveryDate());
        values.put(XmsLogData.KEY_NATIVE_PROVIDER_ID, msg.getNativeProviderId());
        values.put(XmsLogData.KEY_PUSH_STATUS, msg.getPushStatus().toInt());
        values.put(XmsLogData.KEY_SUBJECT, msg.getSubject());
        values.put(XmsLogData.KEY_MMS_ID,mmsId);
        //values.put(XmsLogData.KEY_ATTACHMENT, msg.getAttachment().toString());

        Uri uri = mLocalContentResolver.insert(XmsLogData.CONTENT_URI, values);
        return uri.getLastPathSegment();
    }

    /**
     * @param pushStatus 
     * @return SmsData
     */
    public List<XmsData> getMessages(PushStatus pushStatus) {
        
        List<XmsData> messages = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, null,SELECTION_PUSH_STATUS,new String[]{String.valueOf(pushStatus.toInt())},SORT_BY_DATE_ASC);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);

            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_BASECOLUMN_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_NATIVE_PROVIDER_ID);
            int nativeThreadIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_NATIVE_THREAD_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_CONTACT);
            int contentIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_CONTENT);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_MIME_TYPE);
            int dateIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_DATE);
            int directionIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_DIRECTION);
            int readStatusIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_READ_STATUS);
            int deleteStatusIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_DELETE_STATUS);
            int deliveryDateIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_DELIVERY_DATE);
            int messageCorrelatorIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_MESSAGE_CORRELATOR);
            int mmsIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_MMS_ID);
            int subjectIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_SUBJECT);
            while (cursor.moveToNext()) {

                MimeType mimeType = MimeType.valueOf(cursor.getInt(mimeTypeIdx));
                switch(mimeType){
                    case SMS:
                        messages.add(new SmsData(
                                cursor.getString(baseIdIdx),
                                cursor.getLong(nativeProviderIdIdx),
                                cursor.getLong(nativeThreadIdIdx),
                                cursor.getString(contactIdx),
                                cursor.getString(contentIdx),
                                cursor.getLong(dateIdx),
                                cursor.getLong(deliveryDateIdx),
                                Direction.valueOf(cursor.getInt(directionIdx)),
                                ReadStatus.valueOf(cursor.getInt(readStatusIdx)),
                                DeleteStatus.valueOf(cursor.getInt(deleteStatusIdx)),
                                cursor.getString(messageCorrelatorIdx)
                        ));
                        break;
                    case MMS:
                        messages.add(new MmsData(
                                cursor.getString(baseIdIdx),
                                cursor.getLong(nativeProviderIdIdx),
                                cursor.getLong(nativeThreadIdIdx),
                                cursor.getString(mmsIdIdx),
                                cursor.getString(contactIdx),
                                cursor.getString(subjectIdx),
                                cursor.getString(contentIdx),
                                cursor.getLong(dateIdx),
                                Direction.valueOf(cursor.getInt(directionIdx)),
                                ReadStatus.valueOf(cursor.getInt(readStatusIdx))
                                ));
                        break;
                }
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }
    /**
     * @param nativeProviderId
     * @return contact
     */
    public XmsData getMessageByNativeProviderId(MimeType mimeType, Long nativeProviderId) {
        Cursor cursor = null;

        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_BASE_ID_CONTACT, SELECTION_NATIVE_PROVIDER_ID_MIMETYPE,
                    new String[] {String.valueOf(nativeProviderId), String.valueOf(mimeType.toInt())}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            if (cursor.moveToFirst()) {
                int contactIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_CONTACT);
                int idIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_BASECOLUMN_ID);
                XmsData xmsData = new XmsData();
                xmsData.setBaseId(cursor.getString(idIdx));
                xmsData.setContact(cursor.getString(contactIdx));
                return xmsData;
            }
            return null;
        } finally {
            CursorUtil.close(cursor);
        }            
    }
    
    /**
     * @param nativeProviderId 
     * @param date 
     */
    public void setMessageAsDeliveredWithNativeProviderId(MimeType mimeType, String nativeProviderId, long date) {
        ContentValues values = new ContentValues();
        values.put(XmsLogData.KEY_DELIVERY_DATE, date);
        mLocalContentResolver.update(XmsLogData.CONTENT_URI, values, SELECTION_NATIVE_PROVIDER_ID_MIMETYPE, new String[]{nativeProviderId, String.valueOf(mimeType.toInt())});
    }
    
    /**
     * @param nativeThreadId
     * @param readStatus
     */
    public void updateReadStatusWithNativeThreadId(Long nativeThreadId, ReadStatus readStatus){
        ContentValues values = new ContentValues();
        values.put(XmsLogData.KEY_READ_STATUS, readStatus.toInt());
        mLocalContentResolver.update(XmsLogData.CONTENT_URI, values, SELECTION_NATIVE_THREAD_ID_UNREAD, new String[]{String.valueOf(nativeThreadId)});
    } 
        
    /**
     * @param baseId
     * @param readStatus
     */
    public void updateReadStatusdWithBaseId(String baseId, ReadStatus readStatus){
        ContentValues values = new ContentValues();
        values.put(XmsLogData.KEY_READ_STATUS, readStatus.toInt());
        mLocalContentResolver.update(XmsLogData.CONTENT_URI, values, SELECTION_BASE_ID, new String[]{baseId});
    }
    
    /**
     * @param contact
     * @param readStatus
     * @return number of updated rows
     */
    public int updateReadStatus(String contact, ReadStatus readStatus){
        ContentValues values = new ContentValues();
        values.put(XmsLogData.KEY_READ_STATUS, readStatus.toInt());
       return mLocalContentResolver.update(XmsLogData.CONTENT_URI, values, SELECTION_CONTACT_UNREAD, new String[]{contact});
    } 
    
    /**
     * @param nativeProviderId
     * @param deleteStatus
     */
    public void updateSmsDeleteStatus(Long nativeProviderId, DeleteStatus deleteStatus){
        ContentValues values = new ContentValues();
        values.put(XmsLogData.KEY_DELETE_STATUS, deleteStatus.toInt());
        mLocalContentResolver.update(XmsLogData.CONTENT_URI, values, SELECTION_NATIVE_PROVIDER_ID_MIMETYPE, new String[]{String.valueOf(nativeProviderId), String.valueOf(MimeType.SMS.toInt())});
    }

    /**
     * @param mmsId
     * @param deleteStatus
     */
    public void updateMmsDeleteStatus(String mmsId, DeleteStatus deleteStatus){
        ContentValues values = new ContentValues();
        values.put(XmsLogData.KEY_DELETE_STATUS, deleteStatus.toInt());
        mLocalContentResolver.update(XmsLogData.CONTENT_URI, values, SELECTION_MMS_ID_MIMETYPE, new String[]{mmsId, String.valueOf(MimeType.MMS.toInt())});
    }

    /**
     * @param baseId
     * @param deleteStatus
     */
    public void updateDeleteStatusdWithBaseId(String baseId, DeleteStatus deleteStatus){
        ContentValues values = new ContentValues();
        values.put(XmsLogData.KEY_DELETE_STATUS, deleteStatus.toInt());
        mLocalContentResolver.update(XmsLogData.CONTENT_URI, values, SELECTION_BASE_ID, new String[]{baseId});
    }
    
    /**
     * @param contact
     * @param deleteStatus
     * @return number of updated rows
     */
    public int updateDeleteStatus(String contact,  DeleteStatus deleteStatus){
        ContentValues values = new ContentValues();
        values.put(XmsLogData.KEY_DELETE_STATUS, deleteStatus.toInt());
       return mLocalContentResolver.update(XmsLogData.CONTENT_URI, values, SELECTION_CONTACT, new String[]{contact});
    }

    /**
     * @param nativeThreadId
     * @param deleteStatus
     * @return number of updated rows
     */
    public int updateDeleteStatus(Long nativeThreadId,  DeleteStatus deleteStatus){
        ContentValues values = new ContentValues();
        values.put(XmsLogData.KEY_DELETE_STATUS, deleteStatus.toInt());
        return mLocalContentResolver.update(XmsLogData.CONTENT_URI, values, SELECTION_NATIVE_THREAD_ID, new String[]{String.valueOf(nativeThreadId)});
    }

    /**
     * @param baseId 
     * @param pushStatus 
     * @return number of updated rows
     */
    public int updatePushStatus(String baseId, PushStatus pushStatus){
        ContentValues values = new ContentValues();
        values.put(XmsLogData.KEY_PUSH_STATUS, pushStatus.toInt());
       return mLocalContentResolver.update(XmsLogData.CONTENT_URI, values, SELECTION_BASE_ID, new String[]{baseId});
    }

    /**
     * @param pushStatus
     * @return number of updated rows
     */
    public int updatePushStatus(MimeType mimeType, PushStatus pushStatus){
        ContentValues values = new ContentValues();
        values.put(XmsLogData.KEY_PUSH_STATUS, pushStatus.toInt());
        return mLocalContentResolver.update(XmsLogData.CONTENT_URI, values, SELECTION_MIME_TYPE, new String[]{String.valueOf(mimeType.toInt())});
    }

    /**
     * @param contact
     * @param readStatus
     * @return SmsData
     */
    public List<XmsData> getMessages(String contact, ReadStatus readStatus) {

        List<XmsData> messages = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_BASE_ID_NATIVE_PROVIDER_ID,
                    SELECTION_CONTACT_READSTATUS, new String[] {
                            contact, String.valueOf(readStatus.toInt())
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_BASECOLUMN_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                XmsData xmsData = new XmsData();
                xmsData.setBaseId(cursor.getString(baseIdIdx));
                xmsData.setNativeProviderId(cursor.getLong(nativeProviderIdIdx));
                messages.add(xmsData);
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }

    }

    /**
     * @param nativeThreadId
     * @param readStatus
     * @return SmsData
     */
    public List<XmsData> getMessages(Long nativeThreadId, ReadStatus readStatus) {

        List<XmsData> messages = new ArrayList<XmsData>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_BASE_ID_NATIVE_PROVIDER_ID_CONTACT,
                    SELECTION_NATIVE_THREAD_ID_READSTATUS, new String[] {
                            String.valueOf(nativeThreadId), String.valueOf(readStatus.toInt())
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_BASECOLUMN_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_NATIVE_PROVIDER_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_CONTACT);
            while (cursor.moveToNext()) {
                XmsData xmsData = new XmsData();
                xmsData.setBaseId(cursor.getString(baseIdIdx));
                xmsData.setNativeProviderId(cursor.getLong(nativeProviderIdIdx));
                xmsData.setContact(cursor.getString(contactIdx));
                messages.add(xmsData);
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }

    }

    public List<String> getBaseIds(String contact, Direction direction, String messageCorrelator) {
        Cursor cursor = null;
        List<String> ids = new ArrayList<String>();
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_BASE_ID,
                    SELECTION_CONTACT_DIRECTION_CORRELATOR, new String[] {
                            contact, String.valueOf(direction.toInt()), messageCorrelator}
            , SORT_BY_DATE_DESC);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int idx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_BASECOLUMN_ID);
            while(cursor.moveToNext()) {
                ids.add(cursor.getString(idx));
            }
            return ids;
        } finally {
            CursorUtil.close(cursor);
        }

    }
    
    public List<XmsData> getMessages(String contact, ReadStatus readStatus, DeleteStatus deleteStatus) {

        List<XmsData> messages = new ArrayList<XmsData>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_ID_CONTACT_READ_DELETE,
                    SELECTION_CONTACT_READSTATUS_OR_DELETESTATUS, new String[] {contact,
                            String.valueOf(readStatus.toInt()),String.valueOf(deleteStatus.toInt())
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_BASECOLUMN_ID);
            int contactIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_CONTACT);
            int readIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_READ_STATUS);
            int deleteIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_DELETE_STATUS);
            while (cursor.moveToNext()) {
                XmsData xmsData = new XmsData();
                xmsData.setBaseId(cursor.getString(baseIdIdx));
                xmsData.setContact(cursor.getString(contactIdIdx));
                xmsData.setReadStatus(ReadStatus.valueOf(cursor.getInt(readIdIdx)));
                xmsData.setDeleteStatus(DeleteStatus.valueOf(cursor.getInt(deleteIdIdx)));
                messages.add(xmsData);
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }

    }

    public List<XmsData> getMessages(Long nativeThreadId, DeleteStatus deleteStatus) {

        List<XmsData> messages = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_BASE_ID_NATIVE_PROVIDER_ID_CONTACT,
                    SELECTION_NATIVE_THREAD_ID_DELETESTATUS, new String[] {
                            String.valueOf(nativeThreadId), String.valueOf(deleteStatus.toInt())
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_BASECOLUMN_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_NATIVE_PROVIDER_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_CONTACT);
            while (cursor.moveToNext()) {
                XmsData xmsData = new XmsData();
                xmsData.setBaseId(cursor.getString(baseIdIdx));
                xmsData.setNativeProviderId(cursor.getLong(nativeProviderIdIdx));
                xmsData.setContact(cursor.getString(contactIdx));
                messages.add(xmsData);
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }

    }

    public List<XmsData> getMessages(String contact, DeleteStatus deleteStatus) {

        List<XmsData> messages = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_BASE_ID_NATIVE_PROVIDER_ID,
                    SELECTION_CONTACT_DELETESTATUS, new String[] {
                            contact, String.valueOf(deleteStatus.toInt())
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_BASECOLUMN_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                XmsData xmsData = new XmsData();
                xmsData.setBaseId(cursor.getString(baseIdIdx));
                xmsData.setNativeProviderId(cursor.getLong(nativeProviderIdIdx));
                messages.add(xmsData);
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }

    }

    public MmsData getMessage(String mmsId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_BASE_ID,
                    SELECTION_MMS_ID, new String[] {mmsId}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsLogData.KEY_BASECOLUMN_ID);
            if(cursor.moveToNext()){
                MmsData mmsData = new MmsData();
                mmsData.setBaseId(cursor.getString(baseIdIdx));
                return mmsData;
            }
            return null;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    public Long getNativeProviderId(String baseId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_NATIVE_PROVIDER_ID, SELECTION_BASE_ID,
                    new String[] {
                            baseId
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex(XmsLogData.KEY_NATIVE_PROVIDER_ID));
            }            
            return null;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    public Set<String> getMmsIds(long threadId) {
        Cursor cursor = null;
        Set<String> mmsIds = new HashSet<String>();
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_MMS_ID, SELECTION_NATIVE_THREAD_ID_MMS,
                    new String[] {String.valueOf(threadId)}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int idx = cursor.getColumnIndex(XmsLogData.KEY_MMS_ID);
            while (cursor.moveToNext()) {
                mmsIds.add(cursor.getString(idx));
            }
            return mmsIds;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    public Set<String> getMmsIds(String contact) {
        Cursor cursor = null;
        Set<String> mmsIds = new HashSet<String>();
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_MMS_ID, SELECTION_CONTACT_MMS,
                    new String[] {contact}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int idx = cursor.getColumnIndex(XmsLogData.KEY_MMS_ID);
            while (cursor.moveToNext()) {
                mmsIds.add(cursor.getString(idx));
            }
            return mmsIds;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * @return
     */
    public Set<Long> getNativeProviderIds(MimeType mimeType) {
        Cursor cursor = null;
        Set<Long> ids = new HashSet<Long>();
        try {
            cursor = mLocalContentResolver.query(XmsLogData.CONTENT_URI, PROJECTION_NATIVE_PROVIDER_ID, SELECTION_MIME_TYPE,
                    new String[]{String.valueOf(mimeType.toInt())}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            while(cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }            
            return ids;
        } finally {
            CursorUtil.close(cursor);
        }
    }
    
    /**
     * @param nativeThreadId
     * @return id
     */
    public int deleteConversation(Long nativeThreadId) {
        return mLocalContentResolver.delete(XmsLogData.CONTENT_URI, SELECTION_NATIVE_THREAD_ID,
                new String[] {
                        String.valueOf(nativeThreadId)
                });
    }

    /**
     * @param baseId 
     * @return number of deleted message
     */
    public int deleteMessage(String baseId) {
        return mLocalContentResolver.delete(XmsLogData.CONTENT_URI, SELECTION_BASE_ID, new String[]{baseId});
    }
    
    /**
     * @param 
     * @return number of deleted message
     */
    public int deleteMessages() {
        return mLocalContentResolver.delete(XmsLogData.CONTENT_URI, null,null);
    }
    

}
