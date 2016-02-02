
package com.orangelabs.rcs.cms.toolkit;

import com.orangelabs.rcs.cms.toolkit.delete.DeleteOperations;
import com.orangelabs.rcs.cms.toolkit.operations.RemoteOperations;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Toolkit extends ListActivity {

    private static Context ctx;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        ctx = getApplicationContext();
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Set items */
        String[] items = {
                getString(R.string.menu_cms_toolkit_pref),
                getString(R.string.menu_cms_toolkit_remote_operations),
                getString(R.string.menu_cms_toolkit_delete)
        };
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, items);
        setListAdapter(arrayAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                startActivity(new Intent(this, Preferences.class));
                break;
            case 1:
                startActivity(new Intent(this, RemoteOperations.class));
                break;
            case 2:
                startActivity(new Intent(this, DeleteOperations.class));
                break;
        }
    }

    public static Context getAppContext() {
        return ctx;
    }
}
