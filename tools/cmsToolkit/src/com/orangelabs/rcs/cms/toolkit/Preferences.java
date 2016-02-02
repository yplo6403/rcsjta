
package com.orangelabs.rcs.cms.toolkit;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.RcsActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Preferences extends RcsActivity {

    private static final String KEY_PREFERENCE_MESSAGE_STORE_URL = "message_store_url";
    private static final String KEY_PREFERENCE_MESSAGE_STORE_USER = "message_store_user";
    private static final String KEY_PREFERENCE_MESSAGE_STORE_PWD = "message_store_pwd";

    private static String mMsidsn;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = getApplicationContext();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.rcs_cms_toolkit_preferences);

        if (!isServiceConnected(RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        try {
            String myNumber = getContactApi().getCommonConfiguration().getMyContactId().toString();
            if (myNumber.startsWith("+")) {
                mMsidsn = myNumber.substring(1);
            }
        } catch (RcsGenericException | RcsServiceNotAvailableException e) {
            e.printStackTrace();
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }

        if (mMsidsn != null) {
            String defaultValue;
            String value;
            defaultValue = mContext.getString(R.string.cms_toolkit_pref_message_store_user_default);
            value = getMessageStoreUser(mContext);
            if (defaultValue.equals(value) || "".equals(value)) {
                setMessageStoreUser(mMsidsn);
            }
            defaultValue = mContext.getString(R.string.cms_toolkit_pref_message_store_pwd_default);
            value = getMessageStorePwd(mContext);
            if (defaultValue.equals(value) || "".equals(value)) {
                setMessageStorePwd(mMsidsn);
            }
        }

        // Set buttons callback
        Button btn = (Button) findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);
        ((EditText) findViewById(R.id.cms_toolkit_pref_message_store_url))
                .setText(getMessageStoreUrl(mContext));
        ((EditText) findViewById(R.id.cms_toolkit_pref_message_store_user))
                .setText(getMessageStoreUser(mContext));
        ((EditText) findViewById(R.id.cms_toolkit_pref_message_store_pwd))
                .setText(getMessageStorePwd(mContext));
    }

    /**
     * Save button listener
     */
    private OnClickListener saveBtnListener = new OnClickListener() {
        public void onClick(View v) {
            savePreferences();
            Toast.makeText(Preferences.this,
                    getString(R.string.cms_toolkit_pref_save_btn_ok_label), Toast.LENGTH_LONG)
                    .show();
        }
    };

    public static String getMessageStoreUrl(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getString(
                KEY_PREFERENCE_MESSAGE_STORE_URL,
                ctx.getString(R.string.cms_toolkit_pref_message_store_url_default));
    }

    private void setMessageStoreUser(String user) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Preferences.KEY_PREFERENCE_MESSAGE_STORE_USER, user.trim());
        editor.apply();
    }

    public static String getMessageStoreUser(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getString(
                KEY_PREFERENCE_MESSAGE_STORE_USER,
                ctx.getString(R.string.cms_toolkit_pref_message_store_user_default));
    }

    private void setMessageStorePwd(String pwd) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Preferences.KEY_PREFERENCE_MESSAGE_STORE_PWD, pwd.trim());
        editor.apply();
    }

    public static String getMessageStorePwd(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getString(
                KEY_PREFERENCE_MESSAGE_STORE_PWD,
                ctx.getString(R.string.cms_toolkit_pref_message_store_pwd_default));
    }

    private void savePreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Preferences.KEY_PREFERENCE_MESSAGE_STORE_URL,
                ((EditText) findViewById(R.id.cms_toolkit_pref_message_store_url)).getText()
                        .toString().trim());
        editor.putString(Preferences.KEY_PREFERENCE_MESSAGE_STORE_USER,
                ((EditText) findViewById(R.id.cms_toolkit_pref_message_store_user)).getText()
                        .toString().trim());
        editor.putString(Preferences.KEY_PREFERENCE_MESSAGE_STORE_PWD,
                ((EditText) findViewById(R.id.cms_toolkit_pref_message_store_pwd)).getText()
                        .toString().trim());
        editor.apply();
    }

}
