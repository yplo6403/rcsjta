
package com.gsma.rcs.cms.event;

import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.services.rcs.cms.XmsMessage.State;

public interface INativeXmsEventListener {

    void onIncomingSms(SmsDataObject message);

    void onOutgoingSms(SmsDataObject message);

    void onDeleteNativeSms(long nativeProviderId);

    void onIncomingMms(MmsDataObject message);

    void onOutgoingMms(MmsDataObject message);

    void onDeleteNativeMms(String mmsId);

    void onMessageStateChanged(Long nativeProviderId, String mimeType, State state);

    void onReadNativeConversation(long nativeThreadId);

    void onDeleteNativeConversation(long nativeThreadId);

}
