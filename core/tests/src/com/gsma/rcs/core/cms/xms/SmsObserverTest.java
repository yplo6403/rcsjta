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

package com.gsma.rcs.core.cms.xms;

import com.gsma.rcs.core.cms.integration.RcsSettingsMock;
import com.gsma.rcs.core.cms.xms.observer.XmsObserver;
import com.gsma.rcs.core.cms.xms.observer.XmsObserverListener;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsObserverTest extends AndroidTestCase {

    private SmsDataObject incomingSms;
    private SmsDataObject outgoingSms;
    private XmsObserver mXmsObserver;
    private NativeSmsListenerMock mNativeSmsListenerMock;

    protected void setUp() throws Exception {
        super.setUp();
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        ContactId contact1 = contactUtils.formatContact("+33600000001");
        ContactId contact2 = contactUtils.formatContact("+33600000002");
        incomingSms = new SmsDataObject("messageId1", contact1, "myContent1", Direction.INCOMING,
                ReadStatus.UNREAD, NtpTrustedTime.currentTimeMillis(), 1L, 1L);
        outgoingSms = new SmsDataObject("messageId2", contact2, "myContent2", Direction.OUTGOING,
                ReadStatus.UNREAD, NtpTrustedTime.currentTimeMillis(), 2L, 1L);
        RcsSettings settings = RcsSettingsMock.getMockSettings(mContext);
        mXmsObserver = new XmsObserver(mContext.getContentResolver(), settings);
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
        Assert.assertEquals(incomingSms, mNativeSmsListenerMock.getMessage().get(1L));
    }

    public void testOutgoing() {
        mXmsObserver.onOutgoingSms(outgoingSms);
        Assert.assertEquals(1, mNativeSmsListenerMock.getMessage().size());
        Assert.assertEquals(outgoingSms, mNativeSmsListenerMock.getMessage().get(2L));
    }

    public void testReadNativeConversation() {
        mXmsObserver.onIncomingSms(incomingSms);
        mXmsObserver.onReadMmsConversationFromNativeApp(1L);
        Assert.assertEquals(ReadStatus.READ, mNativeSmsListenerMock.getMessage().get(1L)
                .getReadStatus());
    }

    public void testDeleteSms() {
        mXmsObserver.onIncomingSms(outgoingSms);
        mXmsObserver.onDeleteSmsFromNativeApp(2L);
        Assert.assertNull(mNativeSmsListenerMock.getMessage().get(2L));
    }

    public void testDeleteConversation() {
        mXmsObserver.onIncomingSms(incomingSms);
        mXmsObserver.onOutgoingSms(outgoingSms);
        mXmsObserver.onDeleteMmsConversationFromNativeApp(1L);
        Assert.assertNull(mNativeSmsListenerMock.getMessages(1L));
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
        public void onSmsMessageStateChanged(Long nativeProviderId, State state) {
            smsById.get(nativeProviderId).setState(state);
        }

        @Override
        public void onReadMmsConversationFromNativeApp(long nativeThreadId) {
            for (SmsDataObject smsData : smsByThreadId.get(nativeThreadId)) {
                smsData.setReadStatus(ReadStatus.READ);
            }
        }

        @Override
        public void onDeleteMmsConversationFromNativeApp(long nativeThreadId) {
            for (SmsDataObject smsData : smsByThreadId.get(nativeThreadId)) {
                smsById.remove(smsData.getNativeProviderId());
            }
            smsByThreadId.remove(nativeThreadId);
        }

    }

}
