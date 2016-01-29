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

package com.gsma.rcs.core.cms.toolkit.operations;

import com.gsma.rcs.R;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.sync.scheduler.Scheduler;
import com.gsma.rcs.core.cms.sync.scheduler.SchedulerTask;
import com.gsma.rcs.core.cms.toolkit.AlertDialogUtils;
import com.gsma.rcs.core.cms.toolkit.Toolkit;
import com.gsma.rcs.core.cms.toolkit.operations.remote.ShowMessages;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;

import com.sonymobile.rcs.imap.ImapException;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;

public class RemoteOperations extends ListActivity {

    private RcsSettings mSettings;

    private AlertDialog mInProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Toolkit.checkCore(this) == null) {
            return;
        }
        mSettings = RcsSettings.getInstance(new LocalContentResolver(getApplicationContext()));

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Set items */
        String[] items = {
                getString(R.string.cms_toolkit_remote_operations_delete_messages),
                getString(R.string.cms_toolkit_remote_operations_show_messages)
        };
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                Scheduler scheduler = Core.getInstance().getCmsService().getCmsManager()
                        .getSyncScheduler();
                scheduler.scheduleToolkitTask(new DeleteTask(RemoteOperations.this));
                mInProgressDialog = AlertDialogUtils.displayInfo(RemoteOperations.this,
                        getString(R.string.cms_toolkit_in_progress));
                break;
            case 1:
                startActivity(new Intent(this, ShowMessages.class));
                break;
        }
    }

    public class DeleteTask extends SchedulerTask {

        private Context mContext;

        /**
         * @param ctx
         */
        public DeleteTask(Context ctx) {
            mContext = ctx;
        }

        @Override
        public void run() {
            boolean result;
            try {
                getBasicImapService().init();
                result = deleteExistingMessages(getBasicImapService());
            } catch (Exception e) {
                result = false;
            }

            if (mInProgressDialog != null) {
                mInProgressDialog.dismiss();
            }
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
                for (String imapFolder : imap.list()) {
                    imap.delete(imapFolder);
                }
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
