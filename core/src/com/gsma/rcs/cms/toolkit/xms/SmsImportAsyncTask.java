package com.gsma.rcs.cms.toolkit.xms;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.observer.XmsObserverUtils;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.PartLog;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.MmsData;
import com.gsma.rcs.cms.provider.xms.model.MmsPart;
import com.gsma.rcs.cms.provider.xms.model.XmsData;
import com.gsma.rcs.cms.provider.xms.model.XmsData.PushStatus;
import com.gsma.rcs.cms.provider.xms.model.XmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.TextBasedSmsColumns;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class SmsImportAsyncTask extends AsyncTask<String,String,Boolean> {
    
    private static Uri sSmsUri = Uri.parse("content://sms/");
    private static Uri sMmsUri = Uri.parse("content://mms/");
    
    private final String[] PROJECTION_SMS = new String[]{
            BaseColumns._ID,
            TextBasedSmsColumns.THREAD_ID,
            TextBasedSmsColumns.ADDRESS,
            TextBasedSmsColumns.DATE,
            TextBasedSmsColumns.DATE_SENT,
            TextBasedSmsColumns.PROTOCOL,
            TextBasedSmsColumns.BODY,
            TextBasedSmsColumns.READ};
    
    private final String[] PROJECTION_IDS = new String[]{
            BaseColumns._ID};

    private static final String SELECTION_CONTACT_NOT_NULL = new StringBuilder(TextBasedSmsColumns.ADDRESS).append(" is not null").toString();
    static final String SELECTION_BASE_ID = new StringBuilder(BaseColumns._ID).append("=?").append(" AND ").append(SELECTION_CONTACT_NOT_NULL).toString();

    private ContentResolver mContentResolver;
    private XmsLog mXmsLog;
    private PartLog mPartLog;
    private CmsSettings mSettings;

    private ImportTaskListener mListener;
    
    /**
     * @param context 
     * @param xmsLog
     * @param listener 
     */
    public SmsImportAsyncTask(Context context,CmsSettings settings, XmsLog xmsLog, PartLog partLog, ImportTaskListener listener ){
        mContentResolver = context.getContentResolver();
        mXmsLog = xmsLog;
        mPartLog = partLog;
        mListener = listener;
        mSettings = settings;
    }
    
    private void importSms(){
        
        Set<Long> nativeIds = getSmsNativeIds();
        Set<Long> rcsMessagesIds = getRcsMessageIds(XmsData.MimeType.SMS);
        
        // insert new ids only
        nativeIds.removeAll(rcsMessagesIds);
        
        for (Long id : nativeIds) {
            SmsData smsData = getSmsFromNativeProvider(id);
            if(smsData!=null){
                mXmsLog.addSms(smsData);
            }
        }
    }

    private void importMms(){

        Set<Long> nativeIds = getMmsNativeIds();
        Set<Long> rcsMessagesIds = getRcsMessageIds(XmsData.MimeType.MMS);

        // insert new ids only
        nativeIds.removeAll(rcsMessagesIds);

        for (Long id : nativeIds) {
            MmsData mmsData = getMmsFromNativeProvider(id);
            if(mmsData!=null){
                mXmsLog.addMms(mmsData);
                mPartLog.addParts(mmsData.getMmsId(), mmsData.getParts());
            }
        }
    }

    private Set<Long> getSmsNativeIds(){
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

    private Set<Long> getMmsNativeIds(){
        Cursor cursor = null;
        Set<Long> ids = new HashSet<Long>();
        try {
            cursor = mContentResolver.query(sMmsUri, PROJECTION_IDS, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor,sMmsUri);
            while(cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
            return ids;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Set<Long> getRcsMessageIds(XmsData.MimeType mimeType){
        return  mXmsLog.getNativeProviderIds(mimeType);
    }
        
    private SmsData getSmsFromNativeProvider(Long id){
                
        Cursor cursor = null;
        SmsData smsData = null;
        try {
            cursor = mContentResolver.query(sSmsUri, PROJECTION_SMS, SELECTION_BASE_ID, new String[]{String.valueOf(id)}, null);
            CursorUtil.assertCursorIsNotNull(cursor, sSmsUri);
            
            if(cursor.moveToFirst()) {                
                Long _id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                Long threadId = cursor.getLong(cursor.getColumnIndex(TextBasedSmsColumns.THREAD_ID));
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
                smsData = new SmsData(_id, threadId, address,body, date, direction, readStatus);
                PushStatus pushStatus = mSettings.getPushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED;
                smsData.setPushStatus(pushStatus);
                smsData.setDeliveryDate(date_sent);
            }  
            return smsData;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private MmsData getMmsFromNativeProvider(Long id){

        Long threadId, date;
        date = -1l;
        String subject, messageId, from;
        subject = messageId =from = null;
        Direction direction = Direction.INCOMING;
        TreeSet<String> contacts = new TreeSet<>();
        int read;
        Cursor cursor = null;
        try{
            cursor = mContentResolver.query(XmsObserverUtils.Mms.URI, null, new StringBuilder(XmsObserverUtils.Mms.WHERE).append(" AND ").append(BaseColumns._ID).append("=?").toString(), new String[]{String.valueOf(id)}, Telephony.BaseMmsColumns._ID);
            CursorUtil.assertCursorIsNotNull(cursor, XmsObserverUtils.Mms.URI);
            if (!cursor.moveToNext()) {
                return null;
            }
            threadId = cursor.getLong(cursor.getColumnIndex(Telephony.BaseMmsColumns.THREAD_ID));
            subject =  cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns.SUBJECT));
            messageId = cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns.MESSAGE_ID));
            read = cursor.getInt(cursor.getColumnIndex(Telephony.BaseMmsColumns.READ));
            int messageType = cursor.getInt(cursor.getColumnIndex(Telephony.BaseMmsColumns.MESSAGE_TYPE));
            if(128 == messageType){
                direction = Direction.OUTGOING;
            }
            date = cursor.getLong(cursor.getColumnIndex(Telephony.BaseMmsColumns.DATE));
        }
        finally{
            CursorUtil.close(cursor);
        }

        // Get recipients
        try {
            int type = XmsObserverUtils.Mms.Addr.FROM;
            if(direction == Direction.OUTGOING){
                type = XmsObserverUtils.Mms.Addr.TO;
            }
            cursor = mContentResolver.query(Uri.parse(String.format(XmsObserverUtils.Mms.Addr.URI,id)), XmsObserverUtils.Mms.Addr.PROJECTION, XmsObserverUtils.Mms.Addr.WHERE, new String[]{String.valueOf(type)}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsObserverUtils.Mms.Addr.URI);
            int adressIdx = cursor.getColumnIndex(Telephony.Mms.Addr.ADDRESS);
            while(cursor.moveToNext()){
                contacts.add(cursor.getString(adressIdx));
            }
        } finally {
            CursorUtil.close(cursor);
        }

        // Get part
        List<MmsPart> parts = new ArrayList<>();
        String textContent = null;
        try {
            cursor = mContentResolver.query(Uri.parse(XmsObserverUtils.Mms.Part.URI), XmsObserverUtils.Mms.Part.PROJECTION, XmsObserverUtils.Mms.Part.WHERE, new String[]{String.valueOf(id)}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsObserverUtils.Mms.Part.URI);
            int _idIdx = cursor.getColumnIndexOrThrow(Telephony.BaseMmsColumns._ID);
            int contentTypeIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_TYPE);
            int contentIdIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_ID);
            int textIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.TEXT);
            int dataIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part._DATA);
            while(cursor.moveToNext()){
                String contentType = cursor.getString(contentTypeIdx);
                String text = cursor.getString(textIdx);
                if(Constants.CONTENT_TYPE_TEXT.equals(contentType)){
                    textContent = text;
                }
                parts.add(new MmsPart(
                        null,
                        cursor.getString(_idIdx),
                        contentType,
                        cursor.getString(contentIdIdx),
                        cursor.getString(dataIdx),
                        text));

            }
        }
        finally {
            CursorUtil.close(cursor);
        }

        ReadStatus readStatus = ReadStatus.UNREAD;
        if(read==1){
            readStatus = ReadStatus.READ;
        }
        MmsData mmsData = new MmsData(id, threadId, messageId, contacts, subject, textContent, date*1000, direction, readStatus);
        PushStatus pushStatus = mSettings.getPushMms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED;
        mmsData.setPushStatus(pushStatus);
        mmsData.setParts(parts);
        return mmsData;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        importSms();
        importMms();
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
        * @param result
        */
       public void onImportTaskExecuted(Boolean result);
   }
}
