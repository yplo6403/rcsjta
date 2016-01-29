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

package com.gsma.rcs.core.cms.toolkit;

import com.gsma.rcs.R;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.cms.toolkit.delete.DeleteOperations;
import com.gsma.rcs.core.cms.toolkit.operations.RemoteOperations;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.utils.logger.Logger;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Toolkit extends ListActivity {

    private static final Logger sLogger = Logger.getLogger(Toolkit.class.getSimpleName());

    private static RcsSettings mRcsSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mRcsSettings = RcsSettings.getInstance(new LocalContentResolver(getApplicationContext()));
        super.onCreate(savedInstanceState);

        if (checkCore(this) == null) {
            return;
        }

        checkSettings();

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Set items */
        String[] items = {
                getString(R.string.menu_cms_toolkit_remote_operations),
                getString(R.string.menu_cms_toolkit_delete),
        };
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, items);
        setListAdapter(arrayAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                if (!checkSettings()) {
                    return;
                }
                startActivity(new Intent(this, RemoteOperations.class));
                break;

            case 1:
                if (!checkSettings()) {
                    return;
                }
                startActivity(new Intent(this, DeleteOperations.class));
                break;
        }
    }

    private boolean checkSettings() {

        String defaultCmsServerAddress = RcsSettingsData.DEFAULT_MESSAGE_STORE_URL;
        String defaultLogin = RcsSettingsData.DEFAULT_MESSAGE_STORE_USER;
        String defaultPwd = RcsSettingsData.DEFAULT_MESSAGE_STORE_PWD;

        if (mRcsSettings.getMessageStoreUrl().equals(defaultCmsServerAddress)
                || mRcsSettings.getMessageStoreUser().equals(defaultLogin)
                || mRcsSettings.getMessageStorePwd().equals(defaultPwd)) {
            AlertDialogUtils.showMessage(this,
                    getString(R.string.cms_toolkit_settings_set_provisoning));
            return false;
        }
        return true;
    }

    public static Core checkCore(Context context) {
        Core core = Core.getInstance();
        if (core == null) {
            AlertDialogUtils.showMessage(context,
                    "You have to start the RCS stack before using the Toolkit");
        }
        return core;
    }

}
