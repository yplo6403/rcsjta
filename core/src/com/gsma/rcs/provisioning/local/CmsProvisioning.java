/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provisioning.local;

import static com.gsma.rcs.provisioning.local.Provisioning.saveCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveLongEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveStringEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveUriEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setLongEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setSpinnerParameter;
import static com.gsma.rcs.provisioning.local.Provisioning.setStringEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setUriEditTextParam;

import com.gsma.rcs.R;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.provider.settings.RcsSettingsData.EventReportingFrameworkConfig;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Cms parameters provisioning File
 */
public class CmsProvisioning extends Activity {

    private RcsSettings mRcsSettings;

    private String[] mEventFramework = new String[] {
            EventReportingFrameworkConfig.DISABLED.toString(),
            EventReportingFrameworkConfig.ENABLED.toString(),
            EventReportingFrameworkConfig.IMAP_ONLY.toString()
    };

    private boolean mInFront;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // Set layout
        setContentView(R.layout.rcs_provisioning_cms);
        // Set buttons callback
        Button btn = (Button) findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);
        mRcsSettings = RcsSettings.getInstance(new LocalContentResolver(this));
        updateView(bundle);
        mInFront = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        saveInstanceState(bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mInFront) {
            mInFront = true;
            // Update UI (from DB)
            updateView(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mInFront = false;
    }

    /**
     * Save parameters either in bundle or in RCS settings
     */
    private void saveInstanceState(Bundle bundle) {
        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);
        saveUriEditTextParam(R.id.message_store_url, RcsSettingsData.MESSAGE_STORE_URI, helper);
        saveStringEditTextParam(R.id.message_store_auth, RcsSettingsData.MESSAGE_STORE_AUTH, helper);
        saveStringEditTextParam(R.id.message_store_user, RcsSettingsData.MESSAGE_STORE_USER, helper);
        saveStringEditTextParam(R.id.message_store_pwd, RcsSettingsData.MESSAGE_STORE_PWD, helper);
        CmsLog cmsLog = CmsLog.getInstance();
        saveCheckBoxParam(R.id.message_store_push_sms, RcsSettingsData.MESSAGE_STORE_PUSH_SMS,
                helper);
        if (!((CheckBox) findViewById(R.id.message_store_push_sms)).isChecked()) {
            cmsLog.updatePushStatus(MessageType.SMS, PushStatus.PUSHED);
        }
        saveCheckBoxParam(R.id.message_store_push_mms, RcsSettingsData.MESSAGE_STORE_PUSH_MMS,
                helper);
        if (!((CheckBox) findViewById(R.id.message_store_push_mms)).isChecked()) {
            cmsLog.updatePushStatus(MessageType.MMS, PushStatus.PUSHED);
        }
        Spinner spinner = (Spinner) findViewById(R.id.message_store_event_framework_spinner);
        String selected = (String) spinner.getSelectedItem();
        if (bundle != null) {
            bundle.putInt(RcsSettingsData.EVENT_REPORTING_FRAMEWORK, EventReportingFrameworkConfig
                    .valueOf(selected).toInt());
        } else {
            mRcsSettings.writeInteger(RcsSettingsData.EVENT_REPORTING_FRAMEWORK,
                    EventReportingFrameworkConfig.valueOf(selected).toInt());
        }
        saveLongEditTextParam(R.id.data_connection_sync_timer,
                RcsSettingsData.DATA_CONNECTION_SYNC_TIMER, helper);
        saveLongEditTextParam(R.id.message_store_sync_timer,
                RcsSettingsData.MESSAGE_STORE_SYNC_TIMER, helper);
        saveStringEditTextParam(R.id.ntp_servers, RcsSettingsData.NTP_SERVERS, helper);
        saveLongEditTextParam(R.id.ntp_server_timeout, RcsSettingsData.NTP_SERVER_TIMEOUT, helper);
        saveLongEditTextParam(R.id.ntp_cache_validity, RcsSettingsData.NTP_CACHE_VALIDITY, helper);

    }

    /**
     * Update UI (upon creation, rotation, tab switch...)
     * 
     * @param bundle the bundle to get saved provisioning parameters
     */
    private void updateView(Bundle bundle) {
        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);
        setUriEditTextParam(R.id.message_store_url, RcsSettingsData.MESSAGE_STORE_URI, helper);
        setStringEditTextParam(R.id.message_store_auth, RcsSettingsData.MESSAGE_STORE_AUTH, helper);
        setStringEditTextParam(R.id.message_store_user, RcsSettingsData.MESSAGE_STORE_USER, helper);
        setStringEditTextParam(R.id.message_store_pwd, RcsSettingsData.MESSAGE_STORE_PWD, helper);

        setCheckBoxParam(R.id.message_store_push_sms, RcsSettingsData.MESSAGE_STORE_PUSH_SMS,
                helper);
        setCheckBoxParam(R.id.message_store_push_mms, RcsSettingsData.MESSAGE_STORE_PUSH_MMS,
                helper);
        Spinner spinner = (Spinner) findViewById(R.id.message_store_event_framework_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(CmsProvisioning.this,
                android.R.layout.simple_spinner_item, mEventFramework);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        setSpinnerParameter(spinner, RcsSettingsData.EVENT_REPORTING_FRAMEWORK, true,
                mEventFramework, helper);
        setLongEditTextParam(R.id.data_connection_sync_timer,
                RcsSettingsData.DATA_CONNECTION_SYNC_TIMER, helper);
        setLongEditTextParam(R.id.message_store_sync_timer,
                RcsSettingsData.MESSAGE_STORE_SYNC_TIMER, helper);
        setStringEditTextParam(R.id.ntp_servers, RcsSettingsData.NTP_SERVERS, helper);
        setLongEditTextParam(R.id.ntp_server_timeout, RcsSettingsData.NTP_SERVER_TIMEOUT, helper);
        setLongEditTextParam(R.id.ntp_cache_validity, RcsSettingsData.NTP_CACHE_VALIDITY, helper);
    }

    /**
     * Save button listener
     */
    private OnClickListener saveBtnListener = new OnClickListener() {
        public void onClick(View v) {
            // Save parameters
            saveInstanceState(null);
            Toast.makeText(CmsProvisioning.this, getString(R.string.message_store_save_ok_label),
                    Toast.LENGTH_LONG).show();
        }
    };
}
