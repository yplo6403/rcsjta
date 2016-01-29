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
import com.gsma.services.rcs.contact.ContactId;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.ContactUtil;

/**
 * Created by yplo6403 on 12/11/2015.
 */
public class CmsSyncContact extends RcsActivity {

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;
    private View.OnClickListener mBtnSyncListener;
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
        mCmsService = getCmsApi();
    }

    private ContactId getSelectedContact() {
        // get selected phone number
        ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
        return ContactUtil.formatContact(adapter.getSelectedNumber(mSpinner.getSelectedView()));
    }

    private void initialize() {
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
