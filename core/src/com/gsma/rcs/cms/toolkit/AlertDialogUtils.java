
package com.gsma.rcs.cms.toolkit;

import com.gsma.rcs.R;

import android.app.AlertDialog;
import android.content.Context;

public class AlertDialogUtils {

    public static AlertDialog showMessage(Context context, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton(context.getString(R.string.label_ok), null);
        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }

    public static AlertDialog displayInfo(Context context, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(msg);
        builder.setCancelable(false);
        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }
}
