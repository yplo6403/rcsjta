
package com.gsma.rcs.cms.toolkit.operations.remote;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;
import com.gsma.rcs.cms.toolkit.AlertDialogUtils;
import com.gsma.rcs.cms.toolkit.operations.remote.PushMessageTask.PushMessageTaskCallback;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.services.rcs.RcsService.Direction;

import com.sonymobile.rcs.imap.Flag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CreateMessages extends Activity implements PushMessageTaskCallback{

    private CmsSettings mSettings;
    private AlertDialog mInProgressDialog;

    private Spinner mDirectionSpinner;
    private Spinner mNumberSpinner;
        
    private AlertDialog mSelectFlagsAlertDialog;
    private String[] mFlagsItems;
    private boolean[] mCheckedFlags;
    private List<Flag> mSelectedFlags = new ArrayList<Flag>();
    
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Set layout
        setContentView(R.layout.rcs_cms_toolkit_create_messages);

        // Set buttons callback
        Button btn = (Button) findViewById(R.id.cms_toolkit_create_btn);
        btn.setOnClickListener(saveBtnListener);
        mSettings = CmsSettings.getInstance();

        mDirectionSpinner = (Spinner) findViewById(R.id.cms_toolkit_create_message_direction);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.cms_toolkit_create_message_spinner_direction,
                android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mDirectionSpinner.setAdapter(adapter);

        mNumberSpinner = (Spinner) findViewById(R.id.cms_toolkit_create_message_number);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.cms_toolkit_create_message_spinner_number,
                android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mNumberSpinner.setAdapter(adapter);

        mFlagsItems = getResources().getStringArray(R.array.cms_toolkit_create_message_flags);
        mCheckedFlags = new boolean[mFlagsItems.length];
        ((Button) findViewById(R.id.cms_toolkit_create_message_select_flags_button)).setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(CreateMessages.this);
                builder.setTitle(R.string.cms_toolkit_create_message_select_flags_label);
                builder.setMultiChoiceItems(mFlagsItems,
                        mCheckedFlags,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                mCheckedFlags[which] = isChecked;
                            }
                        });
                builder.setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mSelectFlagsAlertDialog.dismiss();                        
                    }
                });
                mSelectFlagsAlertDialog = builder.show();
                mSelectFlagsAlertDialog.setOnDismissListener(new OnDismissListener(){
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mSelectedFlags = getSelectedFlags();
                        TextView tv =  (TextView)findViewById(R.id.cms_toolkit_create_message_selected_flags);
                        StringBuilder textToDisplay = new StringBuilder(getResources().getString(R.string.cms_toolkit_create_message_selected_flags_label));
                        textToDisplay.append(mSelectedFlags);
                        tv.setText(textToDisplay);
                    }                   
                });

            }});        
    }

    /**
     * Save button listener
     */
    private OnClickListener saveBtnListener = new OnClickListener() {
        public void onClick(View v) {
            String from = ((EditText) findViewById(R.id.cms_toolkit_create_message_from)).getText()
                    .toString();
            String to = ((EditText) findViewById(R.id.cms_toolkit_create_message_to)).getText()
                    .toString();
            String content = ((EditText) findViewById(R.id.cms_toolkit_create_message_content))
                    .getText().toString();            
            String directionStr = (String) mDirectionSpinner.getSelectedItem();
            
            Direction direction = Direction.OUTGOING;
            String contact = to;
            String myNumber = from;
            if("Incoming".equals(directionStr)){
                direction = Direction.INCOMING;
                contact = from;
                myNumber = to;
            }
            
            Integer number = Integer.valueOf((String)mNumberSpinner.getSelectedItem());            
            SmsData sms = new  SmsData(null, contact, content, System.currentTimeMillis(), direction, ReadStatus.READ_REQUESTED);
            SmsData[] messages = new SmsData[number];
            for(int i=0;i<number;i++){
                messages[i] = sms;
            }
            new PushMessageTask(mSettings, messages, myNumber, mSelectedFlags, CreateMessages.this).execute();
            mInProgressDialog = AlertDialogUtils.displayInfo(CreateMessages.this,
                    getString(R.string.cms_toolkit_in_progress));
        }
    };

    
    private List<Flag> getSelectedFlags(){
        List<Flag> selectedFlags = new ArrayList<Flag>();
        for(int i=0;i<mCheckedFlags.length;i++){
            if(mCheckedFlags[i]){
                if("Seen".equals(mFlagsItems[i])){
                    selectedFlags.add(Flag.Seen);
                }
                else if("Deleted".equals(mFlagsItems[i])){
                    selectedFlags.add(Flag.Deleted);
                } 
            }
        }
        return selectedFlags;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mInProgressDialog != null) {
            mInProgressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInProgressDialog != null) {
            mInProgressDialog.dismiss();
        }
    }

    @Override
    public void onPushMessageTaskCallbackExecuted(List<String> result) {
        mInProgressDialog.dismiss();
        String message;
        if (result.isEmpty()) {
            message = getString(R.string.cms_toolkit_result_ko);
        } else {
            message = new StringBuilder(getString(R.string.cms_toolkit_result_ok))
                    .append("\n uid : ").append(result).toString();
        }
        AlertDialogUtils.showMessage(this, message);
    }
}
