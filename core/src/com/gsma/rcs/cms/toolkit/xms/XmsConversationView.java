package com.gsma.rcs.cms.toolkit.xms;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.CmsManager;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceController.ImapServiceListener;
import com.gsma.rcs.cms.imap.task.BasicSynchronizationTask;
import com.gsma.rcs.cms.imap.task.BasicSynchronizationTask.BasicSynchronizationTaskListener;
import com.gsma.rcs.cms.observer.XmsObserverListener;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.toolkit.Toolkit;
import com.gsma.rcs.cms.toolkit.ToolkitHandler;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.PartData;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;

public class XmsConversationView extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>, XmsObserverListener, BasicSynchronizationTaskListener, ImapServiceListener {

    private final static Logger sLogger = Logger.getLogger(XmsConversationView.class.getSimpleName());

    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    protected static final int LOADER_ID = 1;

    private int SIZE_100_DP;

    private static class Xms{

        private final static String SORT = new StringBuilder(
                XmsData.KEY_TIMESTAMP).append(" ASC").toString();

        private final static String WHERE = new StringBuilder(XmsData.KEY_CONTACT).append("=?").toString();

        private final static String[] PROJECTION = new String[]{
                BaseColumns._ID,
                XmsData.KEY_MESSAGE_ID,
                XmsData.KEY_NATIVE_ID,
                XmsData.KEY_CONTACT,
                XmsData.KEY_CONTENT,
                XmsData.KEY_TIMESTAMP,
                XmsData.KEY_DIRECTION,
                XmsData.KEY_READ_STATUS,
                XmsData.KEY_MIME_TYPE,
        };
    }

    private Core mCore;
    private XmsLogAdapter mAdapter;
    private RcsSettings mRcsSettings;
    private CmsManager mCmsManager;
    private XmsLog mXmsLog;
    private LocalStorage mLocalStorage;
    private final static String EXTRA_CONTACT = "contact";

    private ContactId mContact;
    private EditText mContent;
    private ListView mListView;
    private TextView mSyncButton;
    
    private final static int MENU_ITEM_DELETE_MSG = 1; 

    private class ImageViewOnClickListener implements OnClickListener{

        private Uri mUri;

        ImageViewOnClickListener(Uri uri){
            mUri = uri;
        }

        @Override
        public void onClick(View view) {
            startActivity(XmsImageView.forgeIntentToStart(XmsConversationView.this, mUri));
        }
    }


    private boolean hasStartedDisplayImageActivity = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCore = Toolkit.checkCore(this);
        if(mCore == null){
            return;
        }

        SIZE_100_DP = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());

        setContentView(R.layout.rcs_cms_toolkit_xms_conversation_view);

        mRcsSettings = RcsSettings.createInstance(new LocalContentResolver(getApplicationContext()));
        mCmsManager = mCore.getCmsService().getCmsManager();
        mXmsLog = mCmsManager.getXmsLog();
        mLocalStorage = mCmsManager.getLocalStorage();
        mContact = ContactUtil.createContactIdFromTrustedData(getIntent().getStringExtra(EXTRA_CONTACT));
        mAdapter = new XmsLogAdapter(this);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setEmptyView(emptyView);
        mListView.setAdapter(mAdapter);
        registerForContextMenu(mListView);
                
        mContent = (EditText) findViewById(R.id.rcs_cms_toolkit_xms_send_content);
        (findViewById(R.id.rcs_cms_toolkit_xms_send_message_btn)).setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                String messageContent = mContent.getText().toString();
                if(messageContent.isEmpty()){
                    return;
                }
                new SmsSender(getApplicationContext(), mContact.toString(), mContent.getText().toString()).send();
                mContent.setText("");
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE); 
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                
            }            
        });
        
        mSyncButton = (TextView)findViewById(R.id.rcs_cms_toolkit_sync_btn);
        mSyncButton.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                displaySyncButton(false);
                ToolkitHandler.getInstance().scheduleTask(new BasicSynchronizationTask(
                        getApplicationContext(),
                        mRcsSettings,
                        mCmsManager.getImapServiceController(),
                        mLocalStorage,
                        CmsUtils.contactToCmsFolder(mRcsSettings, mContact),
                        XmsConversationView.this
                        ));
            }
            
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!Toolkit.checkCore(this, mCore)){
            return;
        }
        mCmsManager.registerSmsObserverListener(this);
        if(!hasStartedDisplayImageActivity ) {
            refreshView();
        }
        hasStartedDisplayImageActivity = false;
        checkUnreadMessage();
        checkImapServiceStatus();
    }

    private void checkUnreadMessage(){
        Cursor cursor = null;
        try{
            cursor = mCmsManager.getXmsLog().getXmsMessages(mContact);
            int readStatusIdx = cursor.getColumnIndex(XmsData.KEY_READ_STATUS);
            while(cursor.moveToNext()){
                if(ReadStatus.UNREAD == ReadStatus.valueOf(cursor.getInt(readStatusIdx)))
                    markConversationAsRead(mContact);
                    break;
                }
            }
        finally{
            CursorUtil.close(cursor);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCmsManager.unregisterSmsObserverListener(this);
        mCmsManager.getImapServiceController().unregisterListener(this);
    }
    
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCmsManager.unregisterSmsObserverListener(this);
        mCmsManager.getImapServiceController().unregisterListener(this);
    }

    /**
     * Messaging log adapter
     */
    private class XmsLogAdapter extends CursorAdapter {

        private static final int VIEW_TYPE_SMS_IN = 0;
        private static final int VIEW_TYPE_SMS_OUT = 1;
        private static final int VIEW_TYPE_MMS_IN = 2;
        private static final int VIEW_TYPE_MMS_OUT = 3;
        private final int[] VIEW_TYPES = new int[]{
                VIEW_TYPE_SMS_IN,
                VIEW_TYPE_SMS_OUT,
                VIEW_TYPE_MMS_IN,
                VIEW_TYPE_MMS_OUT};

        private LayoutInflater mInflater;

        public XmsLogAdapter(Context context) {
            super(context, null, 0);
            mInflater = LayoutInflater.from(context);
        }

        private abstract class SmsViewHolder {

            RelativeLayout mItemLayout;
            TextView mContent;
            TextView mDate;
            int contactIdx;
            int contentIdx;
            int dateIdx;
            int directionIdx;

            private SmsViewHolder(View view, Cursor cursor){
                contactIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
                contentIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTENT);
                dateIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_TIMESTAMP);
                directionIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DIRECTION);
                mContent = (TextView) view.findViewById(R.id.rcs_cms_toolkit_xms_content);
                mDate = (TextView) view.findViewById(R.id.rcs_cms_toolkit_xms_date);
            }
        }

        private class MmsViewHolder extends SmsViewHolder {

            LinearLayout mImagesLayout;
            int messageIdIdx;
            LinearLayout.LayoutParams imageParams;

            private MmsViewHolder(View view, Cursor cursor) {
                super(view, cursor);
                messageIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_MESSAGE_ID);
                mImagesLayout = (LinearLayout) view.findViewById(R.id.rcs_cms_toolkit_xms_images_layout);
                imageParams = new LinearLayout.LayoutParams(SIZE_100_DP,SIZE_100_DP);
                imageParams.bottomMargin = SIZE_100_DP/10;
            }
        }

        private class SmsInViewHolder extends SmsViewHolder {
            private SmsInViewHolder(View view, Cursor cursor){
                super(view, cursor);
                mItemLayout = (RelativeLayout) view.findViewById(R.id.rcs_cms_toolkit_xms_conv_item_sms_in);
            }
        }

        private class SmsOutViewHolder extends SmsViewHolder {
            private SmsOutViewHolder(View view, Cursor cursor){
                super(view, cursor);
                mItemLayout = (RelativeLayout) view.findViewById(R.id.rcs_cms_toolkit_xms_conv_item_sms_out);
            }
        }

        private class MmsInViewHolder extends MmsViewHolder {
            private MmsInViewHolder(View view, Cursor cursor){
                super(view, cursor);
                mItemLayout = (RelativeLayout) view.findViewById(R.id.rcs_cms_toolkit_xms_conv_item_mms_in);
            }
        }

        private class MmsOutViewHolder extends MmsViewHolder {
            private MmsOutViewHolder(View view, Cursor cursor){
                super(view, cursor);
                mItemLayout = (RelativeLayout) view.findViewById(R.id.rcs_cms_toolkit_xms_conv_item_mms_out);
            }
        }


        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {

            int viewType  = getItemViewType(cursor);
            if(VIEW_TYPE_SMS_IN == viewType){
                final View view = mInflater.inflate(R.layout.rcs_cms_toolkit_xms_conversation_item_sms_in, parent, false);
                view.setTag(new SmsInViewHolder(view, cursor));
                return view;
            }
            if(VIEW_TYPE_SMS_OUT == viewType){
                final View view = mInflater.inflate(R.layout.rcs_cms_toolkit_xms_conversation_item_sms_out, parent, false);
                view.setTag(new SmsOutViewHolder(view, cursor));
                return view;
            }
            else if(VIEW_TYPE_MMS_IN == viewType){
                final View view = mInflater.inflate(R.layout.rcs_cms_toolkit_xms_conversation_item_mms_in, parent, false);
                view.setTag(new MmsInViewHolder(view, cursor));
                return view;
            }
            else { // MMS OUT
                final View view = mInflater.inflate(R.layout.rcs_cms_toolkit_xms_conversation_item_mms_out, parent, false);
                view.setTag(new MmsOutViewHolder(view, cursor));
                return view;
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            int viewType  = getItemViewType(cursor);
            if(VIEW_TYPE_SMS_IN  == viewType || VIEW_TYPE_SMS_OUT  == viewType){
                bindSmsView(view, cursor);
            }
            else{ //MMS
                bindMmsView(view, context, cursor);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return getItemViewType((Cursor)getItem(position));
        }

        private int getItemViewType(Cursor cursor){
            String mimeType = cursor.getString(cursor.getColumnIndex(XmsData.KEY_MIME_TYPE));
            Direction direction = Direction.valueOf(cursor.getInt(cursor.getColumnIndex(XmsData.KEY_DIRECTION)));
            if(MimeType.TEXT_MESSAGE.equals(mimeType)){
                if(Direction.INCOMING == direction ){
                    return VIEW_TYPE_SMS_IN;
                }
                else{
                    return VIEW_TYPE_SMS_OUT;
                }
            }
            else if(Direction.INCOMING == direction ){
                return VIEW_TYPE_MMS_IN;
            }
            else{
                return VIEW_TYPE_MMS_OUT;
            }
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPES.length;
        }

        private void bindSmsView(View view, Cursor cursor){

            SmsViewHolder holder = (SmsViewHolder)view.getTag();
            // Set the date/time field by mixing relative and absolute times
            long date = cursor.getLong(holder.dateIdx);
            holder.mDate.setText(DateUtils.getRelativeTimeSpanString(date,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE));
            holder.mContent.setText(cursor.getString(holder.contentIdx));
        }

        private void bindMmsView(View view, Context context, Cursor cursor){

            MmsViewHolder holder = (MmsViewHolder)view.getTag();
            // Set the date/time field by mixing relative and absolute times
            long date = cursor.getLong(holder.dateIdx);
            holder.mDate.setText(DateUtils.getRelativeTimeSpanString(date,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE));
            String messageId = cursor.getString(holder.messageIdIdx);
            holder.mContent.setText(cursor.getString(holder.contentIdx));
            holder.mImagesLayout.removeAllViews();

            Cursor partCursor = null;
            try{
                partCursor = mXmsLog.getMmsPart(messageId);
                int mimeTypeIdx = partCursor.getColumnIndex(PartData.KEY_MIME_TYPE);
                int fileIconIdx = partCursor.getColumnIndex(PartData.KEY_FILEICON);
                int contentIdx = partCursor.getColumnIndex(PartData.KEY_CONTENT);
                while(partCursor.moveToNext()){
                    // filter out content type that are not image
                    if(!MimeManager.isImageType(partCursor.getString(mimeTypeIdx))){
                        continue;
                    }
                    byte[] fileIcon = partCursor.getBlob(fileIconIdx);
                    ImageView imageView = new ImageView(context);
                    imageView.setOnClickListener(new ImageViewOnClickListener(Uri.parse(partCursor.getString(contentIdx))));
                    imageView.setLayoutParams(holder.imageParams);
                    imageView.setImageBitmap(BitmapFactory.decodeByteArray(fileIcon, 0, fileIcon.length));
                    holder.mImagesLayout.addView(imageView);
                }
            }
            finally {
                CursorUtil.close(partCursor);
            }
        }

    }

    /**
     * Forge intent to start XmsConversationView activity
     * 
     * @param context The context
     * @param contact The address
     * @return intent
     */
    public static Intent forgeIntentToStart(Context context, String contact) {
        Intent intent = new Intent(context, XmsConversationView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONTACT, contact);
        return intent;
    }
    
    private void refreshView(){
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }
    
    @Override
    public void onIncomingSms(SmsDataObject message) {
        if(!message.getContact().toString().equals(mContact)){
            return;
        }
        markMessageAsRead(message.getMessageId());
        refreshView();
    }

    @Override
    public void onOutgoingSms(SmsDataObject message) {
        refreshView();
    }

    @Override
    public void onDeleteSmsFromNativeApp(long nativeProviderId) {
    }

    @Override
    public void onIncomingMms(MmsDataObject message) {
        if(!message.getContact().toString().equals(mContact)){
            return;
        }
        markMessageAsRead(message.getMessageId());
        refreshView();
    }

    @Override
    public void onOutgoingMms(MmsDataObject message) {
        refreshView();
    }

    @Override
    public void onDeleteMmsFromNativeApp(String mmsId) {

    }

    @Override
    public void onXmsMessageStateChanged(Long nativeProviderId, String mimeType, State state) {

    }

    @Override
    public void onReadXmsConversationFromNativeApp(long nativeThreadId) {

    }

    @Override
    public void onDeleteXmsConversationFromNativeApp(long nativeThreadId) {
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, MENU_ITEM_DELETE_MSG, MENU_ITEM_DELETE_MSG, R.string.rcs_cms_toolkit_xms_message_delete);        
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
        mCmsManager.onDeleteXmsMessage(
                cursor.getString(cursor.getColumnIndex(XmsData.KEY_MESSAGE_ID)));
        refreshView();
        return true;
    }
    
    private void markConversationAsRead(ContactId contactId){
        mCmsManager.onReadXmsConversation(contactId);
    }

    private void markMessageAsRead(String messageId){
        mCmsManager.onReadXmsMessage(messageId);
    }

    @Override
    public void onBasicSynchronizationTaskExecuted(Boolean result) {
        if(sLogger.isActivated()) {
            sLogger.info("onBasicSynchronizationTaskExecuted");
        }
        if(!result){
            Toast.makeText(this, getString(R.string.label_cms_toolkit_xms_sync_impossible), Toast.LENGTH_LONG).show();
        }
        displaySyncButton(true);
        refreshView();
    }
    
    private void checkImapServiceStatus(){
        ImapServiceController imapServiceController = mCmsManager.getImapServiceController();
        if(!imapServiceController.isSyncAvailable()){
            imapServiceController.registerListener(this);
            displaySyncButton(false);
        } else{
            displaySyncButton(true);
        }
    }
    
    private void displaySyncButton(boolean display){
        if(display){
            mSyncButton.setVisibility(View.VISIBLE); 
            findViewById(R.id.rcs_cms_toolkit_xms_progressbar).setVisibility(View.GONE);            
        }
        else{
            mSyncButton.setVisibility(View.GONE); 
            findViewById(R.id.rcs_cms_toolkit_xms_progressbar).setVisibility(View.VISIBLE);            
            
        }    
    }

    @Override
    public void onImapServiceAvailable() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displaySyncButton(true);
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        /* Create a new CursorLoader with the following query parameters. */
        return new CursorLoader(this, XmsData.CONTENT_URI, Xms.PROJECTION, Xms.WHERE, new String[]{mContact.toString()}, Xms.SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        /* A switch-case is useful when dealing with multiple Loaders/IDs */
        switch (loader.getId()) {
            case LOADER_ID:
                /*
                 * The asynchronous load is complete and the data is now available for use. Only now
                 * can we associate the queried Cursor with the CursorAdapter.
                 */
                mAdapter.swapCursor(cursor);
                mListView.smoothScrollToPosition(mAdapter.getCount());
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {        /*
         * For whatever reason, the Loader's data is now unavailable. Remove any references to the
         * old data by replacing it with a null Cursor.
         */
        mAdapter.swapCursor(null);
    }
}
