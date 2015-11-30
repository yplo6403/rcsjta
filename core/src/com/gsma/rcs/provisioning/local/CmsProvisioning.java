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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;

import static com.gsma.rcs.provisioning.local.Provisioning.saveCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveStringEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setStringEditTextParam;

/**
 * Cms parameters provisioning File
 */
public class CmsProvisioning extends Activity {

    private RcsSettings mRcsSettings;

    private boolean isInFront;
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Set layout
        setContentView(R.layout.rcs_provisioning_cms);

        // Set buttons callback
        Button btn = (Button) findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);
        mRcsSettings = RcsSettings.createInstance(new LocalContentResolver(this));
        updateView(bundle);
        isInFront = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        saveInstanceState(bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isInFront == false) {
            isInFront = true;
            // Update UI (from DB)
            updateView(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isInFront = false;
    }

    /**
     * Save parameters either in bundle or in RCS settings
     */
    private void saveInstanceState(Bundle bundle) {

        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);

        saveStringEditTextParam(R.id.cms_imap_server_address, RcsSettingsData.CMS_IMAP_SERVER_ADDRESS, helper);
        saveStringEditTextParam(R.id.cms_imap_user_login, RcsSettingsData.CMS_IMAP_USER_LOGIN, helper);
        saveStringEditTextParam(R.id.cms_imap_user_pwd, RcsSettingsData.CMS_IMAP_USER_PWD, helper);

        ImapLog imapLog = ImapLog.getInstance();
        saveCheckBoxParam(R.id.cms_push_sms, RcsSettingsData.CMS_PUSH_SMS, helper);
        if(!((CheckBox)findViewById(R.id.cms_push_sms)).isChecked()){
            imapLog.updatePushStatus(MessageType.SMS, PushStatus.PUSHED);
        }
        saveCheckBoxParam(R.id.cms_push_mms, RcsSettingsData.CMS_PUSH_MMS, helper);
        if(!((CheckBox)findViewById(R.id.cms_push_mms)).isChecked()){
            imapLog.updatePushStatus(MessageType.MMS, PushStatus.PUSHED);
        }
        saveCheckBoxParam(R.id.cms_update_flag_imap_xms, RcsSettingsData.CMS_UPDATE_FLAGS_WITH_IMAP_XMS, helper);

        saveStringEditTextParam(R.id.cms_default_directory_name, RcsSettingsData.CMS_DEFAULT_DIRECTORY_NAME, helper);
        saveStringEditTextParam(R.id.cms_default_directory_separator, RcsSettingsData.CMS_DIRECTORY_SEPARATOR, helper);
    }

    /**
     * Update UI (upon creation, rotation, tab switch...)
     * 
     * @param bundle
     */
    private void updateView(Bundle bundle) {
        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);

        setStringEditTextParam(R.id.cms_imap_server_address, RcsSettingsData.CMS_IMAP_SERVER_ADDRESS, helper);
        setStringEditTextParam(R.id.cms_imap_user_login, RcsSettingsData.CMS_IMAP_USER_LOGIN, helper);
        setStringEditTextParam(R.id.cms_imap_user_pwd, RcsSettingsData.CMS_IMAP_USER_PWD, helper);

        setCheckBoxParam(R.id.cms_push_sms, RcsSettingsData.CMS_PUSH_SMS, helper);
        setCheckBoxParam(R.id.cms_push_mms, RcsSettingsData.CMS_PUSH_MMS, helper);
        setCheckBoxParam(R.id.cms_update_flag_imap_xms, RcsSettingsData.CMS_UPDATE_FLAGS_WITH_IMAP_XMS, helper);

        setStringEditTextParam(R.id.cms_default_directory_name, RcsSettingsData.CMS_DEFAULT_DIRECTORY_NAME, helper);
        setStringEditTextParam(R.id.cms_default_directory_separator, RcsSettingsData.CMS_DIRECTORY_SEPARATOR, helper);
    }

    /**
     * Save button listener
     */
    private OnClickListener saveBtnListener = new OnClickListener() {
        public void onClick(View v) {
            // Save parameters
            saveInstanceState(null);
            Toast.makeText(CmsProvisioning.this, getString(R.string.cms_save_ok), Toast.LENGTH_LONG).show();
        }
    };
}
