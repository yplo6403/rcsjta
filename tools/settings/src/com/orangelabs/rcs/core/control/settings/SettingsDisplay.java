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

package com.orangelabs.rcs.core.control.settings;

import com.gsma.services.rcs.CommonServiceConfiguration;
import com.gsma.services.rcs.CommonServiceConfiguration.MinimumBatteryLevel;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;

import com.orangelabs.rcs.core.control.CoreControlApplication;
import com.orangelabs.rcs.core.control.R;
import com.orangelabs.rcs.core.control.utils.MessageUtils;
import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

/**
 * Settings display
 * 
 * @author Jean-Marc AUFFRET
 */
@SuppressWarnings("deprecation")
public class SettingsDisplay extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    private final static int SERVICE_DEACTIVATION_CONFIRMATION_DIALOG = 1;

    private static final String LOGTAG = "[Core-ctrl][" + SettingsDisplay.class.getSimpleName()
            + "]";

    /**
     * API connection manager
     */
    private ConnectionManager mCnxManager;

    private RcsServiceControl mRcsServiceControl;

    /**
     * Service flag
     */
    private CheckBoxPreference mRcsActivationCheckbox;

    /**
     * Battery level
     */
    private ListPreference mBatteryLevel;

    private RcsServiceListener mRcsServiceListener = new RcsServiceListener() {

        @Override
        public void onServiceConnected() {
            try {
                enablePreferences(true);
                initCheckbox(mRcsActivationCheckbox, true,
                        mRcsServiceControl.isActivationModeChangeable());
                initBatteryLevel(mCnxManager.getFileTransferApi().getCommonConfiguration());
            } catch (RcsServiceException e) {
                Log.e(LOGTAG, "Failed to read configuration", e);
            }
        }

        @Override
        public void onServiceDisconnected(ReasonCode reasonCode) {
            boolean changeable;
            try {
                changeable = mRcsServiceControl.isActivationModeChangeable();
            } catch (RcsServiceException e) {
                changeable = true;
            }
            enablePreferences(false);
            initCheckbox(mRcsActivationCheckbox, false, changeable);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rcs_settings_preferences);

        mRcsActivationCheckbox = (CheckBoxPreference) getPreferenceScreen().findPreference(
                "rcs_activation");
        mBatteryLevel = (ListPreference) findPreference("min_battery_level");

        mRcsServiceControl = CoreControlApplication.getRcsServiceControl();
        mCnxManager = ConnectionManager.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOGTAG, "onResume " + mCnxManager);
        mCnxManager.startMonitorApiCnx(this, mRcsServiceListener, RcsServiceName.FILE_TRANSFER,
                RcsServiceName.CHAT);

        if (!mRcsServiceControl.isAvailable()) {
            initCheckbox(mRcsActivationCheckbox, false, false);
            enablePreferences(false);
            MessageUtils.showMessage(this, getString(R.string.label_service_not_available));
            return;
        }

        try {
            boolean isServiceActivated = mRcsServiceControl.isActivated();
            boolean isChangeable = mRcsServiceControl.isActivationModeChangeable();
            boolean isServiceConnected = mCnxManager
                    .isServiceConnected(RcsServiceName.FILE_TRANSFER);
            initCheckbox(mRcsActivationCheckbox, (isServiceActivated), isChangeable);
            enablePreferences(isServiceActivated && isServiceConnected);
            if (isServiceConnected) {
                initBatteryLevel(mCnxManager.getFileTransferApi().getCommonConfiguration());
            }
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to read activation state", e);
            enablePreferences(false);
            MessageUtils.showMessage(this, getString(R.string.label_api_failed));
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCnxManager.stopMonitorApiCnx(this);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        try {
            if (preference == mRcsActivationCheckbox) {
                if (mRcsActivationCheckbox.isChecked()) {
                    try {
                        mRcsServiceControl.setActivationMode(true);
                    } catch (RcsPermissionDeniedException e) {
                        MessageUtils.showMessage(SettingsDisplay.this,
                                getString(R.string.text_service_activate_unchangeable));
                    }
                } else {
                    if (mRcsServiceControl.isActivated()) {
                        showDialog(SERVICE_DEACTIVATION_CONFIRMATION_DIALOG);
                    }
                }
                return true;
            } else {
                return super.onPreferenceTreeClick(preferenceScreen, preference);
            }
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed onPreferenceTreeClick", e);
            MessageUtils.showMessage(this, getString(R.string.label_api_failed));
            return true;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case SERVICE_DEACTIVATION_CONFIRMATION_DIALOG:
                return new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle(R.string.rcs_settings_label_confirm)
                        .setMessage(R.string.rcs_settings_label_rcs_service_shutdown)
                        .setNegativeButton(R.string.rcs_settings_label_cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int button) {
                                        mRcsActivationCheckbox.setChecked(!mRcsActivationCheckbox
                                                .isChecked());
                                    }
                                })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                mRcsActivationCheckbox.setChecked(!mRcsActivationCheckbox
                                        .isChecked());
                            }
                        })
                        .setPositiveButton(R.string.rcs_settings_label_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int button) {
                                        /* Stop running service */
                                        enablePreferences(false);
                                        try {
                                            mRcsServiceControl.setActivationMode(false);
                                        } catch (RcsPermissionDeniedException e) {
                                            MessageUtils
                                                    .showMessage(
                                                            SettingsDisplay.this,
                                                            getString(R.string.text_service_activate_unchangeable));
                                        } catch (RcsGenericException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).setCancelable(true).create();
        }
        return super.onCreateDialog(id);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if ("min_battery_level".equals(preference.getKey())) {
            try {
                int level = Integer.parseInt((String) objValue);
                CommonServiceConfiguration configuration = mCnxManager.getFileTransferApi()
                        .getCommonConfiguration();
                configuration.setMinimumBatteryLevel(MinimumBatteryLevel.valueOf(level));
            } catch (Exception e) {
                Log.e(LOGTAG, "Failed to read battery level", e);
                MessageUtils.showMessage(this, getString(R.string.label_api_failed));
            }
        }
        return true;
    }

    /**
     * Initialize the service activation checkbox
     * 
     * @param checked
     * @param enabled
     */
    private void initCheckbox(CheckBoxPreference checkbox, boolean checked, boolean enabled) {
        checkbox.setChecked(checked);
        checkbox.setEnabled(enabled);
    }

    /**
     * Enable / disable preferences
     * 
     * @param enabled
     */
    private void enablePreferences(boolean enabled) {
        findPreference("min_battery_level").setEnabled(enabled);
        findPreference("userprofile_settings").setEnabled(enabled);
        findPreference("messaging_settings").setEnabled(enabled);
    }

    /**
     * Initialize battery level from configuration
     * 
     * @param configuration
     * @throws RcsServiceException
     */
    private void initBatteryLevel(CommonServiceConfiguration configuration)
            throws RcsServiceException {
        mBatteryLevel.setPersistent(false);
        mBatteryLevel.setOnPreferenceChangeListener(this);
        mBatteryLevel.setValue(String.valueOf(configuration.getMinimumBatteryLevel().toInt()));
    }

}
