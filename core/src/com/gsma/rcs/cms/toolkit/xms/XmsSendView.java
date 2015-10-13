
package com.gsma.rcs.cms.toolkit.xms;

import com.gsma.rcs.R;
import com.gsma.rcs.cms.CmsService;
import com.gsma.rcs.cms.event.INativeSmsEventListener;
import com.gsma.rcs.cms.observer.SmsObserver;
import com.gsma.rcs.cms.provider.xms.XmsData;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.toolkit.AlertDialogUtils;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.services.rcs.RcsService.Direction;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.gsm.SmsManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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
                    AlertDialogUtils.displayInfo(XmsSendView.this,getString(R.string.cms_toolkit_xms_contact_bad_format));
                    return;
                }
                                
                contact = ContactUtil.createContactIdFromValidatedData(phoneNumber).toString();
                String content = ((EditText) findViewById(R.id.rcs_cms_toolkit_xms_send_content)).getText().toString();
                new SmsSender(getApplicationContext(), contact, content).send();     
                //AlertDialogUtils.showMessage(XmsSendView.this, getString(R.string.cms_toolkit_send_message_sent));
                startActivity(XmsConversationView.forgeIntentToStart(XmsSendView.this, contact));
            }
            
        });
    }    
}
