
package com.orangelabs.rcs.cms.toolkit.operations;

import com.orangelabs.rcs.cms.toolkit.protocol.service.BasicImapService;
import com.orangelabs.rcs.cms.toolkit.protocol.service.ImapServiceHandler;
import com.orangelabs.rcs.cms.toolkit.scheduler.SchedulerTask;
import com.orangelabs.rcs.cms.toolkit.scheduler.task.DeleteTask;
import com.orangelabs.rcs.cms.toolkit.scheduler.task.DeleteTask.DeleteTaskListener;
import com.orangelabs.rcs.cms.toolkit.scheduler.task.DeleteTask.Operation;

import com.orangelabs.rcs.cms.toolkit.AlertDialogUtils;
import com.orangelabs.rcs.cms.toolkit.R;
import com.orangelabs.rcs.cms.toolkit.ToolkitHandler;
import com.orangelabs.rcs.cms.toolkit.operations.remote.ShowMessages;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class RemoteOperations extends ListActivity implements DeleteTaskListener {

    private AlertDialog mInProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Set items */
        String[] items = {
                getString(R.string.cms_toolkit_remote_operations_delete_messages),
                getString(R.string.cms_toolkit_remote_operations_show_messages)
        };
        setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                ToolkitHandler.getInstance().getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        ImapServiceHandler imapServiceHandler = new ImapServiceHandler(
                                getApplicationContext());
                        BasicImapService basicImapService = imapServiceHandler.openService();
                        if (basicImapService != null) {
                            mInProgressDialog = AlertDialogUtils.displayInfo(RemoteOperations.this,
                                    getString(R.string.cms_toolkit_in_progress));
                            SchedulerTask task = new DeleteTask(Operation.DELETE_ALL, null,
                                    RemoteOperations.this);
                            task.setBasicImapService(basicImapService);
                            task.run();
                        }
                    }
                });
                break;
            case 1:
                startActivity(new Intent(this, ShowMessages.class));
                break;
        }
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

    @Override
    public void onDeleteTaskExecuted(Boolean result) {
        if (mInProgressDialog != null) {
            mInProgressDialog.dismiss();
        }
        String message = result ? getString(R.string.cms_toolkit_result_ok)
                : getString(R.string.cms_toolkit_result_ko);
        AlertDialogUtils.showMessage(this, message);
    }
}
