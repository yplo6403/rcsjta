package com.gsma.rcs.cms.observer;

import com.gsma.rcs.cms.event.INativeXmsEventListener;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.test.AndroidTestCase;


import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsObserverTest extends AndroidTestCase {

    private Context mContext;
    private RcsSettings mSettings;

    private ContactId contact1;
    private ContactId contact2;
    private ContactId contact3;

    private SmsDataObject incomingSms;
    private SmsDataObject outgoingSms;
    private SmsDataObject sms;
    private SmsDataObject sms2;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mSettings = RcsSettings.createInstance(new LocalContentResolver(mContext.getContentResolver()));

        contact1 = ContactUtil.getInstance(mContext).formatContact("+33600000001");
        contact2 = ContactUtil.getInstance(mContext).formatContact("+33600000002");
        contact3 = ContactUtil.getInstance(mContext).formatContact("+33600000003");

        incomingSms = new SmsDataObject("messageId1", contact1, "myContent1", Direction.INCOMING, ReadStatus.UNREAD, System.currentTimeMillis(), 1l, 1l);
        outgoingSms = new SmsDataObject("messageId2", contact2, "myContent2", Direction.OUTGOING, ReadStatus.READ, System.currentTimeMillis(), 2l, 1l);
        sms = new SmsDataObject("messageId3", contact3, "myContent3", Direction.INCOMING, ReadStatus.READ, System.currentTimeMillis(), 3l, 1l);
        sms2 = new SmsDataObject("messageId4", contact3, "myContent3", Direction.OUTGOING, ReadStatus.READ, System.currentTimeMillis(), 4l, 1l);

    }

    public void test1(){

        XmsObserver xmsObserver = new XmsObserver(mContext, mSettings);
        NativeSmsListenerMock nativeSmsListenerMock = new NativeSmsListenerMock();
        xmsObserver.registerListener(nativeSmsListenerMock);

        xmsObserver.onIncomingSms(incomingSms);
        xmsObserver.onOutgoingSms(outgoingSms);

        Assert.assertEquals(2, nativeSmsListenerMock.getMessage().size());
        Assert.assertEquals(incomingSms, nativeSmsListenerMock.getMessage().get(1l));
        Assert.assertEquals(outgoingSms, nativeSmsListenerMock.getMessage().get(2l));

        xmsObserver.onMessageStateChanged(1l, MimeType.TEXT_MESSAGE, State.DELIVERED);
        Assert.assertTrue(State.DELIVERED == nativeSmsListenerMock.getMessage().get(1l).getState());

        xmsObserver.onReadNativeConversation(1l);
        Assert.assertEquals(ReadStatus.READ, nativeSmsListenerMock.getMessage().get(1l).getReadStatus());

        xmsObserver.onDeleteNativeSms(2l);
        Assert.assertNull(nativeSmsListenerMock.getMessage().get(2l));

        xmsObserver.unregisterListener(nativeSmsListenerMock);

        xmsObserver.onIncomingSms(sms);
        Assert.assertEquals(1, nativeSmsListenerMock.getMessage().size());
        Assert.assertNull(nativeSmsListenerMock.getMessage().get(sms.getNativeProviderId()));
    }

    public void test2(){

        XmsObserver xmsObserver = new XmsObserver(mContext, mSettings);
        NativeSmsListenerMock nativeSmsListenerMock = new NativeSmsListenerMock();
        xmsObserver.registerListener(nativeSmsListenerMock);

        xmsObserver.onIncomingSms(sms);
        xmsObserver.onOutgoingSms(sms2);

        Assert.assertEquals(2, nativeSmsListenerMock.getMessages(1l).size());

        xmsObserver.onDeleteNativeConversation(1l);
        Assert.assertNull(nativeSmsListenerMock.getMessages(1l));

        xmsObserver.unregisterListener(nativeSmsListenerMock);

        xmsObserver.onIncomingSms(sms);
        Assert.assertEquals(0, nativeSmsListenerMock.getMessage().size());
    }

    private class NativeSmsListenerMock implements INativeXmsEventListener {

        private Map<Long,SmsDataObject> smsById = new HashMap<>() ;
        private Map<Long,List<SmsDataObject>> smsByThreadId = new HashMap<>();

        public NativeSmsListenerMock(){

        }

        public Map<Long,SmsDataObject> getMessage(){
            return smsById;
        }

        public List<SmsDataObject> getMessages(Long threadId){
            return smsByThreadId.get(threadId);
        }


        @Override
        public void onIncomingSms(SmsDataObject message) {
            smsById.put(message.getNativeProviderId(),message);

            List<SmsDataObject> sms = smsByThreadId.get(message.getContact());
            if(sms==null){
                sms = new ArrayList<>();
                smsByThreadId.put(message.getNativeThreadId(), sms);
            }
            sms.add(message);
        }

        @Override
        public void onOutgoingSms(SmsDataObject message) {
            smsById.put(message.getNativeProviderId(),message);

            List<SmsDataObject> sms = smsByThreadId.get(message.getNativeThreadId());
            if(sms==null){
                sms = new ArrayList<>();
                smsByThreadId.put(message.getNativeThreadId(), sms);
            }
            sms.add(message);

        }

        @Override
        public void onDeleteNativeSms(long nativeProviderId) {
            smsById.remove(nativeProviderId);
        }

        @Override
        public void onIncomingMms(MmsDataObject message) {

        }

        @Override
        public void onOutgoingMms(MmsDataObject message) {

        }

        @Override
        public void onDeleteNativeMms(String mmsId) {

        }

        @Override
        public void onMessageStateChanged(Long nativeProviderId, String mimeType, State state) {
            smsById.get(nativeProviderId).setState(state);
        }

        @Override
        public void onReadNativeConversation(long nativeThreadId) {
            for(SmsDataObject smsData : smsByThreadId.get(nativeThreadId)){
                smsData.setReadStatus(ReadStatus.READ);
            }
        }

        @Override
        public void onDeleteNativeConversation(long nativeThreadId) {
            for(SmsDataObject smsData : smsByThreadId.get(nativeThreadId)){
                smsById.remove(smsData.getNativeProviderId());
            }
            smsByThreadId.remove(nativeThreadId);
        }

    }

}
