package com.gsma.rcs.cms.toolkit.xms;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.BaseMmsColumns;
import android.provider.Telephony.TextBasedSmsColumns;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.observer.XmsObserverUtils;
import com.gsma.rcs.cms.observer.XmsObserverUtils.Mms;
import com.gsma.rcs.cms.observer.XmsObserverUtils.Mms.Part;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.cms.utils.MmsUtils;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.ImageUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SmsImportAsyncTask extends AsyncTask<String,String,Boolean> {

    private static final Logger sLogger = Logger.getLogger(SmsImportAsyncTask.class.getSimpleName());

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

    private static final String SELECTION_CONTACT_NOT_NULL = TextBasedSmsColumns.ADDRESS + " is not null";
    static final String SELECTION_BASE_ID = BaseColumns._ID + "=?" + " AND " + SELECTION_CONTACT_NOT_NULL;

    private final Context mCtx;
    private final ContentResolver mContentResolver;
    private final XmsLog mXmsLog;
    private final ImapLog mImapLog;
    private final RcsSettings mSettings;
    private final ImportTaskListener mListener;
    
    /**
     * @param context The context
     * @param settings The RCS settings accessor
     * @param xmsLog The XMS log accessor
     * @param imapLog The IMAP log accessor
     * @param listener The import task listener
     */
    public SmsImportAsyncTask(Context context,RcsSettings settings, XmsLog xmsLog, ImapLog imapLog, ImportTaskListener listener ){
        mCtx = context;
        mContentResolver = context.getContentResolver();
        mXmsLog = xmsLog;
        mImapLog = imapLog;
        mListener = listener;
        mSettings = settings;
    }
    
    private void importSms(){

        Set<Long> nativeIds = getSmsNativeIds();
        Set<Long> rcsMessagesIds = getRcsMessageIds(MimeType.TEXT_MESSAGE);

        // insert new ids only
        nativeIds.removeAll(rcsMessagesIds);

        for (Long id : nativeIds) {
            SmsDataObject smsData = getSmsFromNativeProvider(id);
            if(smsData!=null){
                mXmsLog.addSms(smsData);
                mImapLog.addMessage(new MessageData(
                        CmsUtils.contactToCmsFolder(mSettings, smsData.getContact()),
                        MessageData.ReadStatus.READ,
                        MessageData.DeleteStatus.NOT_DELETED,
                        mSettings.getCmsPushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                        MessageType.SMS,
                        smsData.getMessageId(),
                        smsData.getNativeProviderId()
                ));
            }
        }
    }

    private void importMms(){

        Set<Long> nativeIds = getMmsNativeIds();
        Set<Long> rcsMessagesIds = getRcsMessageIds(MimeType.MULTIMEDIA_MESSAGE);

        // insert new ids only
        nativeIds.removeAll(rcsMessagesIds);

        for (Long id : nativeIds) {
            Collection<MmsDataObject> mmsDataObjects = getMmsFromNativeProvider(id);
            if(mmsDataObjects == null){
                continue;
            }
            for(MmsDataObject mmsData : mmsDataObjects){
                    mXmsLog.addMms(mmsData);
                    mImapLog.addMessage(new MessageData(
                            CmsUtils.contactToCmsFolder(mSettings, mmsData.getContact()),
                            MessageData.ReadStatus.READ,
                            MessageData.DeleteStatus.NOT_DELETED,
                            mSettings.getCmsPushMms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                            MessageType.MMS,
                            mmsData.getMessageId(),
                            mmsData.getNativeProviderId()
                    ));
            }
        }
    }

    private Set<Long> getSmsNativeIds(){
        Cursor cursor = null;
        Set<Long> ids = new HashSet<>();
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
        Set<Long> ids = new HashSet<>();
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

    private Set<Long> getRcsMessageIds(String mimeType){
        Cursor cursor = null;
        Set<Long> ids = new HashSet<>();
        try{
            cursor = mXmsLog.getXmsMessages(mimeType);
            CursorUtil.assertCursorIsNotNull(cursor, XmsMessageLog.CONTENT_URI);
            int nativeIdIdx = cursor.getColumnIndex(XmsData.KEY_NATIVE_ID);
            while(cursor.moveToNext()){
                // TODO consider null value for native ID
                ids.add(cursor.getLong(nativeIdIdx));
            }
            return ids;
        }
        finally{
            CursorUtil.close(cursor);
        }
    }
        
    private SmsDataObject getSmsFromNativeProvider(Long id){
                
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(sSmsUri, PROJECTION_SMS, SELECTION_BASE_ID, new String[]{String.valueOf(id)}, null);
            CursorUtil.assertCursorIsNotNull(cursor, sSmsUri);
            
            if(!cursor.moveToFirst()) {
                return null;
            }
            Long _id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
            Long threadId = cursor.getLong(cursor.getColumnIndex(TextBasedSmsColumns.THREAD_ID));
            String  address = cursor.getString(cursor.getColumnIndex(TextBasedSmsColumns.ADDRESS));
            PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromAndroid(address);
            if(phoneNumber==null){
                return null;
            }
            ContactId contactId = ContactUtil.createContactIdFromValidatedData(phoneNumber);
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
            SmsDataObject smsDataObject = new SmsDataObject(
                    IdGenerator.generateMessageID(),
                    contactId,
                    body,
                    direction,
                    readStatus,
                    date,
                    _id,
                    threadId
            );
            smsDataObject.setTimestampDelivered(date_sent);
            return smsDataObject;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Collection<MmsDataObject> getMmsFromNativeProvider(Long id){
        List<MmsDataObject> mmsDataObject = new ArrayList<>();
        Long threadId, date;
        date = -1l;
        String mmsId;
        Direction direction = Direction.INCOMING;
        Set<ContactId> contacts = new HashSet<>();
        ReadStatus readStatus;
        Cursor cursor = null;
        try{
            // TODO use constant string
            cursor = mContentResolver.query(XmsObserverUtils.Mms.URI, null, Mms.WHERE + " AND " + BaseColumns._ID + "=?", new String[]{String.valueOf(id)}, Telephony.BaseMmsColumns._ID);
            CursorUtil.assertCursorIsNotNull(cursor, XmsObserverUtils.Mms.URI);
            if (!cursor.moveToNext()) {
                return mmsDataObject;
            }
            threadId = cursor.getLong(cursor.getColumnIndex(Telephony.BaseMmsColumns.THREAD_ID));
            mmsId = cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns.MESSAGE_ID));
            readStatus = cursor.getInt(cursor.getColumnIndex(Telephony.BaseMmsColumns.READ))==0 ? ReadStatus.UNREAD : ReadStatus.READ;
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
        Map<ContactId,String> messageIds = new HashMap<>();
        try {
            int type = XmsObserverUtils.Mms.Addr.FROM;
            if(direction == Direction.OUTGOING){
                type = XmsObserverUtils.Mms.Addr.TO;
            }
            cursor = mContentResolver.query(Uri.parse(String.format(XmsObserverUtils.Mms.Addr.URI,id)), XmsObserverUtils.Mms.Addr.PROJECTION, XmsObserverUtils.Mms.Addr.WHERE, new String[]{String.valueOf(type)}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsObserverUtils.Mms.Addr.URI);
            int adressIdx = cursor.getColumnIndex(Telephony.Mms.Addr.ADDRESS);
            while(cursor.moveToNext()){
                String address = cursor.getString(adressIdx);
                PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromAndroid(address);
                if(phoneNumber == null){
                    if(sLogger.isActivated()){
                        sLogger.info("Bad format for contact : " + address);
                    }
                    continue;
                }
                ContactId contact = ContactUtil.createContactIdFromValidatedData(phoneNumber);
                messageIds.put(contact, IdGenerator.generateMessageID());
                contacts.add(contact);
            }
        } finally {
            CursorUtil.close(cursor);
        }

        // Get part
        Map<ContactId,List<MmsPart>> mmsParts= new HashMap<>();
        String textContent = null;
        try {
            cursor = mContentResolver.query(Uri.parse(Mms.Part.URI), Mms.Part.PROJECTION, Mms.Part.WHERE, new String[]{String.valueOf(id)}, null);
            CursorUtil.assertCursorIsNotNull(cursor, Mms.Part.URI);
            int _idIdx = cursor.getColumnIndexOrThrow(BaseMmsColumns._ID);
            int filenameIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.NAME);
            int contentTypeIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_TYPE);
            int textIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.TEXT);
            int dataIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part._DATA);

            while(cursor.moveToNext()){
                String contentType = cursor.getString(contentTypeIdx);
                String text = cursor.getString(textIdx);
                String filename = cursor.getString(filenameIdx);
                String content;
                long fileSize = 0l;
                byte[] fileIcon = null;
                if(Constants.CONTENT_TYPE_TEXT.equals(contentType)){
                    textContent = text;
                }
                String data = cursor.getString(dataIdx);
                if(data != null){
                    content = Part.URI.concat(cursor.getString(_idIdx));
                    Uri file =  Uri.parse(content);
                    byte[] bytes = MmsUtils.getContent(mContentResolver,file);
                    fileSize = bytes.length;
                    if (MimeManager.isImageType(contentType)) {
                        long maxIconSize = mSettings.getMaxFileIconSize();
                        fileIcon = ImageUtils.tryGetThumbnail(mCtx, file, maxIconSize);
                    }
                }
                else{
                    content = text;
                }

                for(ContactId contact : contacts){
                    List<MmsPart> mmsPart = mmsParts.get(contact);
                    if(mmsPart == null){
                        mmsPart = new ArrayList<>();
                        mmsParts.put(contact, mmsPart);
                    }
                    mmsPart.add(new MmsPart(
                            messageIds.get(contact),
                            contact,
                            contentType,
                            filename,
                            fileSize,
                            content,
                            fileIcon
                    ));
                }
            }
        }
        finally {
            CursorUtil.close(cursor);
        }
        String subject = null; // TODO
        Iterator<Entry<ContactId, List<MmsPart>>> iter = mmsParts.entrySet().iterator();
        while(iter.hasNext()){
            Entry<ContactId, List<MmsPart>> entry = iter.next();
            ContactId contact = entry.getKey();
            mmsDataObject.add(new MmsDataObject(
                    mmsId,
                    messageIds.get(contact),
                    contact,
                    subject,
                    textContent,
                    direction,
                    readStatus,
                    date*1000,
                    id,
                    threadId,
                    entry.getValue()
            ));
        }
        return mmsDataObject;
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
       void onImportTaskExecuted(Boolean result);
   }
}
