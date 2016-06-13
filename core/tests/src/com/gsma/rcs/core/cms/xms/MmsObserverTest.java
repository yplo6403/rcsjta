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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.integration.RcsSettingsMock;
import com.gsma.rcs.core.cms.xms.observer.XmsObserver;
import com.gsma.rcs.core.cms.xms.observer.XmsObserverListener;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
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

public class MmsObserverTest extends AndroidTestCase {

    private MmsDataObject mIncomingMms1;
    private MmsDataObject mOutgoingMms2;
    private MmsDataObject mIncomingMms3;
    private MmsDataObject mOutgoingMms4;

    private XmsObserver mXmsObserver;
    private NativeMmsListenerMock mNativeMmsListenerMock;

    protected void setUp() throws Exception {
        super.setUp();
        ContactId contact1 = ContactUtil.getInstance(new ContactUtilMockContext(mContext))
                .formatContact("+33600000001");
        mIncomingMms1 = new MmsDataObject("mmsId1", "messageId1", contact1, "subject",
                Direction.INCOMING, ReadStatus.UNREAD, NtpTrustedTime.currentTimeMillis(), null,
                1L, new ArrayList<MmsPart>());
        mOutgoingMms2 = new MmsDataObject("mmsId2", "messageId2", contact1, "subject",
                Direction.OUTGOING, ReadStatus.UNREAD, NtpTrustedTime.currentTimeMillis(), null,
                1L, new ArrayList<MmsPart>());
        mIncomingMms3 = new MmsDataObject("mmsId3", "messageId3", contact1, "subject",
                Direction.INCOMING, ReadStatus.UNREAD, NtpTrustedTime.currentTimeMillis(), null,
                1L, new ArrayList<MmsPart>());
        mOutgoingMms4 = new MmsDataObject("mmsId4", "messageId4", contact1, "subject",
                Direction.OUTGOING, ReadStatus.UNREAD, NtpTrustedTime.currentTimeMillis(), null,
                1L, new ArrayList<MmsPart>());

        RcsSettings settings = RcsSettingsMock.getMockSettings(mContext);
        mXmsObserver = new XmsObserver(mContext.getContentResolver(), settings);
        mNativeMmsListenerMock = new NativeMmsListenerMock();
        mXmsObserver.registerListener(mNativeMmsListenerMock);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mXmsObserver.unregisterListener(mNativeMmsListenerMock);
    }

    public void testIncoming() {
        mXmsObserver.onIncomingMms(mIncomingMms1);
        Assert.assertEquals(1, mNativeMmsListenerMock.getMessage().size());
        Assert.assertEquals(mIncomingMms1, mNativeMmsListenerMock.getMessage().get("messageId1"));
    }

    public void testOutgoing() throws FileAccessException {
        mXmsObserver.onOutgoingMms(mOutgoingMms2);
        Assert.assertEquals(1, mNativeMmsListenerMock.getMessage().size());
        Assert.assertEquals(mOutgoingMms2, mNativeMmsListenerMock.getMessage().get("messageId2"));
    }

    public void testReadNativeConversation() {
        mXmsObserver.onIncomingMms(mIncomingMms1);
        mXmsObserver.onReadMmsConversationFromNativeApp(1L);
        Assert.assertEquals(ReadStatus.READ, mNativeMmsListenerMock.getMessage().get("messageId1")
                .getReadStatus());
    }

    public void testDeleteMms() {
        mXmsObserver.onIncomingMms(mIncomingMms1);
        mXmsObserver.onDeleteMmsFromNativeApp("mmsId1");
        Assert.assertNull(mNativeMmsListenerMock.getMessage().get("mmsId1"));
    }

    public void testDeleteConversation() throws FileAccessException {
        mXmsObserver.onIncomingMms(mIncomingMms3);
        mXmsObserver.onOutgoingMms(mOutgoingMms4);
        mXmsObserver.onDeleteMmsConversationFromNativeApp(1L);
        Assert.assertNull(mNativeMmsListenerMock.getMessages(1L));
        mXmsObserver.unregisterListener(mNativeMmsListenerMock);
    }

    public void testUnregister() throws FileAccessException {
        mXmsObserver.unregisterListener(mNativeMmsListenerMock);
        mXmsObserver.onIncomingMms(mIncomingMms1);
        mXmsObserver.onOutgoingMms(mOutgoingMms2);
        Assert.assertEquals(0, mNativeMmsListenerMock.getMessage().size());

        mXmsObserver.registerListener(mNativeMmsListenerMock);
        mXmsObserver.onIncomingMms(mIncomingMms1);
        mXmsObserver.onOutgoingMms(mOutgoingMms2);
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
            messagesByMmsId.put(mms.getMessageId(), mms);
            List<MmsDataObject> messages = messagesByThreadId.get(mms.getNativeThreadId());
            if (messages == null) {
                messages = new ArrayList<>();
                messagesByThreadId.put(mms.getNativeThreadId(), messages);
            }
            messages.add(mms);
        }

        @Override
        public void onOutgoingMms(MmsDataObject message) {
            messagesByMmsId.put(message.getMessageId(), message);
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
        public void onSmsMessageStateChanged(Long nativeProviderId, State state) {

        }

        @Override
        public void onReadMmsConversationFromNativeApp(long nativeThreadId) {
            for (MmsDataObject mmsData : messagesByThreadId.get(nativeThreadId)) {
                mmsData.setReadStatus(ReadStatus.READ);
            }
        }

        @Override
        public void onDeleteMmsConversationFromNativeApp(long nativeThreadId) {
            messagesByThreadId.remove(nativeThreadId);
        }

    }

}
