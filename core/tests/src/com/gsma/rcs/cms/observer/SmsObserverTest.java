package com.gsma.rcs.cms.observer;

import com.gsma.rcs.cms.event.INativeSmsEventListener;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;
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
    
    private SmsData incomingSms = new SmsData(1l, "myContact1", "myContent1", System.currentTimeMillis(), Direction.INCOMING, ReadStatus.UNREAD);
    private SmsData outgoingSms = new SmsData(2l, "myContact2", "myContent2", System.currentTimeMillis(), Direction.OUTGOING, ReadStatus.READ);
    private SmsData sms = new SmsData(3l, "myContact3", "myContent3", System.currentTimeMillis(), Direction.INCOMING, ReadStatus.READ);
    private SmsData sms2 = new SmsData(4l, "myContact3", "myContent3", System.currentTimeMillis(), Direction.OUTGOING, ReadStatus.READ);
    
    protected void setUp() throws Exception {
        super.setUp();                        
        mContext = getContext();        
    }
  
    public void test1(){
        
        SmsObserver smsObserver = SmsObserver.createInstance(mContext);
        NativeSmsListenerMock nativeSmsListenerMock = new NativeSmsListenerMock();
        smsObserver.registerListener(nativeSmsListenerMock);
        
        smsObserver.onIncomingSms(incomingSms);
        smsObserver.onOutgoingSms(outgoingSms);
                        
        Assert.assertEquals(2, nativeSmsListenerMock.getMessage().size());        
        Assert.assertEquals(incomingSms, nativeSmsListenerMock.getMessage().get(1l));
        Assert.assertEquals(outgoingSms, nativeSmsListenerMock.getMessage().get(2l));
        
        Long deliveryDate = System.currentTimeMillis();
        smsObserver.onDeliverNativeSms(1l, deliveryDate);
        Assert.assertEquals(deliveryDate, nativeSmsListenerMock.getMessage().get(1l).getDeliveryDate());
        
        smsObserver.onReadNativeSms(1l);
        Assert.assertEquals(ReadStatus.READ_REQUESTED, nativeSmsListenerMock.getMessage().get(1l).getReadStatus());

        smsObserver.onDeleteNativeSms(2l);
        Assert.assertEquals(DeleteStatus.DELETED_REQUESTED, nativeSmsListenerMock.getMessage().get(2l).getDeleteStatus());

        smsObserver.unregisterListener(nativeSmsListenerMock);
        
        smsObserver.onIncomingSms(sms);
        Assert.assertEquals(2, nativeSmsListenerMock.getMessage().size());
        Assert.assertNull(nativeSmsListenerMock.getMessage().get(sms.getNativeProviderId()));
    }
    
    public void test2(){
        
        SmsObserver smsObserver = SmsObserver.createInstance(mContext);
        NativeSmsListenerMock nativeSmsListenerMock = new NativeSmsListenerMock();
        smsObserver.registerListener(nativeSmsListenerMock);
                
        smsObserver.onIncomingSms(sms);
        smsObserver.onOutgoingSms(sms2);
                        
        Assert.assertEquals(2, nativeSmsListenerMock.getMessages("myContact3").size());        

        smsObserver.onDeleteNativeConversation("myContact3");
        Assert.assertEquals(DeleteStatus.DELETED_REQUESTED, nativeSmsListenerMock.getMessage().get(3l).getDeleteStatus());
        Assert.assertEquals(DeleteStatus.DELETED_REQUESTED, nativeSmsListenerMock.getMessage().get(4l).getDeleteStatus());

        smsObserver.unregisterListener(nativeSmsListenerMock);
        
        smsObserver.onIncomingSms(sms);
        Assert.assertEquals(2, nativeSmsListenerMock.getMessage().size());
    }
    
    private class NativeSmsListenerMock implements INativeSmsEventListener {

        private Map<Long,SmsData> smsWithNativeProviderId = new HashMap<Long,SmsData>();
        private Map<String,List<SmsData>> smsWithContact = new HashMap<String,List<SmsData>>();
                
        public NativeSmsListenerMock(){
            
        }
        
        public Map<Long,SmsData> getMessage(){
            return smsWithNativeProviderId;
        }
        
        public List<SmsData> getMessages(String contact){
            return smsWithContact.get(contact);
        }
        

        @Override
        public void onIncomingSms(SmsData message) {
            smsWithNativeProviderId.put(message.getNativeProviderId(),message);
            
            List<SmsData> sms = smsWithContact.get(message.getContact());
            if(sms==null){
                sms = new ArrayList<SmsData>();
                smsWithContact.put(message.getContact(), sms);
            }
            sms.add(message);
        }

        @Override
        public void onOutgoingSms(SmsData message) {
            smsWithNativeProviderId.put(message.getNativeProviderId(),message);

            List<SmsData> sms = smsWithContact.get(message.getContact());
            if(sms==null){
                sms = new ArrayList<SmsData>();
                smsWithContact.put(message.getContact(), sms);
            }
            sms.add(message);

        }

        @Override
        public void onDeliverNativeSms(long nativeProviderId, long sentDate) {
            smsWithNativeProviderId.get(nativeProviderId).setDeliveryDate(sentDate);            
        }

        @Override
        public void onReadNativeSms(long nativeProviderId) {
            smsWithNativeProviderId.get(nativeProviderId).setReadStatus(ReadStatus.READ_REQUESTED);            
        }

        @Override
        public void onDeleteNativeSms(long nativeProviderId) {
            smsWithNativeProviderId.get(nativeProviderId).setDeleteStatus(DeleteStatus.DELETED_REQUESTED);                        
        }

        @Override
        public void onDeleteNativeConversation(String contact) {
            for(SmsData smsData : smsWithContact.get(contact)){
                smsData.setDeleteStatus(DeleteStatus.DELETED_REQUESTED);
            }           
        }
        
    }

}
