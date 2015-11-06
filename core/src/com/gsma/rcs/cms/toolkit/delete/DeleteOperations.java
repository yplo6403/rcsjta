
package com.gsma.rcs.cms.toolkit.delete;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.DeleteTask;
import com.gsma.rcs.cms.imap.task.DeleteTask.DeleteTaskListener;
import com.gsma.rcs.cms.imap.task.DeleteTask.Operation;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.PartLog;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.toolkit.AlertDialogUtils;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class DeleteOperations extends ListActivity implements DeleteTaskListener {

    private CmsSettings mSettings;
    private ImapLog mImapLog;
    private XmsLog mXmsLog;
    private PartLog mPartLog;
    private AlertDialog mInProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext(); 
        mSettings = CmsSettings.getInstance();
        mImapLog = ImapLog.getInstance(context);
        mXmsLog = XmsLog.getInstance(context);
        mPartLog = PartLog.getInstance(context);
        
        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Set items */
        String[] items = {
                getString(R.string.cms_toolkit_delete_local_storage),
                getString(R.string.cms_toolkit_delete_imap_data),
                getString(R.string.cms_toolkit_delete_rcs_messages),
                getString(R.string.cms_toolkit_delete_cms_messages),
        };
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String message;
        switch (position) {
            case 0:
                try {
                    mImapLog.removeFolders(true);
                    mXmsLog.deleteMessages();
                    mPartLog.deleteAll();
                    message = getString(R.string.cms_toolkit_result_ok);
                } catch (Exception e) {
                    e.printStackTrace();
                    message = getString(R.string.cms_toolkit_result_ko);
                }
                AlertDialogUtils.showMessage(DeleteOperations.this, message);
                break;
            case 1:
                try {
                    mImapLog.removeFolders(true);
                    message = getString(R.string.cms_toolkit_result_ok);
                } catch (Exception e) {
                    e.printStackTrace();
                    message = getString(R.string.cms_toolkit_result_ko);
                }
                AlertDialogUtils.showMessage(DeleteOperations.this, message);
                break;  
            case 2:
                try {
                    mXmsLog.deleteMessages();
                    mPartLog.deleteAll();
                    message = getString(R.string.cms_toolkit_result_ok);
                } catch (Exception e) {
                    e.printStackTrace();
                    message = getString(R.string.cms_toolkit_result_ko);
                }
                AlertDialogUtils.showMessage(DeleteOperations.this, message);
                break; 
            case 3:
                mInProgressDialog = AlertDialogUtils.displayInfo(DeleteOperations.this,
                        getString(R.string.cms_toolkit_in_progress));                
                try {
                    new DeleteTask(ImapServiceManager.getService(mSettings), Operation.DELETE_ALL, this).execute(new String[]{});
                } catch (ImapServiceNotAvailableException e) {
                    Toast.makeText(this, getString(R.string.label_cms_toolkit_xms_sync_impossible), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
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
    public void onDeleteTaskExecuted(String[] params, Boolean result) {
        mInProgressDialog.dismiss();
        String message = result ? getString(R.string.cms_toolkit_result_ok) : getString(R.string.cms_toolkit_result_ko);
        AlertDialogUtils.showMessage(this, message);        
    }
}
