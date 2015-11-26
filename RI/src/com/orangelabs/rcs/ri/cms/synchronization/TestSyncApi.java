/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.ri.cms.synchronization;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.cms.CmsService;
import com.gsma.services.rcs.cms.CmsSynchronizationListener;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsListActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Created by yplo6403 on 10/11/2015.
 */
public class TestSyncApi extends RcsListActivity {

    private CmsSynchronizationListener mCmsSyncListener;

    private CmsService mCmsService;

    private Handler mHandler = new Handler();

    private static final String LOGTAG = LogUtils.getTag(TestSyncApi.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        String[] items = {
                getString(R.string.menu_cms_sync_all),
                getString(R.string.menu_cms_sync_one_to_one),
                getString(R.string.menu_cms_sync_group),
        };
        setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
        /* Register to API connection manager */
        if (!isServiceConnected(ConnectionManager.RcsServiceName.CMS)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(ConnectionManager.RcsServiceName.CMS);
        try {
            mCmsService = getCmsApi();
            mCmsService.addEventListener(mCmsSyncListener);
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCmsService != null && isServiceConnected(ConnectionManager.RcsServiceName.CMS)) {
            /* Remove CMS synchronization listener */
            try {
                mCmsService.removeEventListener(mCmsSyncListener);
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    private void initialize() {
        mCmsSyncListener = new CmsSynchronizationListener() {
            @Override
            public void onAllSynchronized() {
                mHandler.post(new Runnable() {
                    public void run() {
                        Utils.displayLongToast(TestSyncApi.this,
                                getString(R.string.cms_sync_completed));
                    }
                });
            }

            @Override
            public void onOneToOneConversationSynchronized(ContactId contact) {
                /* Here we only consider full CMS sync */
            }

            @Override
            public void onGroupConversationSynchronized(String chatId) {
                /* Here we only consider full CMS sync */
            }
        };
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                try {
                    mCmsService.syncAll();
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
                break;

            case 1:
                startActivity(new Intent(this, CmsSyncContact.class));
                break;

            case 2:
                startActivity(new Intent(this, CmsSyncGroup.class));
                break;
        }
    }
}
