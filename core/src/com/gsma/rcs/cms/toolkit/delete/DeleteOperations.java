/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.gsma.rcs.cms.toolkit.delete;
import com.gsma.rcs.R;
import com.gsma.rcs.cms.imap.task.DeleteTask;
import com.gsma.rcs.cms.imap.task.DeleteTask.DeleteTaskListener;
import com.gsma.rcs.cms.imap.task.DeleteTask.Operation;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.scheduler.CmsScheduler;
import com.gsma.rcs.cms.toolkit.AlertDialogUtils;
import com.gsma.rcs.cms.toolkit.Toolkit;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class DeleteOperations extends ListActivity implements DeleteTaskListener {

    private RcsSettings mSettings;
    private ImapLog mImapLog;
    private XmsLog mXmsLog;
    private AlertDialog mInProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Toolkit.checkCore(this) == null) {
            return;
        }
        Context context = getApplicationContext();
        mSettings = RcsSettings.createInstance(new LocalContentResolver(context));
        mImapLog = ImapLog.getInstance();
        mXmsLog = XmsLog.getInstance();

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
                    mXmsLog.deleteAllEntries();
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
                    mXmsLog.deleteAllEntries();
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
                CmsScheduler scheduler = Core.getInstance().getCmsService().getCmsManager()
                        .getSyncScheduler();
                scheduler.scheduleToolkitTask(new DeleteTask(Operation.DELETE_ALL, null, this));
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
        mInProgressDialog.dismiss();
        String message = result ? getString(R.string.cms_toolkit_result_ok)
                : getString(R.string.cms_toolkit_result_ko);
        AlertDialogUtils.showMessage(this, message);
    }
}
