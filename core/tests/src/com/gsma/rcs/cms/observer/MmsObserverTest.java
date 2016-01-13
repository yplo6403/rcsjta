
package com.gsma.rcs.cms.observer;

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

    private XmsObserver mXmsObserver;
    private NativeMmsListenerMock mNativeMmsListenerMock;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        contact1 = ContactUtil.getInstance(mContext).formatContact("+33600000001");

        incomingMms1 = new MmsDataObject("mmsId1", "messageId1", contact1, "subject",
                Direction.INCOMING, ReadStatus.UNREAD, System.currentTimeMillis(), null, 1l,
                new ArrayList<MmsPart>());
        outgoingMms2 = new MmsDataObject("mmsId2", "messageId2", contact1, "subject",
                Direction.OUTGOING, ReadStatus.UNREAD, System.currentTimeMillis(), null, 1l,
                new ArrayList<MmsPart>());
        incomingMms3 = new MmsDataObject("mmsId3", "messageId3", contact1, "subject",
                Direction.INCOMING, ReadStatus.UNREAD, System.currentTimeMillis(), null, 1l,
                new ArrayList<MmsPart>());
        outgoingMms4 = new MmsDataObject("mmsId4", "messageId4", contact1, "subject",
                Direction.OUTGOING, ReadStatus.UNREAD, System.currentTimeMillis(), null, 1l,
                new ArrayList<MmsPart>());

        mXmsObserver = new XmsObserver(mContext);
        mNativeMmsListenerMock = new NativeMmsListenerMock();
        mXmsObserver.registerListener(mNativeMmsListenerMock);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mXmsObserver.unregisterListener(mNativeMmsListenerMock);
    }

    public void testIncoming() {
        mXmsObserver.onIncomingMms(incomingMms1);
        Assert.assertEquals(1, mNativeMmsListenerMock.getMessage().size());
        Assert.assertEquals(incomingMms1, mNativeMmsListenerMock.getMessage().get("mmsId1"));
    }

    public void testOutgoing() {
        mXmsObserver.onOutgoingMms(outgoingMms2);
        Assert.assertEquals(1, mNativeMmsListenerMock.getMessage().size());
        Assert.assertEquals(outgoingMms2, mNativeMmsListenerMock.getMessage().get("mmsId2"));
    }

    public void testReadNativeConversation() {
        mXmsObserver.onIncomingMms(incomingMms1);
        mXmsObserver.onReadXmsConversationFromNativeApp(1l);
        Assert.assertEquals(ReadStatus.READ, mNativeMmsListenerMock.getMessage().get("mmsId1")
                .getReadStatus());
    }

    public void testDeleteMms() {
        mXmsObserver.onIncomingMms(incomingMms1);
        mXmsObserver.onDeleteMmsFromNativeApp("mmsId1");
        Assert.assertNull(mNativeMmsListenerMock.getMessage().get("mmsId1"));
    }

    public void testDeleteConversation() {
        mXmsObserver.onIncomingMms(incomingMms3);
        mXmsObserver.onOutgoingMms(outgoingMms4);
        mXmsObserver.onDeleteXmsConversationFromNativeApp(1l);
        Assert.assertNull(mNativeMmsListenerMock.getMessages(1l));
        mXmsObserver.unregisterListener(mNativeMmsListenerMock);
    }

    public void testUnregister() {
        mXmsObserver.unregisterListener(mNativeMmsListenerMock);
        mXmsObserver.onIncomingMms(incomingMms1);
        mXmsObserver.onOutgoingMms(outgoingMms2);
        Assert.assertEquals(0, mNativeMmsListenerMock.getMessage().size());

        mXmsObserver.registerListener(mNativeMmsListenerMock);
        mXmsObserver.onIncomingMms(incomingMms1);
        mXmsObserver.onOutgoingMms(outgoingMms2);
        Assert.assertEquals(2, mNativeMmsListenerMock.getMessage().size());
    }

    private class NativeMmsListenerMock implements XmsObserverListener {

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
        public void onDeleteSmsFromNativeApp(long nativeProviderId) {

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
        public void onDeleteMmsFromNativeApp(String mmsId) {
            messagesByMmsId.remove(mmsId);
        }

        @Override
        public void onXmsMessageStateChanged(Long nativeProviderId, String mimeType, State state) {

        }

        @Override
        public void onReadXmsConversationFromNativeApp(long nativeThreadId) {
            for (MmsDataObject mmsData : messagesByThreadId.get(nativeThreadId)) {
                mmsData.setReadStatus(ReadStatus.READ);
            }
        }

        @Override
        public void onDeleteXmsConversationFromNativeApp(long nativeThreadId) {
            messagesByThreadId.remove(nativeThreadId);
        }

    }

}
