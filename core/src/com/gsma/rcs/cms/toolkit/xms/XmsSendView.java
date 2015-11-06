
package com.gsma.rcs.cms.toolkit.xms;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.toolkit.AlertDialogUtils;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class XmsSendView extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rcs_cms_toolkit_xms_send_view);

        Button sendbutton= (Button) findViewById(R.id.rcs_cms_toolkit_xms_sendBtn);
        sendbutton.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                
                // check contact 
                String contact = ((EditText) findViewById(R.id.rcs_cms_toolkit_xms_send_contact)).getText().toString();
                
                PhoneNumber phoneNumber = ContactUtil
                        .getValidPhoneNumberFromAndroid(contact);
                if(phoneNumber == null){
                    AlertDialogUtils.showMessage(XmsSendView.this,getString(R.string.cms_toolkit_xms_contact_bad_format));
                    return;
                }
                                
                contact = ContactUtil.createContactIdFromValidatedData(phoneNumber).toString();
                String content = ((EditText) findViewById(R.id.rcs_cms_toolkit_xms_send_content)).getText().toString();
                if(content.isEmpty()){
                    return;
                }
                new SmsSender(getApplicationContext(), contact, content).send();     
                //AlertDialogUtils.showMessage(XmsSendView.this, getString(R.string.cms_toolkit_send_message_sent));
                startActivity(XmsConversationView.forgeIntentToStart(XmsSendView.this, contact));
            }
            
        });
    }    
}
