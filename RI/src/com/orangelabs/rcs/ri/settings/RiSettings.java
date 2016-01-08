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

package com.orangelabs.rcs.ri.settings;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.cms.CmsService;

import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsPreferenceActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by yplo6403 on 05/01/2016.
 */
public class RiSettings extends RcsPreferenceActivity {

    private static final String KEY_PREFERENCE_RESEND_CHAT = "resend_chat";

    private static final String KEY_PREFERENCE_RESEND_FT = "resend_ft";

    private static final String KEY_PREFERENCE_SYNC_MODE = "sync_mode";

    private static CmsService sCmsService;

    private static final String LOGTAG = LogUtils.getTag(RiSettings.class.getSimpleName());

    /**
     * The RCS undelivered message resend preference.
     */
    public enum PreferenceResendRcs {
        /**
         * Send message to XMS
         */
        SEND_TO_XMS(0),
        /**
         * Do nothing
         */
        DO_NOTHING(1),
        /**
         * Always ask
         */
        ALWAYS_ASK(2);

        private final int mValue;

        PreferenceResendRcs(int value) {
            mValue = value;
        }

        /**
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isServiceConnected(ConnectionManager.RcsServiceName.CMS)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        sCmsService = getCmsApi();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new RiSettingsPreferenceFragment()).commit();
    }

    public static class RiSettingsPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            try {
                if (sCmsService.isAllowedToSendMultimediaMessage()) {
                    addPreferencesFromResource(R.xml.ri_preferences);
                } else {
                    addPreferencesFromResource(R.xml.ri_preferences_without_mms);
                }
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
                addPreferencesFromResource(R.xml.ri_preferences_without_mms);
            }
        }
    }

    public static boolean isSyncAutomatic(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return "0".equals(sp.getString(KEY_PREFERENCE_SYNC_MODE, "1"));
    }

    public static PreferenceResendRcs getPreferenceResendChat(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String resendChat = sp.getString(KEY_PREFERENCE_RESEND_CHAT, "2");
        switch (resendChat) {
            case "0":
                return PreferenceResendRcs.SEND_TO_XMS;
            case "2":
                return PreferenceResendRcs.ALWAYS_ASK;
            default:
                return PreferenceResendRcs.DO_NOTHING;
        }
    }

    public static PreferenceResendRcs getPreferenceResendFileTransfer(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String resendChat = sp.getString(KEY_PREFERENCE_RESEND_FT, "2");
        switch (resendChat) {
            case "0":
                return PreferenceResendRcs.SEND_TO_XMS;
            case "2":
                return PreferenceResendRcs.ALWAYS_ASK;
            default:
                return PreferenceResendRcs.DO_NOTHING;
        }
    }

    public static void savePreferenceResendChat(Context ctx, PreferenceResendRcs preference) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(RiSettings.KEY_PREFERENCE_RESEND_CHAT, Integer.valueOf(preference.toInt())
                .toString());
        editor.apply();
    }

    public static void savePreferenceResendFileTransfer(Context ctx, PreferenceResendRcs preference) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(RiSettings.KEY_PREFERENCE_RESEND_FT, Integer.valueOf(preference.toInt())
                .toString());
        editor.apply();
    }
}
