
package com.orangelabs.rcs.cms.toolkit.delete;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.RcsActivity;

import com.orangelabs.rcs.cms.toolkit.AlertDialogUtils;
import com.orangelabs.rcs.cms.toolkit.R;
import com.orangelabs.rcs.cms.toolkit.scheduler.task.DeleteTask.DeleteTaskListener;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class DeleteOperations extends RcsActivity implements DeleteTaskListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Set items */
        String[] items = {
                getString(R.string.cms_toolkit_delete_local_storage),
                getString(R.string.cms_toolkit_delete_imap_data),
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
                }
            }
        });
    }

    @Override
    public void onDeleteTaskExecuted(Boolean result) {
        String message = result ? getString(R.string.cms_toolkit_result_ok)
                : getString(R.string.cms_toolkit_result_ko);
        AlertDialogUtils.showMessage(this, message);
    }
}
