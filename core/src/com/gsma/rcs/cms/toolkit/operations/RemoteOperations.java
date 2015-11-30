
package com.gsma.rcs.cms.toolkit.operations;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.toolkit.AlertDialogUtils;
import com.gsma.rcs.cms.toolkit.operations.remote.CreateMessages;
import com.gsma.rcs.cms.toolkit.operations.remote.ShowMessages;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.ImapService;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;

public class RemoteOperations extends ListActivity {

    private RcsSettings mSettings;

    private AlertDialog mInProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = RcsSettings.createInstance(new LocalContentResolver(getApplicationContext()));

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Set items */
        String[] items = {
                getString(R.string.cms_toolkit_remote_operations_delete_messages),
                getString(R.string.cms_toolkit_remote_operations_create_messages),
                getString(R.string.cms_toolkit_remote_operations_show_messages)
        };
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                try {
                    new DeleteTask(RemoteOperations.this).execute(new String[0]);
                    mInProgressDialog = AlertDialogUtils.displayInfo(RemoteOperations.this,
                            getString(R.string.cms_toolkit_in_progress));

                } catch (ImapServiceNotAvailableException e) {
                    e.printStackTrace();
                    Toast.makeText(this, getString(R.string.label_cms_toolkit_xms_sync_impossible), Toast.LENGTH_LONG).show();
                }
                break;
            case 1:
                startActivity(new Intent(this, CreateMessages.class));
                break;
            case 2:
                startActivity(new Intent(this, ShowMessages.class));
                break;
        }
    }

    public class DeleteTask extends AsyncTask<String, String, Boolean> {

        private Context mContext;
        private ImapService mImapService;

        /**
         * @param ctx
         * @throws ImapServiceNotAvailableException 
         */
        public DeleteTask(Context ctx) throws ImapServiceNotAvailableException {            
            mContext = ctx;
            mImapService = ImapServiceManager.getService(mSettings);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {                
                mImapService.init();
                boolean res = deleteExistingMessages((BasicImapService) mImapService);
                ImapServiceManager.releaseService(mImapService);
                return res;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mInProgressDialog.dismiss();
            String message = result ? getString(R.string.cms_toolkit_result_ok)
                    : getString(R.string.cms_toolkit_result_ko);
            AlertDialogUtils.showMessage(mContext, message);
        }

        /**
         * @param imap
         * @return boolean
         */
        public boolean deleteExistingMessages(BasicImapService imap) {
            try {
                for (ImapFolder imapFolder : imap.listStatus()) {
                    imap.delete(imapFolder.getName());
                }
                ;
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ImapException e) {
                e.printStackTrace();
            }
            return false;
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
