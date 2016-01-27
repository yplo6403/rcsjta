/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.cms.observer;

import com.gsma.rcs.provider.xms.model.MmsDataObject;
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

public class SmsObserverTest extends AndroidTestCase {

    private Context mContext;
    private ContactId contact1;
    private ContactId contact2;
    private ContactId contact3;
    private SmsDataObject incomingSms;
    private SmsDataObject outgoingSms;
    private SmsDataObject sms;
    private SmsDataObject sms2;

    private XmsObserver mXmsObserver;
    private NativeSmsListenerMock mNativeSmsListenerMock;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();

        contact1 = ContactUtil.getInstance(mContext).formatContact("+33600000001");
        contact2 = ContactUtil.getInstance(mContext).formatContact("+33600000002");
        contact3 = ContactUtil.getInstance(mContext).formatContact("+33600000003");

        incomingSms = new SmsDataObject("messageId1", contact1, "myContent1", Direction.INCOMING,
                ReadStatus.UNREAD, System.currentTimeMillis(), 1l, 1l);
        outgoingSms = new SmsDataObject("messageId2", contact2, "myContent2", Direction.OUTGOING,
                ReadStatus.UNREAD, System.currentTimeMillis(), 2l, 1l);
        sms = new SmsDataObject("messageId3", contact3, "myContent3", Direction.INCOMING,
                ReadStatus.UNREAD, System.currentTimeMillis(), 3l, 1l);
        sms2 = new SmsDataObject("messageId4", contact3, "myContent3", Direction.OUTGOING,
                ReadStatus.UNREAD, System.currentTimeMillis(), 4l, 1l);

        mXmsObserver = new XmsObserver(mContext);
        mNativeSmsListenerMock = new NativeSmsListenerMock();
        mXmsObserver.registerListener(mNativeSmsListenerMock);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mXmsObserver.unregisterListener(mNativeSmsListenerMock);
    }

    public void testIncoming() {
        mXmsObserver.onIncomingSms(incomingSms);
        Assert.assertEquals(1, mNativeSmsListenerMock.getMessage().size());
        Assert.assertEquals(incomingSms, mNativeSmsListenerMock.getMessage().get(1l));
    }

    public void testOutgoing() {
        mXmsObserver.onOutgoingSms(outgoingSms);
        Assert.assertEquals(1, mNativeSmsListenerMock.getMessage().size());
        Assert.assertEquals(outgoingSms, mNativeSmsListenerMock.getMessage().get(2l));
    }

    public void testReadNativeConversation() {
        mXmsObserver.onIncomingSms(incomingSms);
        mXmsObserver.onReadXmsConversationFromNativeApp(1l);
        Assert.assertEquals(ReadStatus.READ, mNativeSmsListenerMock.getMessage().get(1l)
                .getReadStatus());
    }

    public void testDeleteSms() {
        mXmsObserver.onIncomingSms(outgoingSms);
        mXmsObserver.onDeleteSmsFromNativeApp(2l);
        Assert.assertNull(mNativeSmsListenerMock.getMessage().get(2l));
    }

    public void testDeleteConversation() {
        mXmsObserver.onIncomingSms(incomingSms);
        mXmsObserver.onOutgoingSms(outgoingSms);
        mXmsObserver.onDeleteXmsConversationFromNativeApp(1l);
        Assert.assertNull(mNativeSmsListenerMock.getMessages(1l));
    }

    public void testUnregister() {
        mXmsObserver.unregisterListener(mNativeSmsListenerMock);
        mXmsObserver.onIncomingSms(incomingSms);
        mXmsObserver.onOutgoingSms(outgoingSms);
        Assert.assertEquals(0, mNativeSmsListenerMock.getMessage().size());

        mXmsObserver.registerListener(mNativeSmsListenerMock);
        mXmsObserver.onIncomingSms(incomingSms);
        mXmsObserver.onOutgoingSms(outgoingSms);
        Assert.assertEquals(2, mNativeSmsListenerMock.getMessage().size());
    }

    private class NativeSmsListenerMock implements XmsObserverListener {

        private Map<Long, SmsDataObject> smsById = new HashMap<>();
        private Map<Long, List<SmsDataObject>> smsByThreadId = new HashMap<>();

        public NativeSmsListenerMock() {
        }

        public Map<Long, SmsDataObject> getMessage() {
            return smsById;
        }

        public List<SmsDataObject> getMessages(Long threadId) {
            return smsByThreadId.get(threadId);
        }

        @Override
        public void onIncomingSms(SmsDataObject message) {
            smsById.put(message.getNativeProviderId(), message);
            List<SmsDataObject> sms = smsByThreadId.get(message.getContact());
            if (sms == null) {
                sms = new ArrayList<>();
                smsByThreadId.put(message.getNativeThreadId(), sms);
            }
            sms.add(message);
        }

        @Override
        public void onOutgoingSms(SmsDataObject message) {
            smsById.put(message.getNativeProviderId(), message);
            List<SmsDataObject> sms = smsByThreadId.get(message.getNativeThreadId());
            if (sms == null) {
                sms = new ArrayList<>();
                smsByThreadId.put(message.getNativeThreadId(), sms);
            }
            sms.add(message);

        }

        @Override
        public void onDeleteSmsFromNativeApp(long nativeProviderId) {
            smsById.remove(nativeProviderId);
        }

        @Override
        public void onIncomingMms(MmsDataObject message) {
        }

        @Override
        public void onOutgoingMms(MmsDataObject message) {
        }

        @Override
        public void onDeleteMmsFromNativeApp(String mmsId) {
        }

        @Override
        public void onXmsMessageStateChanged(Long nativeProviderId, String mimeType, State state) {
            smsById.get(nativeProviderId).setState(state);
        }

        @Override
        public void onReadXmsConversationFromNativeApp(long nativeThreadId) {
            for (SmsDataObject smsData : smsByThreadId.get(nativeThreadId)) {
                smsData.setReadStatus(ReadStatus.READ);
            }
        }

        @Override
        public void onDeleteXmsConversationFromNativeApp(long nativeThreadId) {
            for (SmsDataObject smsData : smsByThreadId.get(nativeThreadId)) {
                smsById.remove(smsData.getNativeProviderId());
            }
            smsByThreadId.remove(nativeThreadId);
        }

    }

}
