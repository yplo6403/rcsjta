//package com.gsma.rcs.cms.provider.xms;
//
//import android.content.Context;
//import android.test.AndroidTestCase;
//
//import com.gsma.rcs.provider.xms.model.SmsDataObject;
//import com.gsma.rcs.utils.ContactUtil;
//import com.gsma.rcs.utils.IdGenerator;
//import com.gsma.services.rcs.RcsService.Direction;
//import com.gsma.services.rcs.RcsService.ReadStatus;
//
//import java.util.Calendar;
//import java.util.List;
//
//public class XmsLogTest extends AndroidTestCase {
//
//    private XmsLog mXmsLog;
//    private XmsLogEnvIntegration mXmsLogEnvIntegration;
//
//    private SmsDataObject[] sms;
//    private long now;
//
//    protected void setUp() throws Exception {
//        super.setUp();
//        Context context = getContext();
//        mXmsLog = XmsLog.getInstance(context);
//        mXmsLogEnvIntegration = XmsLogEnvIntegration.getInstance(context);
//        now = Calendar.getInstance().getTimeInMillis();
//
//        sms = new SmsDataObject[] {
//        		new SmsDataObject(IdGenerator.generateMessageID(), ContactUtil.createContactIdFromTrustedData("+33600000001"), "content1",Direction.INCOMING,now-200000,1l,1l, ReadStatus.UNREAD)
//        };
//
//        mXmsLog.deleteMessages();
//    }
//
//    protected void tearDown() throws Exception {
//        super.tearDown();
//    }
//
//    public void testAddMessage() {
//        assertTrue(mXmsLogEnvIntegration.isEmpty());
//        assertNotNull(mXmsLog.addSms(sms[0]));
//        assertFalse(mXmsLogEnvIntegration.isEmpty());
//    }
//
//    public void testSetMessageAsDeliveredWithNativeProviderId() {
//        mXmsLog.addSms(sms[0]);
//        mXmsLog.setMessageAsDeliveredWithNativeProviderId(XmsData.MimeType.SMS, String.valueOf(sms[0].getNativeProviderId()), now);
//
//        assertTrue(now == mXmsLogEnvIntegration.getMessages(sms[0].getContact()).get(0).getDeliveryDate());
//    }
//
//    public void testUpdateReadStatusWithNativeProviderId(){
//        String baseId = mXmsLog.addSms(sms[0]);
//        mXmsLog.updateReadStatusWithNativeThreadId(sms[0].getNativeThreadId(), ReadStatus.READ);
//        assertEquals(baseId, mXmsLog.getMessages(sms[0].getContact(), ReadStatus.READ).get(0).getBaseId());
//    }
//
//    public void testUpdateReadStatusdWithBaseId(){
//        String baseId =  mXmsLog.addSms(sms[0]);
//        mXmsLog.updateReadStatusdWithBaseId(baseId, ReadStatus.READ);
//        assertEquals(baseId, mXmsLog.getMessages(sms[0].getContact(), ReadStatus.READ).get(0).getBaseId());
//    }
//
//    public void testUpdateReadStatus(){
//        String baseId = mXmsLog.addSms(sms[0]);
//        mXmsLog.updateReadStatus(sms[0].getContact(), ReadStatus.READ);
//        assertEquals(baseId, mXmsLog.getMessages(sms[0].getContact(), ReadStatus.READ).get(0).getBaseId());
//    }
//
//    public void testGetMessages() {
//        mXmsLog.addSms(sms[0]);
//        XmsData xmsData = mXmsLog.getMessages(sms[0].getContact(), sms[0].getReadStatus()).get(0);
//        assertEquals(sms[0].getNativeProviderId(), xmsData.getNativeProviderId());
//    }
//
//    public void testGetMessages(String contact) {
//        mXmsLog.addSms(sms[0]);
//        List<SmsData> messages = mXmsLogEnvIntegration.getMessages(sms[0].getContact());
//        assertEquals(1, messages.size());
//    }
//
//    public void testGetBaseIds() {
//        String expectedBaseId =  mXmsLog.addSms(sms[0]);
//        String baseId = mXmsLog.getBaseIds(sms[0].getContact(), sms[0].getDirection(), sms[0].getMessageCorrelator()).get(0);
//        assertEquals(expectedBaseId, baseId);
//    }
//
//    public void testGetNativeProviderId() {
//        String baseId =  mXmsLog.addSms(sms[0]);
//        long nativeId = mXmsLog.getNativeProviderId(baseId);
//        assertTrue(sms[0].getNativeProviderId() == nativeId);
//    }
//
//
//    public void testDeleteConversationForContact() {
//        String baseId =  mXmsLog.addSms(sms[0]);
//        mXmsLogEnvIntegration.deleteConversationForContact(sms[0].getContact());
//        assertNull(mXmsLog.getNativeProviderId(baseId));
//    }
//
//    public void testDeleteConversation() {
//        String baseId =  mXmsLog.addSms(sms[0]);
//        mXmsLog.deleteConversation(sms[0].getNativeThreadId());
//        assertNull(mXmsLog.getNativeProviderId(baseId));
//    }
//
//    public void testDeleteMessages() {
//        String baseId =  mXmsLog.addSms(sms[0]);
//        mXmsLog.deleteMessages();
//        assertNull(mXmsLog.getNativeProviderId(baseId));
//    }
//
//    public void testDeleteMessage() {
//        String baseId =  mXmsLog.addSms(sms[0]);
//        mXmsLog.deleteMessage(baseId);
//        assertNull(mXmsLog.getNativeProviderId(baseId));
//    }
//}
