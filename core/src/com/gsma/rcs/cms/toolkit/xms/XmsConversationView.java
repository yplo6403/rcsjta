package com.gsma.rcs.cms.toolkit.xms;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.CmsService;
import com.gsma.rcs.cms.event.INativeSmsEventListener;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceManager.ImapServiceListener;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.BasicSynchronizationTask;
import com.gsma.rcs.cms.imap.task.BasicSynchronizationTask.BasicSynchronizationTaskListener;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.XmsData;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
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
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class XmsConversationView extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>, INativeSmsEventListener, BasicSynchronizationTaskListener, ImapServiceListener {

    private final static Logger sLogger = Logger.getLogger(XmsConversationView.class.getSimpleName());

    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    protected static final int LOADER_ID = 1;

    private final static String SORT = new StringBuilder(
            XmsData.KEY_DATE).append(" ASC").toString();

    private final static String WHERE_CLAUSE = new StringBuilder(XmsData.KEY_CONTACT)
            .append("=?").append(" AND " ).append(XmsData.KEY_DELETE_STATUS).append("=").append(DeleteStatus.NOT_DELETED.toInt()).toString();

    private final static String[] PROJECTION = new String[]{
            XmsData.KEY_BASECOLUMN_ID,
            XmsData.KEY_NATIVE_PROVIDER_ID,
            XmsData.KEY_CONTACT,
            XmsData.KEY_CONTENT,
            XmsData.KEY_DATE,
            XmsData.KEY_DIRECTION,
            XmsData.KEY_READ_STATUS,
    }; 
        
    private SmsLogAdapter mAdapter;
    private XmsLog mXmsLog;
    private final static String EXTRA_CONTACT = "contact";

    private String mContact;
    private EditText mContent;
    private ListView mListView;
    private TextView mSyncButton;
    
    private final static int MENU_ITEM_DELETE_MSG = 1; 
    
    private CmsService mCmsService;
        
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rcs_cms_toolkit_xms_conversation_view);
        
        mContact = getIntent().getStringExtra(EXTRA_CONTACT);
        mXmsLog = XmsLog.getInstance(getApplicationContext());
        mAdapter = new SmsLogAdapter(this);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setEmptyView(emptyView);
        mListView.setAdapter(mAdapter);
        registerForContextMenu(mListView);
                
        mContent = (EditText) findViewById(R.id.rcs_cms_toolkit_xms_send_content);
        (findViewById(R.id.rcs_cms_toolkit_xms_send_message_btn)).setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                new SmsSender(getApplicationContext(), mContact, mContent.getText().toString()).send();
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
                try {      
                    new BasicSynchronizationTask(
                            ImapServiceManager.getService(CmsSettings.getInstance()),                 
                            LocalStorage.createInstance(ImapLog.getInstance(getApplicationContext())),
                            XmsConversationView.this
                            ).execute(new String[] {});
                } catch (ImapServiceNotAvailableException e) {                
                    Toast.makeText(XmsConversationView.this, getString(R.string.label_cms_toolkit_xms_sync_already_in_progress), Toast.LENGTH_LONG).show();
                }                 
            }
            
        });
        
        mCmsService = CmsService.getInstance();                     
    }

    @Override
    protected void onResume() {
        super.onResume();              
        mCmsService.registerSmsObserverListener(this);
        refreshView();
        if(!mXmsLog.getMessages(mContact, ReadStatus.UNREAD).isEmpty()){
            markConversationAsRead(mContact);
        }
        checkImapServiceStatus();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mCmsService.unregisterSmsObserverListener(this);
        ImapServiceManager.unregisterListener(this);
    }
    
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCmsService.unregisterSmsObserverListener(this);
        ImapServiceManager.unregisterListener(this);
    }

    /**
     * Messaging log adapter
     */
    private class SmsLogAdapter extends CursorAdapter {

        private LayoutInflater mInflater;
        private Drawable mItemRight;
        private Drawable mItemLeft;

        public SmsLogAdapter(Context context) {
            super(context, null, 0);
            mInflater = LayoutInflater.from(context);
            mItemRight = context.getResources().getDrawable(R.drawable.msg_item_right);
            mItemLeft = context.getResources().getDrawable(R.drawable.msg_item_left);
        }

        private class ViewHolder {

            RelativeLayout mItemLayout;
            TextView mContent;
            TextView mDate;

            int baseIdIdx;
            int nativeProviderIdIdx;
            int contactIdx;
            int contentIdx;
            int dateIdx;
            int directionIdx;
            int readStatusIdx;


            ViewHolder(View view, Cursor cursor) {

                baseIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_BASECOLUMN_ID);
                nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_PROVIDER_ID);
                contactIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
                contentIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTENT);
                dateIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DATE);
                directionIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DIRECTION);
                readStatusIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_READ_STATUS);

                mItemLayout = (RelativeLayout) view.findViewById(R.id.rcs_cms_toolkit_xms_conv_item);
                mContent = (TextView) view.findViewById(R.id.rcs_cms_toolkit_xms_content);
                mDate = (TextView) view.findViewById(R.id.rcs_cms_toolkit_xms_date);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mInflater.inflate(R.layout.rcs_cms_toolkit_xms_conversation_item, parent, false);
            view.setTag(new ViewHolder(view, cursor));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder holder = (ViewHolder) view.getTag();

            // Set the date/time field by mixing relative and absolute times
            long date = cursor.getLong(holder.dateIdx);
            holder.mDate.setText(DateUtils.getRelativeTimeSpanString(date,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE));
            holder.mContent.setText(cursor.getString(holder.contentIdx));


            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            switch (Direction.valueOf(cursor.getInt(holder.directionIdx))) {
                case INCOMING:
                    lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    holder.mItemLayout.setBackgroundDrawable(mItemRight);
                    break;
                case OUTGOING:
                    holder.mItemLayout.setBackgroundDrawable(mItemLeft);
                    lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                case IRRELEVANT:
                    break;
            }

            holder.mItemLayout.setLayoutParams(lp);
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
    public void onIncomingSms(SmsData message) {
        if(!message.getContact().equals(mContact)){
            return;
        }
        markMessageAsRead(message.getContact(), message.getBaseId());
        refreshView();
    }

    @Override
    public void onOutgoingSms(SmsData message) {
        refreshView();
    }

    @Override
    public void onDeliverNativeSms(long nativeProviderId, long sentDate) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onReadNativeSms(long nativeProviderId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onDeleteNativeSms(long nativeProviderId) {
        // TODO Auto-generated method stub   
    }
    
    @Override
    public void onDeleteNativeConversation(String contact) {
        // TODO Auto-generated method stub
        
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
        CmsService.getInstance().onDeleteRcsSms(mContact, cursor.getString(cursor.getColumnIndex(XmsData.KEY_BASECOLUMN_ID)));
        refreshView();
        return true;
    }
    
    private void markConversationAsRead(String contact){
        CmsService.getInstance().onReadRcsConversation(contact);
    }

    private void markMessageAsRead(String contact, String baseId){
        XmsLog.getInstance(getApplicationContext()).getMessages(mContact, ReadStatus.UNREAD);
        CmsService.getInstance().onReadRcsMessage(contact, baseId);
    }

    @Override
    public void onBasicSynchronizationTaskExecuted(String[] params, Boolean result) {
        if(sLogger.isActivated()){
            sLogger.info("onBasicSynchronizationTaskExecuted");
        }
        if(!result){
            Toast.makeText(this, getString(R.string.label_cms_toolkit_xms_sync_impossible), Toast.LENGTH_LONG).show();
        }
        displaySyncButton(true);
        refreshView();
    }
    
    private void checkImapServiceStatus(){
        if(!ImapServiceManager.isAvailable()){
            ImapServiceManager.registerListener(this);
            displaySyncButton(false);
        }
        else{
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
        return new CursorLoader(this, XmsData.CONTENT_URI, PROJECTION, WHERE_CLAUSE, new String[]{mContact}, SORT);
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
