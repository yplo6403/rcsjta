/*
 * Copyright 2014 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gsma.rcs.core.cms.xms;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MmsSettings {

    private static MmsSettings mMmsSettings;

    private static final String MMSC_PREF = "mmsc_url";
    private static final String MMS_PROXY_PREF = "mms_proxy";
    private static final String MMS_PORT_PREF = "mms_port";

    private String mMmsc;
    private String mMmsProxy;
    private String mMmsPort;

    public static MmsSettings get(Context context) {
        return get(context, false);
    }

    public static MmsSettings get(Context context, boolean forceReload) {
        if (mMmsSettings == null || forceReload) {
            mMmsSettings = init(context);
        }
        return mMmsSettings;
    }

    private MmsSettings() {
    }

    private static MmsSettings init(Context context) {
        MmsSettings settings = new MmsSettings();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        settings.mMmsc = sharedPreferences.getString(MMSC_PREF, "");
        settings.mMmsProxy = sharedPreferences.getString(MMS_PROXY_PREF, "");
        settings.mMmsPort = sharedPreferences.getString(MMS_PORT_PREF, "");
        return settings;
    }

    public String getMmsc() {
        return mMmsc;
    }

    public String getMmsProxy() {
        return mMmsProxy;
    }

    public String getMmsPort() {
        return mMmsPort;
    }
}
