
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
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class XmsList2 extends Activity implements INativeSmsEventListener, BasicSynchronizationTaskListener, ImapServiceListener {

    private static final Logger sLogger = Logger.getLogger(XmsList2.class.getSimpleName());
    
    private final static String SORT = new StringBuilder(
            XmsData.KEY_DATE).append(" DESC").toString();

    private final static String WHERE_CLAUSE = new StringBuilder(XmsData.KEY_DELETE_STATUS).append("=").append(DeleteStatus.NOT_DELETED.toInt()).append(" group by ")
            .append(XmsData.KEY_CONTACT).toString();

    private final static String[] PROJECTION = new String[]{
            XmsData.KEY_BASECOLUMN_ID,
            XmsData.KEY_NATIVE_PROVIDER_ID,
            XmsData.KEY_CONTACT,
            XmsData.KEY_CONTENT,
            XmsData.KEY_DATE,
            XmsData.KEY_DIRECTION,
            XmsData.KEY_READ_STATUS
    };
    
    
    private final static int MESSAGE_MAX_SIZE =25;
    
    private final static int MENU_ITEM_DELETE_CONV = 1;
    
    private ListView mListview;
    private ArrayAdapter<SmsData> mArrayAdapter;
    private final List<SmsData> mMessages = new ArrayList<SmsData>();
    private CmsService mCmsService;

    private TextView mSyncButton;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rcs_cms_toolkit_xms_list);

        mArrayAdapter = new SmsLogAdapter(this);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mListview = (ListView) findViewById(android.R.id.list);
        mListview.setEmptyView(emptyView);
        mListview.setAdapter(mArrayAdapter);
        registerForContextMenu(mListview);
        mListview.setOnItemClickListener(getOnItemClickListener());
        Button sendButton = (Button) findViewById(R.id.rcs_cms_toolkit_xms_sendBtn);
        sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(XmsList2.this, XmsSendView.class));
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
                            XmsList2.this
                            ).execute(new String[] {});
                } catch (ImapServiceNotAvailableException e) {                
                    Toast.makeText(XmsList2.this, getString(R.string.label_cms_toolkit_xms_sync_already_in_progress), Toast.LENGTH_LONG).show();
                }                 
            }
            
        });
        
        mCmsService = CmsService.getInstance();                
           
    }

    @Override
    protected void onResume() {
        if(sLogger.isActivated()){
            sLogger.debug("onResume");
        }
        super.onResume();
        mCmsService.registerSmsObserverListener(this);
        checkImapServiceStatus();       
        queryProvider();
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

        private Drawable mDrawableIncoming;
        private Drawable mDrawableOutgoing;
        
        public SmsLogAdapter(Context context) {
            super(context, R.layout.rcs_cms_toolkit_xms_list_item, mMessages);
            
            mDrawableIncoming = context.getResources().getDrawable(
                    R.drawable.rcs_cms_toolkit_xms_incoming_call);
            mDrawableOutgoing = context.getResources().getDrawable(
                    R.drawable.rcs_cms_toolkit_xms_outgoing_call);            
            mContext = context;
        }

        private class ViewHolder {

            TextView mContact;
            TextView mContent;
            TextView mDate;
            ImageView mDirection;     
            int defaultColor;

            ViewHolder(View view) {
                mContact = (TextView) view.findViewById(R.id.rcs_cms_toolkit_xms_contact);
                mContent = (TextView) view.findViewById(R.id.rcs_cms_toolkit_xms_content);                
                mDate = (TextView) view.findViewById(R.id.rcs_cms_toolkit_xms_date);
                mDirection = (ImageView) view.findViewById(R.id.rcs_cms_toolkit_xms_direction_icon);
                defaultColor = mContent.getTextColors().getDefaultColor();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.rcs_cms_toolkit_xms_list_item, parent, false);
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

            // Set the status text and destination icon
            
            switch (msg.getDirection()) {
                case INCOMING:
                        viewHolder.mDirection.setImageDrawable(mDrawableIncoming);
                    break;
                case OUTGOING:
                        viewHolder.mDirection.setImageDrawable(mDrawableOutgoing);
                case IRRELEVANT:
                    break;
            }            
            viewHolder.mContact.setText(msg.getContact());
            viewHolder.mContent.setText(truncate(msg.getContent()));

            viewHolder.mContact.setTypeface(null,Typeface.NORMAL);
            viewHolder.mContent.setTypeface(null,Typeface.NORMAL);
            viewHolder.mContent.setTextColor(viewHolder.defaultColor);
            viewHolder.mContact.setTextColor(viewHolder.defaultColor);
            if(msg.getReadStatus() == ReadStatus.UNREAD){
                viewHolder.mContact.setTextColor(Color.GREEN);
                viewHolder.mContact.setTypeface(null, Typeface.BOLD|Typeface.ITALIC);
                viewHolder.mContent.setTextColor(Color.GREEN);
                viewHolder.mContent.setTypeface(null, Typeface.BOLD|Typeface.ITALIC);
            }            
            return convertView;
        }
    }

    protected void queryProvider() { 
            mMessages.clear();
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(XmsData.CONTENT_URI, PROJECTION, WHERE_CLAUSE, null, SORT);

                int baseIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_BASECOLUMN_ID);
                int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_PROVIDER_ID);                
                int contactIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
                int contentIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTENT);
                int dateIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DATE);
                int directionIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DIRECTION);
                int readIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_READ_STATUS);
                while (cursor.moveToNext()) {                    
                    SmsData smsData = new SmsData(
                            cursor.getLong(nativeProviderIdIdx),
                            cursor.getString(contactIdx),
                            cursor.getString(contentIdx),
                            cursor.getLong(dateIdx),
                            Direction.valueOf(cursor.getInt(directionIdx)),
                            ReadStatus.valueOf(cursor.getInt(readIdx))
                            );
                    smsData.setBaseId(cursor.getString(baseIdIdx));
                    mMessages.add(smsData);
                }

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        mArrayAdapter.notifyDataSetChanged();
    }

    private OnItemClickListener getOnItemClickListener() {
        return new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
               SmsData sms = (SmsData) (parent.getAdapter()).getItem(pos);
               startActivity(XmsConversationView.forgeIntentToStart(XmsList2.this, sms.getContact()));
            }
        };
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, MENU_ITEM_DELETE_CONV, MENU_ITEM_DELETE_CONV, R.string.rcs_cms_toolkit_xms_conversation_delete);        
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        SmsData message = mArrayAdapter.getItem(info.position);                        
        mMessages.remove(message);
        mArrayAdapter.notifyDataSetChanged(); 
        CmsService.getInstance().onDeleteRcsConversation(message.getContact());                
        return true;
    }
    
    private void refreshView(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                queryProvider();
            }
        });        
    }
    
    @Override
    public void onIncomingSms(SmsData message) {
        if(sLogger.isActivated()){
            sLogger.debug("onIncomingSms");
        }
        refreshView();
    }

    @Override
    public void onOutgoingSms(SmsData message) {
        if(sLogger.isActivated()){
            sLogger.debug("onOutgoingSms");
        }
        refreshView();
    }

    @Override
    public void onDeliverNativeSms(long nativeProviderId, long sentDate) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onDeleteNativeConversation(String contact) {
        if(sLogger.isActivated()){
            sLogger.debug("onDeleteNativeConversation");
        }
        refreshView();            
    }
    
    @Override
    public void onReadNativeSms(long nativeProviderId) {
        if(sLogger.isActivated()){
            sLogger.debug("onReadNativeSms");
        }
        refreshView();      
    }

    @Override
    public void onDeleteNativeSms(long nativeProviderId) {
        if(sLogger.isActivated()){
            sLogger.debug("onReadNativeSms");
        }
        refreshView();        
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
        queryProvider();        
        mListview.setSelection(0);  
    }
       
    private String truncate(String content){
        if(content.length() > MESSAGE_MAX_SIZE){
            content = content.substring(0, MESSAGE_MAX_SIZE);
            content = content.concat("...");
        }
        return content;
        
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
