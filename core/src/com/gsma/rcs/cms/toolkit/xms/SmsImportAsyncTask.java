package com.gsma.rcs.cms.toolkit.xms;

import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.PushStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.services.rcs.RcsService.Direction;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.Telephony.TextBasedSmsColumns;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

public class SmsImportAsyncTask extends AsyncTask<String,String,Boolean> {
    
    private static Uri sSmsUri = Uri.parse("content://sms/");
    
    private final String[] PROJECTION_SMS = new String[]{
            BaseColumns._ID,
            TextBasedSmsColumns.ADDRESS,
            TextBasedSmsColumns.DATE,
            TextBasedSmsColumns.DATE_SENT,
            TextBasedSmsColumns.PROTOCOL,
            TextBasedSmsColumns.BODY,
            TextBasedSmsColumns.READ};
    
    private final String[] PROJECTION_IDS = new String[]{
            BaseColumns._ID};

    private static final String SELECTION_CONTACT_NOT_NULL = new StringBuilder(TextBasedSmsColumns.ADDRESS).append(" is not null").toString();
    private static final String SELECTION_BASE_ID = new StringBuilder(BaseColumns._ID).append("=?").append(" AND ").append(SELECTION_CONTACT_NOT_NULL).toString();
    
    private ContentResolver mContentResolver;
    private XmsLog mXmsLog; 
    
    private ImportTaskListener mListener;
    
    /**
     * @param context 
     * @param xmsLog
     * @param listener 
     */
    public SmsImportAsyncTask(Context context, XmsLog xmsLog, ImportTaskListener listener ){
        mContentResolver = context.getContentResolver();
        mXmsLog = xmsLog;
        mListener = listener;
    }
    
    private void importMessages(){
        
        Set<Long> nativeIds = getNativeIds();
        Set<Long> rcsMessagesIds = getRcsMessageIds();
        
        // insert new ids only
        nativeIds.removeAll(rcsMessagesIds);
        
        for (Long id : nativeIds) {
            SmsData smsData = getMessageFromNativeProvider(id);
            if(smsData!=null){
                mXmsLog.addMessage(smsData);
            }
        }
    }

    private Set<Long> getNativeIds(){        
        Cursor cursor = null;
        Set<Long> ids = new HashSet<Long>();
        try {
            cursor = mContentResolver.query(sSmsUri, PROJECTION_IDS, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor,sSmsUri);
            while(cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }            
            return ids;
        } finally {
            CursorUtil.close(cursor);
        }
    }
   
    private Set<Long> getRcsMessageIds(){
        return  mXmsLog.getNativeProviderIds();
    }
        
    private SmsData getMessageFromNativeProvider(Long id){
                
        Cursor cursor = null;
        SmsData smsData = null;
        try {
            cursor = mContentResolver.query(sSmsUri, PROJECTION_SMS, SELECTION_BASE_ID, new String[]{String.valueOf(id)}, null);
            CursorUtil.assertCursorIsNotNull(cursor, sSmsUri);
            
            if(cursor.moveToFirst()) {                
                Long _id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                String  address = cursor.getString(cursor.getColumnIndex(TextBasedSmsColumns.ADDRESS));     
                PhoneNumber phoneNumber = ContactUtil
                        .getValidPhoneNumberFromAndroid(address);
                if(phoneNumber != null){
                    address = ContactUtil.createContactIdFromValidatedData(phoneNumber).toString();
                }
                else{
                    address = address.replaceAll(" ","");
                }
                long  date = cursor.getLong(cursor.getColumnIndex(TextBasedSmsColumns.DATE));
                long date_sent = cursor.getLong(cursor.getColumnIndex(TextBasedSmsColumns.DATE_SENT));
                String  protocol = cursor.getString(cursor.getColumnIndex(TextBasedSmsColumns.PROTOCOL));
                String  body = cursor.getString(cursor.getColumnIndex(TextBasedSmsColumns.BODY));                
                int read = cursor.getInt(cursor.getColumnIndex(TextBasedSmsColumns.READ));
                
                Direction direction = Direction.OUTGOING;
                if(protocol!=null){
                    direction = Direction.INCOMING;
                }
                
                ReadStatus readStatus = ReadStatus.READ;
                if(read==0){
                    readStatus = ReadStatus.UNREAD;
                }
                smsData = new SmsData(_id, address,body, date, direction, readStatus);
                smsData.setPushStatus(PushStatus.PUSH_REQUESTED);
                smsData.setDeliveryDate(date_sent);
            }  
            return smsData;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {
        importMessages();
        return true;
    }
    
    @Override
    protected void onPostExecute(Boolean result) {
        if(mListener!=null){
            mListener.onImportTaskExecuted(result);
        }
    }
    
    /**
    *
    */
   public interface ImportTaskListener {
       
       /**
        * @param params
        * @param result
        */
       public void onImportTaskExecuted(Boolean result);
   }
}
