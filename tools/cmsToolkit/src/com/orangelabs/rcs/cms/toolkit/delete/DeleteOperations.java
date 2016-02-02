
package com.orangelabs.rcs.cms.toolkit.delete;

import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.sync.scheduler.SchedulerTask;
import com.gsma.rcs.core.cms.sync.scheduler.task.DeleteTask;
import com.gsma.rcs.core.cms.sync.scheduler.task.DeleteTask.DeleteTaskListener;
import com.gsma.rcs.core.cms.sync.scheduler.task.DeleteTask.Operation;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.cms.toolkit.AlertDialogUtils;
import com.orangelabs.rcs.cms.toolkit.R;
import com.orangelabs.rcs.cms.toolkit.ToolkitHandler;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class DeleteOperations extends RcsActivity implements DeleteTaskListener {

    private AlertDialog mInProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Set items */
        String[] items = {
                getString(R.string.cms_toolkit_delete_local_storage),
                getString(R.string.cms_toolkit_delete_imap_data),
                getString(R.string.cms_toolkit_delete_xms_messages),
                getString(R.string.cms_toolkit_delete_chat_messages),
                getString(R.string.cms_toolkit_delete_cms_messages),
        };

        ListView listView = new ListView(this);
        setContentView(listView);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
        listView.setOnItemClickListener(new OnItemClickListener() {
            String message;

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                switch (position) {
                    case 0:
                        try {
                            if (!isServiceConnected(RcsServiceName.CMS, RcsServiceName.CHAT)) {
                                showMessage(R.string.label_service_not_available);
                                break;
                            }
                            getCmsApi().deleteImapData();
                            getCmsApi().deleteXmsMessages();
                            getChatApi().deleteOneToOneChats();
                            getChatApi().deleteGroupChats();
                            message = getString(R.string.cms_toolkit_result_ok);
                        } catch (Exception e) {
                            e.printStackTrace();
                            message = getString(R.string.cms_toolkit_result_ko);
                        }
                        AlertDialogUtils.showMessage(DeleteOperations.this, message);
                        break;
                    case 1:
                        try {
                            if (!isServiceConnected(RcsServiceName.CMS)) {
                                showMessage(R.string.label_service_not_available);
                                break;
                            }
                            getCmsApi().deleteImapData();
                            message = getString(R.string.cms_toolkit_result_ok);
                        } catch (Exception e) {
                            e.printStackTrace();
                            message = getString(R.string.cms_toolkit_result_ko);
                        }
                        AlertDialogUtils.showMessage(DeleteOperations.this, message);
                        break;
                    case 2:
                        try {
                            if (!isServiceConnected(RcsServiceName.CMS)) {
                                showMessage(R.string.label_service_not_available);
                                break;
                            }
                            getCmsApi().deleteXmsMessages();
                            message = getString(R.string.cms_toolkit_result_ok);
                        } catch (Exception e) {
                            e.printStackTrace();
                            message = getString(R.string.cms_toolkit_result_ko);
                        }
                        AlertDialogUtils.showMessage(DeleteOperations.this, message);
                        break;
                    case 3:
                        try {
                            if (!isServiceConnected(RcsServiceName.CHAT)) {
                                showMessage(R.string.label_service_not_available);
                                break;
                            }
                            getChatApi().deleteOneToOneChats();
                            getChatApi().deleteGroupChats();
                            message = getString(R.string.cms_toolkit_result_ok);
                        } catch (Exception e) {
                            e.printStackTrace();
                            message = getString(R.string.cms_toolkit_result_ko);
                        }
                        AlertDialogUtils.showMessage(DeleteOperations.this, message);
                        break;
                    case 4:
                        ToolkitHandler.getInstance().getHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                ImapServiceHandler imapServiceHandler = new ImapServiceHandler(
                                        getApplicationContext());
                                BasicImapService basicImapService = imapServiceHandler
                                        .openService();
                                if (basicImapService != null) {
                                    mInProgressDialog = AlertDialogUtils.displayInfo(
                                            DeleteOperations.this,
                                            getString(R.string.cms_toolkit_in_progress));
                                    SchedulerTask task = new DeleteTask(Operation.DELETE_ALL, null,
                                            DeleteOperations.this);
                                    task.setBasicImapService(basicImapService);
                                    task.run();
                                }
                            }
                        });
                        break;
                }
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
