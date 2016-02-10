
package com.orangelabs.rcs.cms.toolkit.operations.remote;

import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.sync.scheduler.SchedulerTask;
import com.gsma.rcs.core.cms.sync.scheduler.task.ShowMessagesTask;
import com.gsma.rcs.core.cms.sync.scheduler.task.ShowMessagesTask.ShowMessagesTaskListener;

import com.orangelabs.rcs.cms.toolkit.AlertDialogUtils;
import com.orangelabs.rcs.cms.toolkit.R;
import com.orangelabs.rcs.cms.toolkit.ToolkitHandler;
import com.gsma.rcs.imaplib.imap.ImapMessage;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ShowMessages extends ListActivity implements ShowMessagesTaskListener {

    private ListView mListView;
    private ArrayAdapter<Message> mArrayAdapter;
    private AlertDialog mInProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.rcs_cms_toolkit_messages_list);

        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setEmptyView(emptyView);
        registerForContextMenu(mListView);
        mArrayAdapter = new ArrayAdapter<>(ShowMessages.this,
                android.R.layout.simple_list_item_1,new ArrayList<Message>());
        mListView.setAdapter(mArrayAdapter);

        mInProgressDialog = AlertDialogUtils.displayInfo(ShowMessages.this,
                getString(R.string.cms_toolkit_in_progress));
        ToolkitHandler.getInstance().getHandler().post(new Runnable() {
            @Override
            public void run() {

                ImapServiceHandler imapServiceHandler = new ImapServiceHandler(
                        getApplicationContext());
                BasicImapService basicImapService = imapServiceHandler.openService();
                if (basicImapService == null) {
                    mInProgressDialog.dismiss();
                    return;
                }
                SchedulerTask task = new ShowMessagesTask(ShowMessages.this);
                task.setBasicImapService(basicImapService);
                task.run();
            }
        });
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
            body = mImapMessage.getTextBody();
            sb.append(body);
            sb.append("\r\n").append(mImapMessage.getMetadata().getFlags());
            return sb.toString();
        }

        public ImapMessage getImapMessage() {
            return mImapMessage;
        }
    }

    private List<Message> formatImapMessage(List<ImapMessage> imapMessages) {
        List<Message> messages = new ArrayList<Message>();
        for (ImapMessage imapMessage : imapMessages) {
            messages.add(new Message(imapMessage));
        }
        return messages;
    }

    @Override
    public void onShowMessagesTaskExecuted(final List<ImapMessage> result) {
        if (mInProgressDialog != null) {
            mInProgressDialog.dismiss();
        }
        if (result == null) {
            return;
        }
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mArrayAdapter = new ArrayAdapter<Message>(ShowMessages.this,
                        android.R.layout.simple_list_item_1, formatImapMessage(result));
                mListView.setAdapter(mArrayAdapter);
            }
        });
    }
}
