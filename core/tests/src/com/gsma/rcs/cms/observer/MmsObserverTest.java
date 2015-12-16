
package com.gsma.rcs.cms.observer;

import com.gsma.rcs.cms.event.INativeXmsEventListener;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MmsObserverTest extends AndroidTestCase {

    private Context mContext;
    private ContactId contact1;
    private MmsDataObject incomingMms1;
    private MmsDataObject outgoingMms2;
    private MmsDataObject incomingMms3;
    private MmsDataObject outgoingMms4;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        contact1 = ContactUtil.getInstance(mContext).formatContact("+33600000001");

        incomingMms1 = new MmsDataObject("mmsId1", "messageId1", contact1, "subject",
                Direction.INCOMING, ReadStatus.READ, System.currentTimeMillis(), null, 1l,
                new ArrayList<MmsPart>());
        outgoingMms2 = new MmsDataObject("mmsId2", "messageId2", contact1, "subject",
                Direction.OUTGOING, ReadStatus.READ, System.currentTimeMillis(), null, 1l,
                new ArrayList<MmsPart>());
        incomingMms3 = new MmsDataObject("mmsId3", "messageId3", contact1, "subject",
                Direction.INCOMING, ReadStatus.READ, System.currentTimeMillis(), null, 1l,
                new ArrayList<MmsPart>());
        outgoingMms4 = new MmsDataObject("mmsId4", "messageId4", contact1, "subject",
                Direction.OUTGOING, ReadStatus.READ, System.currentTimeMillis(), null, 1l,
                new ArrayList<MmsPart>());

    }

    // TODO FG give a comprehensible name to test
    public void test1() {
        XmsObserver xmsObserver = new XmsObserver(mContext);
        NativeMmsListenerMock nativeSmsListenerMock = new NativeMmsListenerMock();
        xmsObserver.registerListener(nativeSmsListenerMock);

        xmsObserver.onIncomingMms(incomingMms1);
        xmsObserver.onOutgoingMms(outgoingMms2);

        Assert.assertEquals(2, nativeSmsListenerMock.getMessage().size());
        Assert.assertEquals(incomingMms1, nativeSmsListenerMock.getMessage().get("mmsId1"));
        Assert.assertEquals(outgoingMms2, nativeSmsListenerMock.getMessage().get("mmsId2"));

        xmsObserver.onReadNativeConversation(1l);
        Assert.assertEquals(ReadStatus.READ, nativeSmsListenerMock.getMessage().get("mmsId1")
                .getReadStatus());

        xmsObserver.onDeleteNativeMms("mmsId1");
        Assert.assertNull(nativeSmsListenerMock.getMessage().get("mmsId1"));

        xmsObserver.unregisterListener(nativeSmsListenerMock);

        xmsObserver.onIncomingMms(incomingMms3);
        Assert.assertEquals(1, nativeSmsListenerMock.getMessage().size());
        Assert.assertNull(nativeSmsListenerMock.getMessage().get(incomingMms3.getMmsId()));
    }

    public void test2() {

        XmsObserver xmsObserver = new XmsObserver(mContext);
        NativeMmsListenerMock nativeMmsListenerMock = new NativeMmsListenerMock();
        xmsObserver.registerListener(nativeMmsListenerMock);

        xmsObserver.onIncomingMms(incomingMms3);
        xmsObserver.onOutgoingMms(outgoingMms4);

        Assert.assertEquals(2, nativeMmsListenerMock.getMessages(1l).size());

        xmsObserver.onDeleteNativeConversation(1l);
        Assert.assertNull(nativeMmsListenerMock.getMessages(1l));
        xmsObserver.unregisterListener(nativeMmsListenerMock);

        xmsObserver.onIncomingMms(incomingMms3);
        Assert.assertEquals(2, nativeMmsListenerMock.getMessage().size());
    }

    private class NativeMmsListenerMock implements INativeXmsEventListener {

        private Map<String, MmsDataObject> messagesByMmsId = new HashMap<>();
        private Map<Long, List<MmsDataObject>> messagesByThreadId = new HashMap<>();

        public NativeMmsListenerMock() {

        }

        public Map<String, MmsDataObject> getMessage() {
            return messagesByMmsId;
        }

        public List<MmsDataObject> getMessages(Long threadId) {
            return messagesByThreadId.get(threadId);
        }

        @Override
        public void onIncomingSms(SmsDataObject message) {

        }

        @Override
        public void onOutgoingSms(SmsDataObject message) {

        }

        @Override
        public void onDeleteNativeSms(long nativeProviderId) {

        }

        @Override
        public void onIncomingMms(MmsDataObject mms) {

            messagesByMmsId.put(mms.getMmsId(), mms);

            List<MmsDataObject> messages = messagesByThreadId.get(mms.getNativeThreadId());
            if (messages == null) {
                messages = new ArrayList<>();
                messagesByThreadId.put(mms.getNativeThreadId(), messages);
            }
            messages.add(mms);
        }

        @Override
        public void onOutgoingMms(MmsDataObject message) {

            messagesByMmsId.put(message.getMmsId(), message);

            List<MmsDataObject> sms = messagesByThreadId.get(message.getNativeThreadId());
            if (sms == null) {
                sms = new ArrayList<>();
                messagesByThreadId.put(message.getNativeThreadId(), sms);
            }
            sms.add(message);

        }

        @Override
        public void onDeleteNativeMms(String mmsId) {
            messagesByMmsId.remove(mmsId);
        }

        @Override
        public void onMessageStateChanged(Long nativeProviderId, String mimeType, State state) {

        }

        @Override
        public void onReadNativeConversation(long nativeThreadId) {
            for (MmsDataObject mmsData : messagesByThreadId.get(nativeThreadId)) {
                mmsData.setReadStatus(ReadStatus.READ);
            }
        }

        @Override
        public void onDeleteNativeConversation(long nativeThreadId) {
            messagesByThreadId.remove(nativeThreadId);
        }

    }

}
