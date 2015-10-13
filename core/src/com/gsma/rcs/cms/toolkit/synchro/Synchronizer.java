
package com.gsma.rcs.cms.toolkit.synchro;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.BasicSynchronizationTask;
import com.gsma.rcs.cms.imap.task.BasicSynchronizationTask.BasicSynchronizationTaskListener;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.toolkit.AlertDialogUtils;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class Synchronizer extends ListActivity implements BasicSynchronizationTaskListener {

    private AlertDialog mInProgressDialog;
    private LocalStorage mLocalStorageHandler;
    private CmsSettings mSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Set items */
        String[] items = {
                getString(R.string.cms_toolkit_synchronizer_start_basic)
        };
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
        mLocalStorageHandler = LocalStorage.createInstance(ImapLog.getInstance(getApplicationContext()));
        mSettings = CmsSettings.getInstance();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                try {
                    BasicSynchronizationTask task = new BasicSynchronizationTask(ImapServiceManager.getService(mSettings), mLocalStorageHandler,this);
                    task.execute(new String[0]);
                    mInProgressDialog = AlertDialogUtils.displayInfo(Synchronizer.this,
                            getString(R.string.cms_toolkit_in_progress));
                } catch (ImapServiceNotAvailableException e) {
                    e.printStackTrace();
                    Toast.makeText(this, getString(R.string.label_cms_toolkit_xms_sync_impossible), Toast.LENGTH_LONG).show();
                }
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
    public void onBasicSynchronizationTaskExecuted(String[] params, Boolean result) {
        mInProgressDialog.dismiss();
        String message = result ? getString(R.string.cms_toolkit_result_ok) : getString(R.string.cms_toolkit_result_ko);
        AlertDialogUtils.showMessage(this, message);
    }
}
