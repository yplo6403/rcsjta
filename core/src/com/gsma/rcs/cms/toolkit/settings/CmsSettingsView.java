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

package com.gsma.rcs.cms.toolkit.settings;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.settings.CmsSettingsData;
import com.gsma.rcs.provider.LocalContentResolver;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Cms parameters provisioning File
 */
public class CmsSettingsView extends Activity {

    private CmsSettings mSettings;

    private boolean isInFront;
    @Override
    public void onCreate(Bundle bundle) {
        Context context = getApplicationContext();
        
        super.onCreate(bundle);

        // Set layout
        setContentView(R.layout.rcs_cms_toolkit_settings);

        // Set buttons callback
        Button btn = (Button) findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);
        mSettings = CmsSettings.createInstance(context);
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
        saveEditTextParam(R.id.cms_rcs_message_folder, CmsSettingsData.CMS_RCS_MESSAGE_FOLDER);
        saveEditTextParam(R.id.cms_imap_server_address, CmsSettingsData.CMS_IMAP_SERVER_ADDRESS);
        saveEditTextParam(R.id.cms_imap_user_login, CmsSettingsData.CMS_IMAP_USER_LOGIN);
        saveEditTextParam(R.id.cms_imap_user_pwd, CmsSettingsData.CMS_IMAP_USER_PWD);
        saveEditTextParam(R.id.cms_toolkit_settings_myNumber, CmsSettingsData.CMS_MY_NUMBER);
    }

    /**
     * Update UI (upon creation, rotation, tab switch...)
     * 
     * @param bundle
     */
    private void updateView(Bundle bundle) {
        // Display cms parameters
        setEditTextParam(R.id.cms_rcs_message_folder, CmsSettingsData.CMS_RCS_MESSAGE_FOLDER);
        setEditTextParam(R.id.cms_imap_server_address, CmsSettingsData.CMS_IMAP_SERVER_ADDRESS);
        setEditTextParam(R.id.cms_imap_user_login, CmsSettingsData.CMS_IMAP_USER_LOGIN);
        setEditTextParam(R.id.cms_imap_user_pwd, CmsSettingsData.CMS_IMAP_USER_PWD);
        setEditTextParam(R.id.cms_toolkit_settings_myNumber, CmsSettingsData.CMS_MY_NUMBER);
    }

    /**
     * Save button listener
     */
    private OnClickListener saveBtnListener = new OnClickListener() {
        public void onClick(View v) {
            // Save parameters
            saveInstanceState(null);
            Toast.makeText(CmsSettingsView.this, getString(R.string.cms_toolkit_settings_save_ok), Toast.LENGTH_LONG).show();
        }
    };

    /* package private */void saveEditTextParam(int viewID, String settingsKey) {
        EditText txt = (EditText) findViewById(viewID);
        mSettings.writeParameter(settingsKey, txt.getText().toString());

    }

    /**
     * Set edit text either from bundle or from RCS settings if bundle is null
     * 
     * @param viewID the view ID for the text edit
     * @param settingsKey the key of the RCS parameter
     * @param helper
     */
    /* package private */ void setEditTextParam(int viewID, String settingsKey) {
        String parameter = mSettings.readParameter(settingsKey);
        EditText editText = (EditText) findViewById(viewID);
        editText.setText(parameter);
    }
}
