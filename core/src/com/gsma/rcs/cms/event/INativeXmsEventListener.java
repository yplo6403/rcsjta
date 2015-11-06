package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.provider.xms.model.MmsData;
import com.gsma.rcs.cms.provider.xms.model.SmsData;

public interface INativeXmsEventListener {

    void onIncomingSms(SmsData message);
    void onOutgoingSms(SmsData message);
    void onDeliverNativeSms(long nativeProviderId, long sentDate);
    void onDeleteNativeSms(long nativeProviderId);

    void onIncomingMms(MmsData message);
    void onOutgoingMms(MmsData message);
    void onDeleteNativeMms(String mmsId);

    void onReadNativeConversation(long nativeThreadId);
    void onDeleteNativeConversation(long nativeThreadId);
}
