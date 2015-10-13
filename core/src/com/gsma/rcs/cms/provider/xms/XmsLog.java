
package com.gsma.rcs.cms.provider.xms;

import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.PushStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;
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
            XmsData.KEY_BASECOLUMN_ID
    };
    
    private static final String[] PROJECTION_NATIVE_PROVIDER_ID = new String[] {
            XmsData.KEY_NATIVE_PROVIDER_ID 
    };
    
    private static final String[] PROJECTION_BASE_ID_NATIVE_PROVIDER_ID = new String[] {
            XmsData.KEY_BASECOLUMN_ID,
            XmsData.KEY_NATIVE_PROVIDER_ID 
    };

    private static final String[] PROJECTION_BASE_ID_CONTACT = new String[] {
            XmsData.KEY_BASECOLUMN_ID,
            XmsData.KEY_CONTACT
    };
    
    private static final String[] PROJECTION_ID_CONTACT_READ_DELETE = new String[] {
            XmsData.KEY_BASECOLUMN_ID,
            XmsData.KEY_CONTACT,
            XmsData.KEY_READ_STATUS,
            XmsData.KEY_DELETE_STATUS
    };
       
    private static final String[] PROJECTION_COUNT = new String[] {"count(*)"};
    
    private static final String SELECTION_CONTACT = new StringBuilder(XmsData.KEY_CONTACT).append("=?").toString();
    private static final String SELECTION_READ_STATUS = new StringBuilder(XmsData.KEY_READ_STATUS).append("=?").toString();
    private static final String SELECTION_DELETE_STATUS = new StringBuilder(XmsData.KEY_DELETE_STATUS).append("=?").toString();
    private static final String SELECTION_PUSH_STATUS = new StringBuilder(XmsData.KEY_PUSH_STATUS).append("=?").toString();
    private static final String SELECTION_UNREAD = new StringBuilder(XmsData.KEY_READ_STATUS).append("=").append(ReadStatus.UNREAD.toInt()).toString();    
    private static final String SELECTION_NATIVE_PROVIDER_ID = new StringBuilder(XmsData.KEY_NATIVE_PROVIDER_ID).append("=?").toString();
    private static final String SELECTION_BASE_ID = new StringBuilder(XmsData.KEY_BASECOLUMN_ID).append("=?").toString();    
    private static final String SELECTION_DIRECTION = new StringBuilder(XmsData.KEY_DIRECTION).append("=?").toString();
    private static final String SELECTION_CORRELATOR = new StringBuilder(XmsData.KEY_MESSAGE_CORRELATOR).append("=?").toString();
    
    private static final String SELECTION_CONTACT_UNREAD = new StringBuilder(SELECTION_CONTACT).append(" AND ").append(SELECTION_UNREAD).toString();
    private static final String SELECTION_NATIVE_PROVIDER_ID_UNREAD = new StringBuilder(SELECTION_NATIVE_PROVIDER_ID).append(" AND ")
            .append(SELECTION_UNREAD).toString();
    private static final String SELECTION_CONTACT_READSTATUS = new StringBuilder(SELECTION_CONTACT).append(" AND ").append(SELECTION_READ_STATUS).toString();
    private static final String SELECTION_CONTACT_DELETESTATUS = new StringBuilder(SELECTION_CONTACT).append(" AND ").append(SELECTION_DELETE_STATUS).toString();    
    private static final String SELECTION_READSTATUS_OR_DELETESTATUS = new StringBuilder(SELECTION_READ_STATUS).append(" OR ").append(SELECTION_DELETE_STATUS).toString();
    private static final String SELECTION_CONTACT_READSTATUS_OR_DELETESTATUS = new StringBuilder(SELECTION_CONTACT).append(" AND (").append(SELECTION_READ_STATUS).append(" OR ").append(SELECTION_DELETE_STATUS).append(")").toString();
    private static final String SELECTION_CONTACT_DIRECTION_CORRELATOR = new StringBuilder(SELECTION_CONTACT).append(" AND (").append(SELECTION_DIRECTION).append(" AND ").append(SELECTION_CORRELATOR).append(")").toString();
    
    private static final String SORT_BY_DATE_DESC = new StringBuilder(
            XmsData.KEY_DATE).append(" DESC").toString();
    private static final String SORT_BY_DATE_ASC = new StringBuilder(
            XmsData.KEY_DATE).append(" ASC").toString();
    
    
    private static final Logger sLogger = Logger.getLogger(XmsLog.class.getSimpleName());

    /**
     * Gets the instance of SecurityLog singleton
     * @param context  
     * @return the instance of SecurityLog singleton
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
     * @param localContentResolver
     */
    private XmsLog(Context context) {
        mLocalContentResolver = new LocalContentResolver(context.getContentResolver());
       
        HistoryProvider historyProvider = (HistoryProvider) context.getContentResolver()
                .acquireContentProviderClient(HistoryLogData.CONTENT_URI.getAuthority()).getLocalContentProvider();
        Map<String,String> columnMapping = new HashMap<String,String>(); 
        columnMapping.put(HistoryLog.PROVIDER_ID,
                String.valueOf(XmsData.HISTORYLOG_MEMBER_ID));
        columnMapping.put(HistoryLog.BASECOLUMN_ID, XmsData.KEY_BASECOLUMN_ID);
        columnMapping.put(HistoryLog.MIME_TYPE, XmsData.KEY_MIME_TYPE);
        columnMapping.put(HistoryLog.DIRECTION, XmsData.KEY_DIRECTION);
        columnMapping.put(HistoryLog.CONTACT, XmsData.KEY_CONTACT);
        columnMapping.put(HistoryLog.TIMESTAMP, XmsData.KEY_DATE);        
        columnMapping.put(HistoryLog.TIMESTAMP_SENT, XmsData.KEY_DATE);
        columnMapping.put(HistoryLog.TIMESTAMP_DELIVERED, XmsData.KEY_DELIVERY_DATE);
        columnMapping.put(HistoryLog.CHAT_ID, XmsData.KEY_CONTACT);
        columnMapping.put(HistoryLog.CONTENT, XmsData.KEY_CONTENT);
        
        //TODO FGI
        // traiter l'exception
        try {
            historyProvider.registerDatabase(XmsData.HISTORYLOG_MEMBER_ID, XmsData.CONTENT_URI, Uri.fromFile(context.getDatabasePath(XmsData.DATABASE_NAME)), XmsData.TABLE_XMS, columnMapping);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add Sms
     * @param msg 
     * @return baseColumnId
     */
    public String addMessage(AbstractXmsData msg) {

        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_CONTACT, msg.getContact());
        values.put(XmsData.KEY_CONTENT, msg.getContent());
        values.put(XmsData.KEY_DATE, msg.getDate());
        values.put(XmsData.KEY_DIRECTION, msg.getDirection().toInt());
        values.put(XmsData.KEY_MIME_TYPE, msg.getMimeType().toInt());
        values.put(XmsData.KEY_READ_STATUS, msg.getReadStatus().toInt());
        values.put(XmsData.KEY_DELETE_STATUS, msg.getDeleteStatus().toInt());
        values.put(XmsData.KEY_DELIVERY_DATE, msg.getDeliveryDate());
        values.put(XmsData.KEY_NATIVE_PROVIDER_ID, msg.getNativeProviderId());
        values.put(XmsData.KEY_PUSH_STATUS, msg.getPushStatus().toInt());
                
        if(msg instanceof SmsData){
            SmsData sms = (SmsData)msg;
            values.put(XmsData.KEY_MESSAGE_CORRELATOR, sms.getMessageCorrelator());
        }
        else if(msg instanceof MmsData){
            MmsData mms = (MmsData)msg;
            values.put(XmsData.KEY_SUBJECT, mms.getSubject());
            values.put(XmsData.KEY_ATTACHMENT, mms.getAttachment().toString());
        }
                
        Uri uri = mLocalContentResolver.insert(XmsData.CONTENT_URI, values);
        return uri.getLastPathSegment();
    }

    /**
     * @param contact
     * @return SmsData
     */
    public List<SmsData> getMessages(String contact) {
        
        List<SmsData> messages = new ArrayList<SmsData>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,SELECTION_CONTACT,new String[]{contact},SORT_BY_DATE_ASC);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);

            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_BASECOLUMN_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_PROVIDER_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
            int contentIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTENT);
            int dateIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DATE);
            int directionIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DIRECTION);
            int readStatusIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_READ_STATUS);
            int deleteStatusIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DELETE_STATUS);
            int deliveryDateIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DELIVERY_DATE);
            int messageCorrelatorIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_MESSAGE_CORRELATOR);
            
            while (cursor.moveToNext()) {
                messages.add(new SmsData(
                        cursor.getString(baseIdIdx),
                        cursor.getLong(nativeProviderIdIdx),
                        cursor.getString(contactIdx),
                        cursor.getString(contentIdx),
                        cursor.getLong(dateIdx),
                        cursor.getLong(deliveryDateIdx),
                        Direction.valueOf(cursor.getInt(directionIdx)),
                        ReadStatus.valueOf(cursor.getInt(readStatusIdx)),
                        DeleteStatus.valueOf(cursor.getInt(deleteStatusIdx)),
                        cursor.getString(messageCorrelatorIdx)                        
                        ));
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }
    
    /**
     * @param pushStatus 
     * @return SmsData
     */
    public List<SmsData> getMessages(PushStatus pushStatus) {
        
        List<SmsData> messages = new ArrayList<SmsData>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null,SELECTION_PUSH_STATUS,new String[]{String.valueOf(pushStatus.toInt())},SORT_BY_DATE_ASC);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);

            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_BASECOLUMN_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_PROVIDER_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
            int contentIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTENT);
            int dateIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DATE);
            int directionIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DIRECTION);
            int readStatusIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_READ_STATUS);
            int deleteStatusIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DELETE_STATUS);
            int deliveryDateIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DELIVERY_DATE);
            int messageCorrelatorIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_MESSAGE_CORRELATOR);
            
            while (cursor.moveToNext()) {
                messages.add(new SmsData(
                        cursor.getString(baseIdIdx),
                        cursor.getLong(nativeProviderIdIdx),
                        cursor.getString(contactIdx),
                        cursor.getString(contentIdx),
                        cursor.getLong(dateIdx),
                        cursor.getLong(deliveryDateIdx),
                        Direction.valueOf(cursor.getInt(directionIdx)),
                        ReadStatus.valueOf(cursor.getInt(readStatusIdx)),
                        DeleteStatus.valueOf(cursor.getInt(deleteStatusIdx)),
                        cursor.getString(messageCorrelatorIdx)                        
                        ));
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
    public SmsData getMessageByNativeProviderId(Long nativeProviderId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJECTION_BASE_ID_CONTACT, SELECTION_NATIVE_PROVIDER_ID,
                    new String[] {String.valueOf(nativeProviderId)}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            if (cursor.moveToFirst()) {
                int contactIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
                int idIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_BASECOLUMN_ID);
                return new SmsData(
                        cursor.getString(idIdx),
                        cursor.getString(contactIdx)
                        );
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
    public void setMessageAsDeliveredWithNativeProviderId(String nativeProviderId, long date) {
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_DELIVERY_DATE, date);
        mLocalContentResolver.update(XmsData.CONTENT_URI, values, SELECTION_NATIVE_PROVIDER_ID, new String[]{nativeProviderId});
    }
    
    /**
     * @param nativeProviderId
     * @param readStatus
     */
    public void updateReadStatusWithNativeProviderId(Long nativeProviderId, ReadStatus readStatus){
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_READ_STATUS, readStatus.toInt());
        mLocalContentResolver.update(XmsData.CONTENT_URI, values, SELECTION_NATIVE_PROVIDER_ID_UNREAD, new String[]{String.valueOf(nativeProviderId)});        
    } 
        
    /**
     * @param baseId
     * @param readStatus
     */
    public void updateReadStatusdWithBaseId(String baseId, ReadStatus readStatus){
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_READ_STATUS, readStatus.toInt());
        mLocalContentResolver.update(XmsData.CONTENT_URI, values, SELECTION_BASE_ID, new String[]{baseId});        
    }
    
    /**
     * @param contact
     * @param readStatus
     * @return number of updated rows
     */
    public int updateReadStatus(String contact, ReadStatus readStatus){
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_READ_STATUS, readStatus.toInt());
       return mLocalContentResolver.update(XmsData.CONTENT_URI, values, SELECTION_CONTACT_UNREAD, new String[]{contact});        
    } 
    
    /**
     * @param nativeProviderId
     * @param deleteStatus
     */
    public void updateDeleteStatusWithNativeProviderId(Long nativeProviderId, DeleteStatus deleteStatus){
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_DELETE_STATUS, deleteStatus.toInt());
        mLocalContentResolver.update(XmsData.CONTENT_URI, values, SELECTION_NATIVE_PROVIDER_ID, new String[]{String.valueOf(nativeProviderId)});        
    } 
        
    /**
     * @param baseId
     * @param deleteStatus
     */
    public void updateDeleteStatusdWithBaseId(String baseId, DeleteStatus deleteStatus){
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_DELETE_STATUS, deleteStatus.toInt());
        mLocalContentResolver.update(XmsData.CONTENT_URI, values, SELECTION_BASE_ID, new String[]{baseId});        
    }
    
    /**
     * @param contact
     * @param deleteStatus
     * @return number of updated rows
     */
    public int updateDeleteStatus(String contact,  DeleteStatus deleteStatus){
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_DELETE_STATUS, deleteStatus.toInt());
       return mLocalContentResolver.update(XmsData.CONTENT_URI, values, SELECTION_CONTACT, new String[]{contact});        
    } 
    
    /**
     * @param baseId 
     * @param pushStatus 
     * @return number of updated rows
     */
    public int updatePushStatus(String baseId, PushStatus pushStatus){
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_PUSH_STATUS, pushStatus.toInt());
       return mLocalContentResolver.update(XmsData.CONTENT_URI, values, SELECTION_BASE_ID, new String[]{baseId});        
    } 
    
    /**
     * @param contact
     * @param readStatus
     * @return SmsData
     */
    public List<SmsData> getMessages(String contact, ReadStatus readStatus) {

        List<SmsData> messages = new ArrayList<SmsData>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJECTION_BASE_ID_NATIVE_PROVIDER_ID,
                    SELECTION_CONTACT_READSTATUS, new String[] {
                            contact, String.valueOf(readStatus.toInt())
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_BASECOLUMN_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                messages.add(new SmsData(
                        cursor.getString(baseIdIdx),
                        cursor.getLong(nativeProviderIdIdx)));
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
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJECTION_BASE_ID,
                    SELECTION_CONTACT_DIRECTION_CORRELATOR, new String[] {
                            contact, String.valueOf(direction.toInt()), messageCorrelator}
            , SORT_BY_DATE_DESC);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            int idx = cursor.getColumnIndexOrThrow(XmsData.KEY_BASECOLUMN_ID);
            while(cursor.moveToNext()) {
                ids.add(cursor.getString(idx));
            }
            return ids;
        } finally {
            CursorUtil.close(cursor);
        }

    }
    
    public List<SmsData> getMessages(String contact, ReadStatus readStatus, DeleteStatus deleteStatus) {

        List<SmsData> messages = new ArrayList<SmsData>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJECTION_ID_CONTACT_READ_DELETE,
                    SELECTION_CONTACT_READSTATUS_OR_DELETESTATUS, new String[] {contact,
                            String.valueOf(readStatus.toInt()),String.valueOf(deleteStatus.toInt())
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_BASECOLUMN_ID);
            int contactIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
            int readIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_READ_STATUS);
            int deleteIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DELETE_STATUS);
            while (cursor.moveToNext()) {
                messages.add(new SmsData(
                        cursor.getString(baseIdIdx),
                        cursor.getString(contactIdIdx),
                        ReadStatus.valueOf(cursor.getInt(readIdIdx)),
                        DeleteStatus.valueOf(cursor.getInt(deleteIdIdx))
                        ));
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }

    }
    
    public List<SmsData> getMessages(ReadStatus readStatus, DeleteStatus deleteStatus) {

        List<SmsData> messages = new ArrayList<SmsData>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJECTION_ID_CONTACT_READ_DELETE,
                    SELECTION_READSTATUS_OR_DELETESTATUS, new String[] {
                            String.valueOf(readStatus.toInt()),String.valueOf(deleteStatus.toInt())
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_BASECOLUMN_ID);
            int contactIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
            int readIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_READ_STATUS);
            int deleteIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DELETE_STATUS);
            while (cursor.moveToNext()) {
                messages.add(new SmsData(
                        cursor.getString(baseIdIdx),
                        cursor.getString(contactIdIdx),
                        ReadStatus.valueOf(cursor.getInt(readIdIdx)),
                        DeleteStatus.valueOf(cursor.getInt(deleteIdIdx))
                        ));
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }

    }
    
    public List<SmsData> getMessages(String contact, DeleteStatus deleteStatus) {

        List<SmsData> messages = new ArrayList<SmsData>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJECTION_BASE_ID_NATIVE_PROVIDER_ID,
                    SELECTION_CONTACT_DELETESTATUS, new String[] {
                            contact, String.valueOf(deleteStatus.toInt())
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            int baseIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_BASECOLUMN_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_PROVIDER_ID);
            while (cursor.moveToNext()) {
                messages.add(new SmsData(
                        cursor.getString(baseIdIdx),
                        cursor.getLong(nativeProviderIdIdx)));
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }

    }
    
    public Long getNativeProviderId(String baseId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJECTION_NATIVE_PROVIDER_ID, SELECTION_BASE_ID,
                    new String[] {
                            baseId
            }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex(XmsData.KEY_NATIVE_PROVIDER_ID));
            }            
            return null;
        } finally {
            CursorUtil.close(cursor);
        }
    }
    
    /**
     * @return
     */
    public Set<Long> getNativeProviderIds() {
        Cursor cursor = null;
        Set<Long> ids = new HashSet<Long>();
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, PROJECTION_NATIVE_PROVIDER_ID, null,
                    null, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);
            while(cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }            
            return ids;
        } finally {
            CursorUtil.close(cursor);
        }
    }
    
    /**
     * @param contact
     * @return id
     */
    public int deleteConversationForContact(String contact) {
        return mLocalContentResolver.delete(XmsData.CONTENT_URI, SELECTION_CONTACT,
                new String[] {
                        contact
        });
    }

    /**
     * @param deleteStatus 
     * @return number of deleted message
     */
    public int deleteMessages(DeleteStatus deleteStatus) {
        return mLocalContentResolver.delete(XmsData.CONTENT_URI, SELECTION_DELETE_STATUS, new String[]{String.valueOf(deleteStatus.toInt())});
    }
    
    /**
     * @param baseId 
     * @return number of deleted message
     */
    public int deleteMessage(String baseId) {
        return mLocalContentResolver.delete(XmsData.CONTENT_URI, SELECTION_BASE_ID, new String[]{baseId});
    }
    
    /**
     * @param 
     * @return number of deleted message
     */
    public int deleteMessages() {
        return mLocalContentResolver.delete(XmsData.CONTENT_URI, null,null);
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
