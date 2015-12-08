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
import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by yplo6403 on 12/11/2015.
 */
public class CmsSyncContact extends RcsActivity {

    private final static SimpleDateFormat sDateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss",
            Locale.getDefault());
    private static final String LOGTAG = LogUtils.getTag(CmsSyncContact.class.getSimpleName());
    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;
    private View.OnClickListener mBtnSyncListener;
    private CmsSynchronizationListener mCmsSyncListener;
    private Handler mHandler = new Handler();
    private CmsService mCmsService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.cms_sync_contact);

        /* Set the contact selector */
        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createContactListAdapter(this,
                getString(R.string.label_history_log_contact_spinner_default_value)));

        /* Set button callback */
        Button syncBtn = (Button) findViewById(R.id.sync_btn);
        syncBtn.setOnClickListener(mBtnSyncListener);

        /* Update refresh button */
        if (mSpinner.getAdapter().getCount() == 0) {
            // Disable button if no contact available
            syncBtn.setEnabled(false);
        } else {
            syncBtn.setEnabled(true);
        }

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

    /**
     * Returns the selected contact
     *
     * @return Contact
     */
    private ContactId getSelectedContact() {
        // get selected phone number
        ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
        return ContactUtil.formatContact(adapter.getSelectedNumber(mSpinner.getSelectedView()));
    }

    private void initialize() {
        mCmsSyncListener = new CmsSynchronizationListener() {
            @Override
            public void onAllSynchronized() {
            }

            @Override
            public void onOneToOneConversationSynchronized(final ContactId contact) {
                mHandler.post(new Runnable() {
                    public void run() {
                        Utils.displayLongToast(CmsSyncContact.this,
                                getString(R.string.cms_sync_contact_completed, contact.toString()));
                        TextView timestamp = (TextView) findViewById(R.id.timestamp);
                        timestamp.setText(sDateFormat.format(System.currentTimeMillis()));
                    }
                });
            }

            @Override
            public void onGroupConversationSynchronized(String chatId) {
            }
        };

        mBtnSyncListener = new View.OnClickListener() {
            public void onClick(View v) {
                ContactId contact = getSelectedContact();
                if (contact != null) {
                    try {
                        mCmsService.syncOneToOneConversation(contact);

                    } catch (RcsServiceException e) {
                        showExceptionThenExit(e);
                    }
                }
            }
        };

    }

}
