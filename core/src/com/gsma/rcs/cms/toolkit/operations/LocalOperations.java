
package com.gsma.rcs.cms.toolkit.operations;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.toolkit.AlertDialogUtils;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class LocalOperations extends ListActivity {

    private ImapLog mImapLog;
    private AlertDialog mInProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalContentResolver localContentResolver = new LocalContentResolver(
                getApplicationContext());
        RcsSettings.createInstance(localContentResolver);
        mImapLog = ImapLog.getInstance(getApplicationContext());
        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Set items */
        String[] items = {
                getString(R.string.cms_toolkit_local_operations_delete_all),
                getString(R.string.cms_toolkit_local_operations_delete_folder),
                getString(R.string.cms_toolkit_local_operations_delete_message),
                "show local sms",
                "simulate sms",
        };
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                String message;
                try {
                    mImapLog.removeFolders(true);
                    message = getString(R.string.cms_toolkit_result_ok);
                } catch (Exception e) {
                    e.printStackTrace();
                    message = getString(R.string.cms_toolkit_result_ko);
                }
                AlertDialogUtils.showMessage(LocalOperations.this, message);
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
}
