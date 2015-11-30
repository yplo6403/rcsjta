//package com.gsma.rcs.cms.observer;
//
//import android.content.Context;
//import android.test.AndroidTestCase;
//
//import com.gsma.rcs.cms.event.INativeXmsEventListener;
//import com.gsma.rcs.cms.provider.settings.CmsSettings;
//import com.gsma.rcs.cms.provider.xms.model.XmsData.ReadStatus;
//import com.gsma.rcs.cms.provider.xms.model.MmsData;
//import com.gsma.rcs.cms.provider.xms.model.SmsData;
//import com.gsma.services.rcs.RcsService.Direction;
//
//import junit.framework.Assert;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.TreeSet;
//
//public class MmsObserverTest extends AndroidTestCase {
//
//    private Context mContext;
//    private CmsSettings mSettings;
//
//    private TreeSet<String> contact1 = new TreeSet<>();
//    private TreeSet<String> contact3 = new TreeSet<>();
//
//    private MmsDataObject incomingMms = new MmsData(null,1l,"1",contact1 ,"textContent", System.currentTimeMillis(), Direction.INCOMING, ReadStatus.READ);
//    private MmsData outgoingMms = new MmsData(null,1l,"2",contact1 ,"textContent", System.currentTimeMillis(), Direction.OUTGOING, ReadStatus.READ);
//
//    private MmsData mms = new MmsData(null,1l,"3", contact1, "myContent3", System.currentTimeMillis(), Direction.INCOMING, ReadStatus.READ);
//    private MmsData mms2 = new MmsData(null,1l,"4", contact1, "myContent3", System.currentTimeMillis(), Direction.OUTGOING, ReadStatus.READ);
//
//    protected void setUp() throws Exception {
//        super.setUp();
//        mContext = getContext();
//        mSettings = CmsSettings.createInstance(mContext);
//
//        contact1 = new TreeSet<>();
//        contact1.add("myContact1");
//
//        contact3 = new TreeSet<>();
//        contact3.add("myContact3");
//
//    }
//
//    //TODO FGI uncomment test case
////    public void test1(){
////
////        XmsObserver xmsObserver = XmsObserver.createInstance(mContext, mSettings);
////        NativeMmsListenerMock nativeSmsListenerMock = new NativeMmsListenerMock();
////        xmsObserver.registerListener(nativeSmsListenerMock);
////
////        xmsObserver.onIncomingMms(incomingMms);
////        xmsObserver.onOutgoingMms(outgoingMms);
////
////        Assert.assertEquals(2, nativeSmsListenerMock.getMessage().size());
////        Assert.assertEquals(incomingMms, nativeSmsListenerMock.getMessage().get("1"));
////        Assert.assertEquals(outgoingMms, nativeSmsListenerMock.getMessage().get("2"));
////
////        xmsObserver.onReadNativeConversation(1l);
////        Assert.assertEquals(ReadStatus.READ, nativeSmsListenerMock.getMessage().get("1").getReadStatus());
////
////        xmsObserver.onDeleteNativeMms("1");
////        Assert.assertNull(nativeSmsListenerMock.getMessage().get("1"));
////
////        xmsObserver.unregisterListener(nativeSmsListenerMock);
////
////        xmsObserver.onIncomingMms(mms);
////        Assert.assertEquals(1, nativeSmsListenerMock.getMessage().size());
////        Assert.assertNull(nativeSmsListenerMock.getMessage().get(mms.getNativeProviderId()));
////    }
////
////    public void test2(){
////
////        XmsObserver xmsObserver = XmsObserver.createInstance(mContext, mSettings);
////        NativeMmsListenerMock nativeMmsListenerMock = new NativeMmsListenerMock();
////        xmsObserver.registerListener(nativeMmsListenerMock);
////
////        xmsObserver.onIncomingMms(mms);
////        xmsObserver.onOutgoingMms(mms2);
////
////        Assert.assertEquals(2, nativeMmsListenerMock.getMessages(1l).size());
////
////        xmsObserver.onDeleteNativeConversation(1l);
////        Assert.assertNull(nativeMmsListenerMock.getMessages(1l));
////        xmsObserver.unregisterListener(nativeMmsListenerMock);
////
////        xmsObserver.onIncomingMms(mms);
////        Assert.assertEquals(2, nativeMmsListenerMock.getMessage().size());
////    }
//
//    private class NativeMmsListenerMock implements INativeXmsEventListener {
//
//        private Map<String,MmsData> messagesByMmsId = new HashMap<String,MmsData>();
//        private Map<Long,List<MmsData>> messagesByThreadId = new HashMap<Long,List<MmsData>>();
//
//        public NativeMmsListenerMock(){
//
//        }
//
//        public Map<String,MmsData> getMessage(){
//            return messagesByMmsId;
//        }
//
//        public List<MmsData> getMessages(Long threadId){
//            return messagesByThreadId.get(threadId);
//        }
//
//
//        @Override
//        public void onIncomingSms(SmsData message) {}
//
//        @Override
//        public void onOutgoingSms(SmsData message) {}
//
//        @Override
//        public void onDeliverNativeSms(long nativeProviderId, long sentDate) {}
//
//        @Override
//        public void onDeleteNativeSms(long nativeProviderId) {}
//
//        @Override
//        public void onIncomingMms(MmsData mms) {
//
//            messagesByMmsId.put(mms.getMmsId(), mms);
//
//            List<MmsData> messages = messagesByThreadId.get(mms.getNativeThreadId());
//            if(messages==null){
//                messages = new ArrayList<MmsData>();
//                messagesByThreadId.put(mms.getNativeThreadId(), messages);
//            }
//            messages.add(mms);
//        }
//
//        @Override
//        public void onOutgoingMms(MmsData message) {
//
//            messagesByMmsId.put(message.getMmsId(), message);
//
//            List<MmsData> sms = messagesByThreadId.get(message.getNativeThreadId());
//            if(sms==null){
//                sms = new ArrayList<MmsData>();
//                messagesByThreadId.put(message.getNativeThreadId(), sms);
//            }
//            sms.add(message);
//
//        }
//
//        @Override
//        public void onDeleteNativeMms(String mmsId) {
//            messagesByMmsId.remove(mmsId);
//        }
//
//        @Override
//        public void onReadNativeConversation(long nativeThreadId) {
//            for(MmsData mmsData : messagesByThreadId.get(nativeThreadId)){
//                mmsData.setReadStatus(ReadStatus.READ);
//            }
//        }
//
//        @Override
//        public void onDeleteNativeConversation(long nativeThreadId) {
//            messagesByThreadId.remove(nativeThreadId);
//        }
//
//    }
//
//}
