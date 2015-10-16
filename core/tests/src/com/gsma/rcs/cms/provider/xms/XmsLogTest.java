package com.gsma.rcs.cms.provider.xms;

import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.PushStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.services.rcs.RcsService.Direction;

import android.content.Context;
import android.test.AndroidTestCase;

import java.util.Calendar;
import java.util.List;

public class XmsLogTest extends AndroidTestCase {

    private XmsLog mXmsLog;
    
    private SmsData[] sms;
    private long now;

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        mXmsLog = XmsLog.getInstance(context);        
        now = Calendar.getInstance().getTimeInMillis();        
        
        sms = new SmsData[] {
        		new SmsData(1l,"+33600000001", "content1",now-200000,Direction.INCOMING, ReadStatus.UNREAD, "content1")
        };
                
        mXmsLog.deleteMessages();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAddMessage() {
        assertTrue(mXmsLog.isEmpty());
        assertNotNull(mXmsLog.addMessage(sms[0]));
        assertFalse(mXmsLog.isEmpty());
    }

    public void testSetMessageAsDeliveredWithNativeProviderId(String nativeProviderId, long date) {        
        mXmsLog.addMessage(sms[0]);
        mXmsLog.setMessageAsDeliveredWithNativeProviderId(String.valueOf(sms[0].getNativeProviderId()), now);        
        assertTrue(now == mXmsLog.getMessages(sms[0].getContact(), sms[0].getReadStatus()).get(0).getDeliveryDate());
    }
    
    public void testUpdateReadStatusWithNativeProviderId(){
        String baseId = mXmsLog.addMessage(sms[0]);
        mXmsLog.updateReadStatusWithNativeProviderId(sms[0].getNativeProviderId(), ReadStatus.READ_REQUESTED);
        assertEquals(baseId, mXmsLog.getMessages(sms[0].getContact(), ReadStatus.READ_REQUESTED).get(0).getBaseId());
    } 
        
    public void testUpdateReadStatusdWithBaseId(){
        String baseId =  mXmsLog.addMessage(sms[0]);
        mXmsLog.updateReadStatusdWithBaseId(baseId, ReadStatus.READ);
        assertEquals(baseId, mXmsLog.getMessages(sms[0].getContact(), ReadStatus.READ).get(0).getBaseId());
    }
    
    public void testUpdateReadStatus(){
        String baseId = mXmsLog.addMessage(sms[0]);
        mXmsLog.updateReadStatus(sms[0].getContact(), ReadStatus.READ);
        assertEquals(baseId, mXmsLog.getMessages(sms[0].getContact(), ReadStatus.READ).get(0).getBaseId());
    } 
    
    public void testUpdatePushStatus(){
        String baseId = mXmsLog.addMessage(sms[0]);
        mXmsLog.updatePushStatus(baseId, PushStatus.PUSH_REQUESTED);
        assertEquals(baseId, mXmsLog.getMessages(PushStatus.PUSH_REQUESTED).get(0).getBaseId());
    }
    
    public void testUpdateDeleteStatusWithNativeProviderId(Long nativeProviderId, DeleteStatus deleteStatus){
        String baseId = mXmsLog.addMessage(sms[0]);
        mXmsLog.updateDeleteStatusWithNativeProviderId(sms[0].getNativeProviderId(), DeleteStatus.DELETED_REQUESTED);
        assertEquals(baseId, mXmsLog.getMessages(sms[0].getContact(), DeleteStatus.DELETED_REQUESTED).get(0).getBaseId());        
    } 
        
    public void testUpdateDeleteStatusdWithBaseId(){
        String baseId =  mXmsLog.addMessage(sms[0]);
        mXmsLog.updateDeleteStatusdWithBaseId(baseId, DeleteStatus.DELETED);
        assertEquals(baseId, mXmsLog.getMessages(sms[0].getContact(), DeleteStatus.DELETED).get(0).getBaseId());
    }
    
    public void testUpdateDeleteStatus(){
        String baseId =  mXmsLog.addMessage(sms[0]);
        mXmsLog.updateDeleteStatus(sms[0].getContact(), DeleteStatus.DELETED);
        assertEquals(baseId, mXmsLog.getMessages(sms[0].getContact(), DeleteStatus.DELETED).get(0).getBaseId());    
    } 
    
    public void testGetMessages() {
        mXmsLog.addMessage(sms[0]);
        SmsData smsData = mXmsLog.getMessages(sms[0].getContact(), sms[0].getReadStatus()).get(0);
        assertEquals(sms[0].getNativeProviderId(), smsData.getNativeProviderId());
    }
    
    public void testGetMessages(String contact) {
        mXmsLog.addMessage(sms[0]);
        List<SmsData> messages = mXmsLog.getMessages(sms[0].getContact());        
        assertEquals(1, messages.size());
    }
    
    public void testGetBaseIds() {
        String expectedBaseId =  mXmsLog.addMessage(sms[0]);        
        String baseId = mXmsLog.getBaseIds(sms[0].getContact(), sms[0].getDirection(), sms[0].getMessageCorrelator()).get(0);
        assertEquals(expectedBaseId, baseId);        
    }
    
    public void testGetMessages2() {
        String expectedBaseId =  mXmsLog.addMessage(sms[0]);        
        String baseId = mXmsLog.getMessages(sms[0].getContact(), sms[0].getReadStatus(), sms[0].getDeleteStatus()).get(0).getBaseId();
        assertEquals(expectedBaseId, baseId);        
    }
    
    public void testGetMessages3() {
        String expectedBaseId =  mXmsLog.addMessage(sms[0]);        
        String baseId = mXmsLog.getMessages(PushStatus.PUSHED).get(0).getBaseId();
        assertEquals(expectedBaseId, baseId);        
    }
    
    public void getMessages3() {
        String expectedBaseId =  mXmsLog.addMessage(sms[0]);        
        String baseId = mXmsLog.getMessages(sms[0].getReadStatus(), sms[0].getDeleteStatus()).get(0).getBaseId();
        assertEquals(expectedBaseId, baseId);        
    }
    
    public void getMessages4(String contact, DeleteStatus deleteStatus) {
        String expectedBaseId =  mXmsLog.addMessage(sms[0]);        
        String baseId = mXmsLog.getMessages(sms[0].getContact(), sms[0].getDeleteStatus()).get(0).getBaseId();
        assertEquals(expectedBaseId, baseId);        
    }
    
    public void testGetNativeProviderId() {
        String baseId =  mXmsLog.addMessage(sms[0]);        
        long nativeId = mXmsLog.getNativeProviderId(baseId);
        assertTrue(sms[0].getNativeProviderId()==nativeId);        
    }
    

    public void testDeleteConversationForContact(String contact) {
        String baseId =  mXmsLog.addMessage(sms[0]);        
        mXmsLog.deleteConversationForContact(sms[0].getContact());
        assertNull(mXmsLog.getNativeProviderId(baseId));                
    }

   
    public void testDeleteMessages() {
        String baseId =  mXmsLog.addMessage(sms[0]);        
        mXmsLog.deleteMessages();
        assertNull(mXmsLog.getNativeProviderId(baseId));                
    }

    public void testDeleteMessage() {
        String baseId =  mXmsLog.addMessage(sms[0]);        
        mXmsLog.deleteMessage(baseId);
        assertNull(mXmsLog.getNativeProviderId(baseId));                
    }
}
