package com.gsma.rcs.cms.observer;

import com.gsma.rcs.cms.event.INativeXmsEventListener;
import com.gsma.rcs.cms.provider.xms.model.XmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.XmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.MmsData;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.services.rcs.RcsService.Direction;

import android.content.Context;
import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsObserverTest extends AndroidTestCase {
    
    private Context mContext;
    
    private SmsData incomingSms = new SmsData(1l,1l, "myContact1", "myContent1", System.currentTimeMillis(), Direction.INCOMING, ReadStatus.UNREAD);
    private SmsData outgoingSms = new SmsData(2l,1l, "myContact2", "myContent2", System.currentTimeMillis(), Direction.OUTGOING, ReadStatus.READ);
    private SmsData sms = new SmsData(3l,1l, "myContact3", "myContent3", System.currentTimeMillis(), Direction.INCOMING, ReadStatus.READ);
    private SmsData sms2 = new SmsData(4l,1l, "myContact3", "myContent3", System.currentTimeMillis(), Direction.OUTGOING, ReadStatus.READ);
    
    protected void setUp() throws Exception {
        super.setUp();                        
        mContext = getContext();        
    }
  
    public void test1(){
        
        XmsObserver xmsObserver = XmsObserver.createInstance(mContext);
        NativeSmsListenerMock nativeSmsListenerMock = new NativeSmsListenerMock();
        xmsObserver.registerListener(nativeSmsListenerMock);
        
        xmsObserver.onIncomingSms(incomingSms);
        xmsObserver.onOutgoingSms(outgoingSms);
                        
        Assert.assertEquals(2, nativeSmsListenerMock.getMessage().size());        
        Assert.assertEquals(incomingSms, nativeSmsListenerMock.getMessage().get(1l));
        Assert.assertEquals(outgoingSms, nativeSmsListenerMock.getMessage().get(2l));
        
        Long deliveryDate = System.currentTimeMillis();
        xmsObserver.onDeliverNativeSms(1l, deliveryDate);
        Assert.assertTrue(deliveryDate == nativeSmsListenerMock.getMessage().get(1l).getDeliveryDate());
        
        xmsObserver.onReadNativeConversation(1l);
        Assert.assertEquals(ReadStatus.READ_REQUESTED, nativeSmsListenerMock.getMessage().get(1l).getReadStatus());

        xmsObserver.onDeleteNativeSms(2l);
        Assert.assertEquals(DeleteStatus.DELETED_REQUESTED, nativeSmsListenerMock.getMessage().get(2l).getDeleteStatus());

        xmsObserver.unregisterListener(nativeSmsListenerMock);
        
        xmsObserver.onIncomingSms(sms);
        Assert.assertEquals(2, nativeSmsListenerMock.getMessage().size());
        Assert.assertNull(nativeSmsListenerMock.getMessage().get(sms.getNativeProviderId()));
    }
    
    public void test2(){
        
        XmsObserver xmsObserver = XmsObserver.createInstance(mContext);
        NativeSmsListenerMock nativeSmsListenerMock = new NativeSmsListenerMock();
        xmsObserver.registerListener(nativeSmsListenerMock);
                
        xmsObserver.onIncomingSms(sms);
        xmsObserver.onOutgoingSms(sms2);
                        
        Assert.assertEquals(2, nativeSmsListenerMock.getMessages(1l).size());

        xmsObserver.onDeleteNativeConversation(1l);
        Assert.assertEquals(DeleteStatus.DELETED_REQUESTED, nativeSmsListenerMock.getMessage().get(3l).getDeleteStatus());
        Assert.assertEquals(DeleteStatus.DELETED_REQUESTED, nativeSmsListenerMock.getMessage().get(4l).getDeleteStatus());

        xmsObserver.unregisterListener(nativeSmsListenerMock);
        
        xmsObserver.onIncomingSms(sms);
        Assert.assertEquals(2, nativeSmsListenerMock.getMessage().size());
    }
    
    private class NativeSmsListenerMock implements INativeXmsEventListener {

        private Map<Long,SmsData> smsById = new HashMap<Long,SmsData>();
        private Map<Long,List<SmsData>> smsByThreadId = new HashMap<Long,List<SmsData>>();
                
        public NativeSmsListenerMock(){
            
        }
        
        public Map<Long,SmsData> getMessage(){
            return smsById;
        }
        
        public List<SmsData> getMessages(Long threadId){
            return smsByThreadId.get(threadId);
        }
        

        @Override
        public void onIncomingSms(SmsData message) {
            smsById.put(message.getNativeProviderId(),message);
            
            List<SmsData> sms = smsByThreadId.get(message.getContact());
            if(sms==null){
                sms = new ArrayList<SmsData>();
                smsByThreadId.put(message.getNativeThreadId(), sms);
            }
            sms.add(message);
        }

        @Override
        public void onOutgoingSms(SmsData message) {
            smsById.put(message.getNativeProviderId(),message);

            List<SmsData> sms = smsByThreadId.get(message.getNativeThreadId());
            if(sms==null){
                sms = new ArrayList<SmsData>();
                smsByThreadId.put(message.getNativeThreadId(), sms);
            }
            sms.add(message);

        }

        @Override
        public void onDeliverNativeSms(long nativeProviderId, long sentDate) {
            smsById.get(nativeProviderId).setDeliveryDate(sentDate);
        }

        @Override
        public void onDeleteNativeSms(long nativeProviderId) {
            smsById.get(nativeProviderId).setDeleteStatus(DeleteStatus.DELETED_REQUESTED);
        }

        @Override
        public void onIncomingMms(MmsData message) {

        }

        @Override
        public void onOutgoingMms(MmsData message) {

        }

        @Override
        public void onDeleteNativeMms(String mmsId) {

        }

        @Override
        public void onReadNativeConversation(long nativeThreadId) {
            for(SmsData smsData : smsByThreadId.get(nativeThreadId)){
                smsData.setReadStatus(ReadStatus.READ_REQUESTED);
            }
        }

        @Override
        public void onDeleteNativeConversation(long nativeThreadId) {
            for(SmsData smsData : smsByThreadId.get(nativeThreadId)){
                smsData.setDeleteStatus(DeleteStatus.DELETED_REQUESTED);
            }           
        }
        
    }

}
