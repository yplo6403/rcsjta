package com.gsma.rcs.cms.toolkit.xms;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.telephony.gsm.SmsManager;

public class SmsSender {
    
    private Context mContext;
    private String mContact;
    private String mContent;
    
    public SmsSender(Context context,  String contact, String content){
        mContext = context;
        mContact = contact;
        mContent = content;       
    }
    
    public void send(){
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(mContact, null, mContent, null, null);   
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            ContentValues values = new ContentValues();
            values.put("address", mContact);
            values.put("body", mContent);
            mContext.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
        }
        
    }

}
