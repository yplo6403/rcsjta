
package com.gsma.rcs.cms.toolkit;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.CmsService;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.PartLog;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.sync.adapter.CmsAccountException;
import com.gsma.rcs.cms.sync.adapter.CmsAccountManager;
import com.gsma.rcs.cms.toolkit.delete.DeleteOperations;
import com.gsma.rcs.cms.toolkit.operations.RemoteOperations;
import com.gsma.rcs.cms.toolkit.settings.CmsSettingsView;
import com.gsma.rcs.cms.toolkit.synchro.Synchronizer;
import com.gsma.rcs.cms.toolkit.xms.SmsImportAsyncTask;
import com.gsma.rcs.cms.toolkit.xms.SmsImportAsyncTask.ImportTaskListener;
import com.gsma.rcs.cms.toolkit.xms.XmsList;
import com.gsma.rcs.utils.logger.Logger;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Toolkit extends ListActivity implements ImportTaskListener {

    private static final Logger sLogger = Logger.getLogger(Toolkit.class.getSimpleName());

    private AlertDialog mInProgressDialog; 
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        try {
            CmsAccountManager.createInstance(getApplicationContext());
        } catch (CmsAccountException e) {
            sLogger.error("Can not create android cms account", e);
            e.printStackTrace();
        }

        CmsService.createInstance(getApplicationContext());
        checkSettings();
                
        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Set items */
        String[] items = {
                getString(R.string.menu_cms_toolkit_settings),
                getString(R.string.menu_cms_toolkit_remote_operations),
                getString(R.string.menu_cms_toolkit_delete),
                getString(R.string.menu_cms_toolkit_synchronizer),
                getString(R.string.menu_cms_toolkit_sms),
                getString(R.string.menu_cms_toolkit_import),
        };
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
        setListAdapter(arrayAdapter);        
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                startActivity(new Intent(this, CmsSettingsView.class));
                break;

            case 1:
                if(!checkSettings()){
                    return;
                }
                startActivity(new Intent(this, RemoteOperations.class));
                break;

            case 2:
                if(!checkSettings()){
                    return;
                }
                startActivity(new Intent(this, DeleteOperations.class));
                break;

            case 3:
                if(!checkSettings()){
                    return;
                }
                startActivity(new Intent(this, Synchronizer.class));
                break;
                
            case 4:
                if(!checkSettings()){
                    return;
                }
                startActivity(new Intent(this, XmsList.class));
                break;
                
            case 5:
                mInProgressDialog = AlertDialogUtils.displayInfo(Toolkit.this,
                        getString(R.string.cms_toolkit_in_progress));
                Context context = getApplicationContext();
                new SmsImportAsyncTask(context, XmsLog.getInstance(context), PartLog.getInstance(context), this).execute();
                break;                
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mInProgressDialog!=null){
            mInProgressDialog.dismiss();
        }
    };
        
    private boolean checkSettings() {
        boolean res = CmsSettings.getInstance().isEmpty();
        if(res){
            AlertDialogUtils.showMessage(this,
                    getString(R.string.cms_toolkit_settings_set_provisoning));
        }
        return !res;
    }

    @Override
    public void onImportTaskExecuted(Boolean result) {
        if(mInProgressDialog!=null){
            mInProgressDialog.dismiss();
        }
    }
}
