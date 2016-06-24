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

package com.gsma.rcs.provisioning.local;

import com.gsma.rcs.R;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.provider.settings.RcsSettingsData.EventReportingFrameworkConfig;
import com.gsma.rcs.utils.logger.Logger;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

/**
 * Cms parameters provisioning File
 */
public class CmsProvisioning extends Fragment implements IProvisioningFragment {

    private static final Logger sLogger = Logger.getLogger(StackProvisioning.class.getName());

    private static RcsSettings sRcsSettings;

    private static final Uri CONTENT_URI = Uri.parse("content://com.gsma.rcs.cms.imap/message");
    private static final String KEY_PUSH_STATUS = "pushStatus";
    private static final String SEL_TYPE = "msgType=?";

    private String[] mEventFramework = new String[] {
            EventReportingFrameworkConfig.DISABLED.toString(),
            EventReportingFrameworkConfig.ENABLED.toString(),
            EventReportingFrameworkConfig.IMAP_ONLY.toString()
    };

    private View mRootView;
    private ProvisioningHelper mHelper;
    private LocalContentResolver mLocalContentResolver;

    public static CmsProvisioning newInstance(RcsSettings rcsSettings) {
        if (sLogger.isActivated()) {
            sLogger.debug("new instance");
        }
        CmsProvisioning f = new CmsProvisioning();
        /*
         * If Android decides to recreate your Fragment later, it's going to call the no-argument
         * constructor of your fragment. So overloading the constructor is not a solution. A way to
         * pass argument to new fragment is to store it as static.
         */
        sRcsSettings = rcsSettings;
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.provisioning_cms, container, false);
        mHelper = new ProvisioningHelper(mRootView, sRcsSettings);
        mLocalContentResolver = new LocalContentResolver(getContext().getContentResolver());
        displayRcsSettings();
        return mRootView;
    }

    @Override
    public void displayRcsSettings() {
        if (sLogger.isActivated()) {
            sLogger.debug("displayRcsSettings");
        }
        mHelper.setUriEditText(R.id.message_store_url, RcsSettingsData.MESSAGE_STORE_URI);
        mHelper.setStringEditText(R.id.message_store_auth, RcsSettingsData.MESSAGE_STORE_AUTH);
        mHelper.setStringEditText(R.id.message_store_user, RcsSettingsData.MESSAGE_STORE_USER);
        mHelper.setStringEditText(R.id.message_store_pwd, RcsSettingsData.MESSAGE_STORE_PWD);

        mHelper.setBoolCheckBox(R.id.message_store_push_sms, RcsSettingsData.MESSAGE_STORE_PUSH_SMS);
        mHelper.setBoolCheckBox(R.id.message_store_push_mms, RcsSettingsData.MESSAGE_STORE_PUSH_MMS);
        Spinner spinner = (Spinner) mRootView
                .findViewById(R.id.message_store_event_framework_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, mEventFramework);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        mHelper.setSpinnerParameter(spinner, RcsSettingsData.EVENT_REPORTING_FRAMEWORK, true,
                mEventFramework);
        mHelper.setLongEditText(R.id.data_connection_sync_timer,
                RcsSettingsData.DATA_CONNECTION_SYNC_TIMER);
        mHelper.setLongEditText(R.id.message_store_sync_timer,
                RcsSettingsData.MESSAGE_STORE_SYNC_TIMER);
        mHelper.setStringEditText(R.id.ntp_servers, RcsSettingsData.NTP_SERVERS);
        mHelper.setLongEditText(R.id.ntp_server_timeout, RcsSettingsData.NTP_SERVER_TIMEOUT);
        mHelper.setLongEditText(R.id.ntp_cache_validity, RcsSettingsData.NTP_CACHE_VALIDITY);
    }

    @Override
    public void persistRcsSettings() {
        if (sLogger.isActivated()) {
            sLogger.debug("persistRcsSettings");
        }
        mHelper.saveUriEditText(R.id.message_store_url, RcsSettingsData.MESSAGE_STORE_URI);
        mHelper.saveStringEditText(R.id.message_store_auth, RcsSettingsData.MESSAGE_STORE_AUTH);
        mHelper.saveStringEditText(R.id.message_store_user, RcsSettingsData.MESSAGE_STORE_USER);
        mHelper.saveStringEditText(R.id.message_store_pwd, RcsSettingsData.MESSAGE_STORE_PWD);
        mHelper.saveBoolCheckBox(R.id.message_store_push_sms,
                RcsSettingsData.MESSAGE_STORE_PUSH_SMS);
        if (!((CheckBox) mRootView.findViewById(R.id.message_store_push_sms)).isChecked()) {
            updatePushStatus(MessageType.SMS, PushStatus.PUSHED);
        }
        mHelper.saveBoolCheckBox(R.id.message_store_push_mms,
                RcsSettingsData.MESSAGE_STORE_PUSH_MMS);
        if (!((CheckBox) mRootView.findViewById(R.id.message_store_push_mms)).isChecked()) {
            updatePushStatus(MessageType.MMS, PushStatus.PUSHED);
        }
        Spinner spinner = (Spinner) mRootView
                .findViewById(R.id.message_store_event_framework_spinner);
        String selected = (String) spinner.getSelectedItem();
        sRcsSettings.writeInteger(RcsSettingsData.EVENT_REPORTING_FRAMEWORK,
                EventReportingFrameworkConfig.valueOf(selected).toInt());
        mHelper.saveLongEditText(R.id.data_connection_sync_timer,
                RcsSettingsData.DATA_CONNECTION_SYNC_TIMER);
        mHelper.saveLongEditText(R.id.message_store_sync_timer,
                RcsSettingsData.MESSAGE_STORE_SYNC_TIMER);
        mHelper.saveStringEditText(R.id.ntp_servers, RcsSettingsData.NTP_SERVERS);
        mHelper.saveLongEditText(R.id.ntp_server_timeout, RcsSettingsData.NTP_SERVER_TIMEOUT);
        mHelper.saveLongEditText(R.id.ntp_cache_validity, RcsSettingsData.NTP_CACHE_VALIDITY);
    }

    /**
     * Updates push status by messageType
     *
     * @param messageType the type
     * @param pushStatus the push status
     */
    public void updatePushStatus(MessageType messageType, PushStatus pushStatus) {
        ContentValues values = new ContentValues();
        values.put(KEY_PUSH_STATUS, pushStatus.toInt());
        mLocalContentResolver.update(CONTENT_URI, values, SEL_TYPE, new String[] {
            messageType.toString()
        });
    }
}
