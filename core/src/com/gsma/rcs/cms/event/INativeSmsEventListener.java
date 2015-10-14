package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.provider.xms.model.SmsData;

public interface INativeSmsEventListener {

    public void onIncomingSms(SmsData message);    
    public void onOutgoingSms(SmsData message);
    public void onDeliverNativeSms(long nativeProviderId, long sentDate);
    public void onReadNativeSms(long nativeProviderId);
    public void onDeleteNativeSms(long nativeProviderId);
    public void onDeleteNativeConversation(String contact);
}
