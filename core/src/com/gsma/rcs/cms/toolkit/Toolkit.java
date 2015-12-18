
package com.gsma.rcs.cms.toolkit;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.toolkit.delete.DeleteOperations;
import com.gsma.rcs.cms.toolkit.operations.RemoteOperations;
import com.gsma.rcs.cms.toolkit.xms.XmsList;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.utils.logger.Logger;

public class Toolkit extends ListActivity {

    private static final Logger sLogger = Logger.getLogger(Toolkit.class.getSimpleName());

    private RcsSettings mRcsSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mRcsSettings = RcsSettings.createInstance(new LocalContentResolver(getApplicationContext()));
        super.onCreate(savedInstanceState);

        if(checkCore(this) == null){
            return;
        }

        checkSettings();
                
        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Set items */
        String[] items = {
                getString(R.string.menu_cms_toolkit_remote_operations),
                getString(R.string.menu_cms_toolkit_delete),
                getString(R.string.menu_cms_toolkit_sms),
        };
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
        setListAdapter(arrayAdapter);        
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                if(!checkSettings()){
                    return;
                }
                startActivity(new Intent(this, RemoteOperations.class));
                break;

            case 1:
                if(!checkSettings()){
                    return;
                }
                startActivity(new Intent(this, DeleteOperations.class));
                break;

            case 2:
                if(!checkSettings()){
                    return;
                }
                startActivity(new Intent(this, XmsList.class));
                break;
        }
    }

    private boolean checkSettings() {

        String defaultCmsServerAddress = RcsSettingsData.DEFAULT_CMS_IMAP_SERVER_ADDRESS;
        String defaultLogin = RcsSettingsData.DEFAULT_CMS_IMAP_USER_LOGIN;
        String defaultPwd = RcsSettingsData.DEFAULT_CMS_IMAP_USER_PWD;

        if(mRcsSettings.getCmsServerAddress().equals(defaultCmsServerAddress) ||
            mRcsSettings.getCmsUserLogin().equals(defaultLogin) ||
            mRcsSettings.getCmsUserPwd().equals(defaultPwd)
                ) {
            AlertDialogUtils.showMessage(this, getString(R.string.cms_toolkit_settings_set_provisoning));
            return false;
        }
        return true;
    }

    public static Core checkCore(Context context){
        Core core = Core.getInstance();
        if(core == null){
            AlertDialogUtils.showMessage(context, "You have to start the RCS stack before using the Toolkit");
        }
        return core;
    }

    public static boolean checkCore(Context context, Core core ){
        if(core != Core.getInstance()){
            AlertDialogUtils.showMessage(context, "You have to restart the Toolkit");
            return false;
        }
        return true;
    }
}
