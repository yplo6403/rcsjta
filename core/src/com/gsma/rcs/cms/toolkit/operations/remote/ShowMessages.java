
package com.gsma.rcs.cms.toolkit.operations.remote;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.ShowMessagesTask;
import com.gsma.rcs.cms.imap.task.ShowMessagesTask.ShowMessagesTaskListener;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask.UpdateFlagTaskListener;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.cms.sync.strategy.FlagChange.Operation;
import com.gsma.rcs.cms.toolkit.AlertDialogUtils;
import com.gsma.rcs.cms.toolkit.Toolkit;
import com.gsma.rcs.cms.toolkit.ToolkitHandler;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.Base64;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ShowMessages extends ListActivity implements ShowMessagesTaskListener, UpdateFlagTaskListener {

    private final int MENU_ITEM_SET_READ_FLAG = 0;
    private final int MENU_ITEM_UNSET_READ_FLAG = 1;
    private final int MENU_ITEM_SET_DELETED_FLAG = 2;
    private final int MENU_ITEM_UNSET_DELETED_FLAG = 3;

    private ListView mListView;
    private ArrayAdapter<Message> mArrayAdapter;
    private RcsSettings mSettings;
    private AlertDialog mInProgressDialog;
    private ImapServiceController mImapServiceController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Toolkit.checkCore(this) == null){
            return;
        }
        mImapServiceController = Core.getInstance().getCmsService().getCmsManager().getImapServiceController();;
        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.rcs_cms_toolkit_messages_list);

        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setEmptyView(emptyView);
        registerForContextMenu(mListView);
        mSettings = RcsSettings.createInstance(new LocalContentResolver(getApplicationContext()));

        try {
            ToolkitHandler.getInstance().scheduleTask(new ShowMessagesTask(mImapServiceController, this));
            mInProgressDialog = AlertDialogUtils.displayInfo(ShowMessages.this,
                    getString(R.string.cms_toolkit_in_progress));
        } catch (ImapServiceNotAvailableException e) {
            Toast.makeText(this, getString(R.string.label_cms_toolkit_xms_sync_impossible), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        Message message = (Message) mArrayAdapter
                .getItem(((AdapterContextMenuInfo) menuInfo).position);
        Set<Flag> flags = message.getImapMessage().getMetadata().getFlags();

        if (flags.contains(Flag.Seen)) {
            menu.add(0, MENU_ITEM_UNSET_READ_FLAG, MENU_ITEM_UNSET_READ_FLAG,
                    getResources().getString(
                            R.string.cms_toolkit_remote_operations_show_messages_unset_read_flag));
        } else {
            menu.add(0, MENU_ITEM_SET_READ_FLAG, MENU_ITEM_SET_READ_FLAG, getResources()
                    .getString(R.string.cms_toolkit_remote_operations_show_messages_set_read_flag));
        }

        if (flags.contains(Flag.Deleted)) {
            menu.add(0, MENU_ITEM_UNSET_DELETED_FLAG, MENU_ITEM_UNSET_DELETED_FLAG,
                    getResources().getString(
                            R.string.cms_toolkit_remote_operations_show_messages_unset_deleted_flag));
        } else {
            menu.add(0, MENU_ITEM_SET_DELETED_FLAG, MENU_ITEM_SET_DELETED_FLAG,
                    getResources().getString(
                            R.string.cms_toolkit_remote_operations_show_messages_set_deleted_flag));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
         AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
         Message message = (Message) (mArrayAdapter.getItem(info.position));

         Operation operation = null;
         Flag flag = null;
         switch (item.getItemId()) {
             case MENU_ITEM_SET_READ_FLAG:    
                 operation = Operation.ADD_FLAG;
                 flag = Flag.Seen;                 
                 break;
             case MENU_ITEM_UNSET_READ_FLAG:
                 operation = Operation.REMOVE_FLAG;
                 flag = Flag.Seen;
                 break;
             case MENU_ITEM_SET_DELETED_FLAG:
                 operation = Operation.ADD_FLAG;
                 flag = Flag.Deleted;
                 break;
             case MENU_ITEM_UNSET_DELETED_FLAG:
                 operation = Operation.REMOVE_FLAG;
                 flag = Flag.Deleted;
                 break;
             }
         FlagChange flagChange =  new FlagChange(message.getImapMessage().getFolderPath(), message.getImapMessage().getUid(), flag,  operation);
         new Thread(new UpdateFlagTask(
                 mImapServiceController,
                 Arrays.asList(flagChange),
                 this
         )).start();
        mInProgressDialog.show();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mInProgressDialog != null) {
            mInProgressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInProgressDialog != null) {
            mInProgressDialog.dismiss();
        }
    }

    public class Message {

        private ImapMessage mImapMessage;

        public Message(ImapMessage imapMessage) {
            mImapMessage = imapMessage;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("UID:").append(mImapMessage.getUid()).append("\r\n");
            sb.append("MODSEQ:").append(mImapMessage.getMetadata().getModseq()).append("\r\n");
            String body;
            String encoding = mImapMessage.getBody().getHeader(Constants.HEADER_CONTENT_TRANSFER_ENCODING);
            if(encoding!=null && Constants.HEADER_BASE64.equals(encoding)){
                body = new String(Base64.decodeBase64(mImapMessage.getTextBody().getBytes()));
            }
            else{
                body = mImapMessage.getTextBody();
            }
            sb.append(body);
            sb.append("\r\n").append(mImapMessage.getMetadata().getFlags());
            return sb.toString();
        }
        
        public ImapMessage getImapMessage(){
            return mImapMessage;
        }
    }
    
    private List<Message> formatImapMessage(List<ImapMessage> imapMessages){
        List<Message> messages = new ArrayList<Message>();
        for (ImapMessage imapMessage : imapMessages) {
            messages.add(new Message(imapMessage));
        }   
        return messages;
    }

    @Override
    public void onUpdateFlagTaskExecuted(List<FlagChange> changes) {
        try {
            new ShowMessagesTask(mImapServiceController,this).run();
        } catch (ImapServiceNotAvailableException e) {
            mInProgressDialog.dismiss();
            Toast.makeText(this, getString(R.string.label_cms_toolkit_xms_sync_impossible), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }        
    }

    @Override
    public void onShowMessagesTaskExecuted(final List<ImapMessage> result) {
        if (mInProgressDialog != null) {
            mInProgressDialog.dismiss();
        }
        if(result == null){
            return;
        }
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mArrayAdapter = new ArrayAdapter<Message>(ShowMessages.this, android.R.layout.simple_list_item_1,
                        formatImapMessage(result));
                mListView.setAdapter(mArrayAdapter);
            }
        });
    }
}
