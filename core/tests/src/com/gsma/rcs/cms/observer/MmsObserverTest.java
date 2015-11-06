package com.gsma.rcs.cms.observer;

import android.content.Context;
import android.test.AndroidTestCase;

import com.gsma.rcs.cms.event.INativeXmsEventListener;
import com.gsma.rcs.cms.provider.xms.model.XmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.XmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.MmsData;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.services.rcs.RcsService.Direction;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class MmsObserverTest extends AndroidTestCase {
    
    private Context mContext;

    private TreeSet<String> contact1 = new TreeSet<>();
    private TreeSet<String> contact3 = new TreeSet<>();

    private MmsData incomingMms = new MmsData(1l,1l,"1",contact1 ,null,"textContent", System.currentTimeMillis(), Direction.INCOMING, ReadStatus.READ);
    private MmsData outgoingMms = new MmsData(2l,1l,"2",contact1 ,null,"textContent", System.currentTimeMillis(), Direction.OUTGOING, ReadStatus.READ);

    private MmsData mms = new MmsData(3l,1l,"3", contact1, null, "myContent3", System.currentTimeMillis(), Direction.INCOMING, ReadStatus.READ);
    private MmsData mms2 = new MmsData(4l,1l,"4", contact1, null, "myContent3", System.currentTimeMillis(), Direction.OUTGOING, ReadStatus.READ);
    
    protected void setUp() throws Exception {
        super.setUp();                        
        mContext = getContext();

        contact1 = new TreeSet<>();
        contact1.add("myContact1");

        contact3 = new TreeSet<>();
        contact3.add("myContact3");

    }
  
    public void test1(){
        
        XmsObserver xmsObserver = XmsObserver.createInstance(mContext);
        NativeMmsListenerMock nativeSmsListenerMock = new NativeMmsListenerMock();
        xmsObserver.registerListener(nativeSmsListenerMock);
        
        xmsObserver.onIncomingMms(incomingMms);
        xmsObserver.onOutgoingMms(outgoingMms);
                        
        Assert.assertEquals(2, nativeSmsListenerMock.getMessage().size());        
        Assert.assertEquals(incomingMms, nativeSmsListenerMock.getMessage().get(1l));
        Assert.assertEquals(outgoingMms, nativeSmsListenerMock.getMessage().get(2l));
        
        Long deliveryDate = System.currentTimeMillis();
        xmsObserver.onDeliverNativeSms(1l, deliveryDate);
        Assert.assertTrue(deliveryDate == nativeSmsListenerMock.getMessage().get(1l).getDeliveryDate());
        
        xmsObserver.onReadNativeConversation(1l);
        Assert.assertEquals(ReadStatus.READ_REQUESTED, nativeSmsListenerMock.getMessage().get(1l).getReadStatus());

        xmsObserver.onDeleteNativeSms(2l);
        Assert.assertEquals(DeleteStatus.DELETED_REQUESTED, nativeSmsListenerMock.getMessage().get(2l).getDeleteStatus());

        xmsObserver.unregisterListener(nativeSmsListenerMock);
        
        xmsObserver.onIncomingMms(mms);
        Assert.assertEquals(2, nativeSmsListenerMock.getMessage().size());
        Assert.assertNull(nativeSmsListenerMock.getMessage().get(mms.getNativeProviderId()));
    }
    
    public void test2(){
        
        XmsObserver xmsObserver = XmsObserver.createInstance(mContext);
        NativeMmsListenerMock nativeMmsListenerMock = new NativeMmsListenerMock();
        xmsObserver.registerListener(nativeMmsListenerMock);
                
        xmsObserver.onIncomingMms(mms);
        xmsObserver.onOutgoingMms(mms2);
                        
        Assert.assertEquals(2, nativeMmsListenerMock.getMessages(1l).size());

        xmsObserver.onDeleteNativeConversation(1l);
        Assert.assertEquals(DeleteStatus.DELETED_REQUESTED, nativeMmsListenerMock.getMessage().get(3l).getDeleteStatus());
        Assert.assertEquals(DeleteStatus.DELETED_REQUESTED, nativeMmsListenerMock.getMessage().get(4l).getDeleteStatus());

        xmsObserver.unregisterListener(nativeMmsListenerMock);
        
        xmsObserver.onIncomingMms(mms);
        Assert.assertEquals(2, nativeMmsListenerMock.getMessage().size());
    }
    
    private class NativeMmsListenerMock implements INativeXmsEventListener {

        private Map<Long,MmsData> messagesById = new HashMap<Long,MmsData>();
        private Map<Long,List<MmsData>> messagesByThreadId = new HashMap<Long,List<MmsData>>();
                
        public NativeMmsListenerMock(){
            
        }
        
        public Map<Long,MmsData> getMessage(){
            return messagesById;
        }
        
        public List<MmsData> getMessages(Long threadId){
            return messagesByThreadId.get(threadId);
        }
        

        @Override
        public void onIncomingSms(SmsData message) {}

        @Override
        public void onOutgoingSms(SmsData message) {}

        @Override
        public void onDeliverNativeSms(long nativeProviderId, long sentDate) {}

        @Override
        public void onDeleteNativeSms(long nativeProviderId) {}

        @Override
        public void onIncomingMms(MmsData message) {

            messagesById.put(message.getNativeProviderId(), message);

            List<MmsData> sms = messagesByThreadId.get(message.getContact());
            if(sms==null){
                sms = new ArrayList<MmsData>();
                messagesByThreadId.put(message.getNativeThreadId(), sms);
            }
            sms.add(message);
        }

        @Override
        public void onOutgoingMms(MmsData message) {

            messagesById.put(message.getNativeProviderId(), message);

            List<MmsData> sms = messagesByThreadId.get(message.getNativeThreadId());
            if(sms==null){
                sms = new ArrayList<MmsData>();
                messagesByThreadId.put(message.getNativeThreadId(), sms);
            }
            sms.add(message);

        }

        @Override
        public void onDeleteNativeMms(String mmsId) {

        }

        @Override
        public void onReadNativeConversation(long nativeThreadId) {
            for(MmsData mmsData : messagesByThreadId.get(nativeThreadId)){
                mmsData.setReadStatus(ReadStatus.READ_REQUESTED);
            }
        }

        @Override
        public void onDeleteNativeConversation(long nativeThreadId) {
            for(MmsData mmsData : messagesByThreadId.get(nativeThreadId)){
                mmsData.setDeleteStatus(DeleteStatus.DELETED_REQUESTED);
            }           
        }
        
    }

}
