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
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class XmsConversationView extends Activity implements INativeSmsEventListener, BasicSynchronizationTaskListener, ImapServiceListener {

    private final static Logger sLogger = Logger.getLogger(XmsConversationView.class.getSimpleName());
    
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
        
    private ArrayAdapter<SmsData> mArrayAdapter;
    private final List<SmsData> mMessages = new ArrayList<SmsData>();

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
        
        mArrayAdapter = new SmsLogAdapter(this);        
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setEmptyView(emptyView);
        mListView.setAdapter(mArrayAdapter);        
        registerForContextMenu(mListView);
                
        mContent = (EditText) findViewById(R.id.rcs_cms_toolkit_xms_send_content);
        ((Button) findViewById(R.id.rcs_cms_toolkit_xms_send_message_btn)).setOnClickListener(new OnClickListener(){
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
        if(queryProvider(mContact)){
            markConversationAsRead(mContact);    
        }        
        mListView.setSelection(mArrayAdapter.getCount());           
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
    private class SmsLogAdapter extends ArrayAdapter<SmsData> {
        private Context mContext;

        public SmsLogAdapter(Context context) {
            super(context, R.layout.rcs_cms_toolkit_xms_conversation_item, mMessages);
            mContext = context;
        }

        private class ViewHolder {

            RelativeLayout mItemLayout;
            TextView mContent;
            TextView mDate;                  
            Drawable mItemRight;
            Drawable mItemLeft;

            ViewHolder(View view) {
                mItemLayout = (RelativeLayout) view.findViewById(R.id.rcs_cms_toolkit_xms_conv_item);
                mContent = (TextView) view.findViewById(R.id.rcs_cms_toolkit_xms_content);                
                mDate = (TextView) view.findViewById(R.id.rcs_cms_toolkit_xms_date);                
                mItemRight = mContext.getResources().getDrawable(R.drawable.msg_item_right);
                mItemLeft = mContext.getResources().getDrawable(R.drawable.msg_item_left);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.rcs_cms_toolkit_xms_conversation_item, parent, false);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            SmsData msg = mMessages.get(position);

            // Set the date/time field by mixing relative and absolute times
            long date = msg.getDate();
            viewHolder.mDate.setText(DateUtils.getRelativeTimeSpanString(date,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE));
            viewHolder.mContent.setText(msg.getContent());
            
            
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            switch (msg.getDirection()) {
                case INCOMING:
                    lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    viewHolder.mItemLayout.setBackgroundDrawable(viewHolder.mItemRight);
                    break;
                case OUTGOING:
                    viewHolder.mItemLayout.setBackgroundDrawable(viewHolder.mItemLeft);
                    lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);                        
                case IRRELEVANT:
                    break;
            }

            viewHolder.mItemLayout.setLayoutParams(lp);                        
            return convertView;
        }
    }

    /**
     * Return true if the conversation should be considered as read
     * ie at least one message is unread in content provider
     * @param contact
     * @return
     */
    protected Boolean queryProvider(String contact) { 
            mMessages.clear();
            Boolean markConversationAsRead = false;
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(XmsData.CONTENT_URI, PROJECTION, WHERE_CLAUSE, new String[]{contact}, SORT);
                int baseIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_BASECOLUMN_ID);
                int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_PROVIDER_ID);                
                int contactIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
                int contentIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTENT);
                int dateIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DATE);
                int directionIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DIRECTION);     
                int readStatusIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_READ_STATUS);
                while (cursor.moveToNext()) {
                    ReadStatus readStatus = ReadStatus.valueOf(cursor.getInt(readStatusIdx));
                    if(ReadStatus.UNREAD == readStatus){
                        markConversationAsRead = true;
                    }
                    SmsData mySmsMessage = new SmsData(
                            cursor.getLong(nativeProviderIdIdx),
                            cursor.getString(contactIdx),
                            cursor.getString(contentIdx),
                            cursor.getLong(dateIdx),
                            Direction.valueOf(cursor.getInt(directionIdx)),
                            readStatus
                            );                          
                    mySmsMessage.setBaseId(cursor.getString(baseIdIdx));
                    mMessages.add(mySmsMessage);
                }
                mArrayAdapter.notifyDataSetChanged();
                return markConversationAsRead;
            } finally {
                if (cursor!=null) {
                    cursor.close();
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mArrayAdapter.notifyDataSetChanged();
                mListView.smoothScrollToPosition(mListView.getLastVisiblePosition()+1);
            }
        });        
    }
    
    @Override
    public void onIncomingSms(SmsData message) {
        markMessageAsRead(message.getContact(), message.getBaseId());
        mMessages.add(message);
        refreshView();
    }

    @Override
    public void onOutgoingSms(SmsData message) {
        mMessages.add(message);
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
        SmsData message = mArrayAdapter.getItem(info.position);                            
        mMessages.remove(message);
        mArrayAdapter.notifyDataSetChanged();                
        CmsService.getInstance().onDeleteRcsSms(mContact, message.getBaseId());
        return true;
    }
    
    private void markConversationAsRead(String contact){
        CmsService.getInstance().onReadRcsConversation(contact);
    }

    private void markMessageAsRead(String contact, String baseId){
        CmsService.getInstance().onReadRcsMessage(contact, baseId);
    }

    @Override
    public int getPriority() {
        return PRIORITY_LOW;
    }
    
    @Override
    public int compareTo(INativeSmsEventListener another) {
        return another.getPriority() - getPriority();
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
        queryProvider(mContact);        
        mListView.setSelection(mArrayAdapter.getCount());  
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
                displaySyncButton(true);            }
        });
    }
}
