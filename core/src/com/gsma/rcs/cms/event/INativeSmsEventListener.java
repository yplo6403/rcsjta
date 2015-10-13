package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.provider.xms.model.SmsData;

import android.telephony.SmsMessage;
import android.telephony.SmsMessage;;

public interface INativeSmsEventListener extends Comparable<INativeSmsEventListener>{
    
    public static final int PRIORITY_HIGH = 100;
    public static final int PRIORITY_MEDIUM = 50;
    public static final int PRIORITY_LOW = 0;
    
    public int getPriority();
    public void onIncomingSms(SmsData message);    
    public void onOutgoingSms(SmsData message);
    public void onDeliverNativeSms(long nativeProviderId, long sentDate);
    public void onReadNativeSms(long nativeProviderId);
    public void onDeleteNativeSms(long nativeProviderId);
    public void onDeleteNativeConversation(String contact);
    public int compareTo(INativeSmsEventListener another);
}
